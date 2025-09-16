package com.chimaenono.dearmind.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.chimaenono.dearmind.userEmotionAnalysis.UserEmotionAnalysis;
import com.chimaenono.dearmind.userEmotionAnalysis.UserEmotionAnalysisRepository;

@Service
public class EmotionFlowService {

    // ----- 라벨/스칼라 맵 -----
    private static final List<String> L6 = List.of("기쁨","당황","분노","불안","상처","슬픔");
    private static final Map<String, Double> VALENCE = Map.of(
            "기쁨", +1.0, "당황", -1.0, "분노", -1.0, "불안", -1.0, "상처", -1.0, "슬픔", -1.0
    );
    private static final Map<String, Double> AROUSAL = Map.of(
            "기쁨", +0.3, "당황", +1.0, "분노", +1.0, "불안", +1.0, "상처", -1.0, "슬픔", -1.0
    );

    // ----- 파라미터(필요시 조정) -----
    private static final double BETA = 0.4;     // EMA 스무딩
    private static final int W = 3;             // 창 크기(창 다수결)
    private static final double TAU = 0.6;      // 창 다수 임계
    private static final int MIN_SEG = 2;       // 최소 세그 길이
    private static final int COOLDOWN = 2;      // 전환 쿨다운

    @Autowired private ObjectMapper om;

    @Autowired private UserEmotionAnalysisRepository userEmotionAnalysisRepository;
    @Autowired private ConversationRepository conversationRepository;

    // ====== Public API ======
    @Operation(summary = "대화 감정 흐름 계산/저장", description = "conversationId에 대해 Emotion Flow를 계산하고 DB에 저장합니다.")
    @Transactional
    public void computeAndSaveFlow(Long conversationId) {
        // 1) 해당 대화의 '사용자 턴'을 순서대로 로드
        List<UserEmotionAnalysis> rows = loadOrderedUserTurns(conversationId);
        if (rows.isEmpty()) {
            saveEmpty(conversationId);
            return;
        }

        // 2) p_fused 시퀀스 + conf 시퀀스 구성
        List<Map<String, Double>> pSeq = new ArrayList<>();
        List<Double> confSeq = new ArrayList<>();
        List<String> idSeqForHash = new ArrayList<>();

        for (UserEmotionAnalysis r : rows) {
            Map<String, Double> p = null;
            String distJson = r.getCombinedDistribution();
            if (distJson != null && !distJson.isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> raw = om.readValue(distJson, Map.class);
                    p = normalizeToL6(raw);
                } catch (Exception ignore) { /* fallback below */ }
            }
            if (p == null) {
                // 과거 데이터 호환: 라벨+신뢰도로 의사분포
                p = pseudo(r.getCombinedEmotion(), safe(r.getCombinedConfidence(), 0.5));
            }
            pSeq.add(p);
            confSeq.add(safe(r.getCombinedConfidence(), 0.5));
            // 해시 재현용 식별자: messageId 있으면 사용, 없으면 id 사용
            String mid = String.valueOf(
                    r.getConversationMessage() != null ? r.getConversationMessage().getId() :
                    r.getId() != null ? r.getId() :
                    0L
            );
            idSeqForHash.add(mid);
        }

        // 3) EMA 스무딩
        List<Map<String, Double>> pSm = ema(pSeq, BETA);

        // 4) 세그먼트 탐지
        List<Segment> segments = segmentize(pSm, confSeq);

        // 5) 메트릭/패턴
        Metrics metrics = buildMetrics(pSm);

        // 6) params + inputHash 기록
        Map<String, Object> params = Map.of(
                "W", W, "beta", BETA, "tau", TAU, "minSegLen", MIN_SEG, "cooldown", COOLDOWN,
                "labels", L6
        );
        String inputHash = buildInputHash(conversationId, idSeqForHash, pSeq);

        // 7) JSON payload
        Map<String, Object> flowJson = Map.of(
                "segments", segments.stream().map(Segment::toMap).collect(Collectors.toList()),
                "metrics", metrics.toMap(),
                "params", params,
                "inputHash", inputHash,
                "generatedAt", Instant.now().toString()
        );

        // 8) 저장
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("conversation not found: " + conversationId));
        conv.setEmotionFlow(serialize(flowJson));
        conv.setFlowPattern(metrics.pattern);
        conversationRepository.save(conv);
    }

    // ====== 내부 구현 ======

    private List<UserEmotionAnalysis> loadOrderedUserTurns(Long conversationId) {
        // 실제 Repository 메서드 사용
        List<UserEmotionAnalysis> list = userEmotionAnalysisRepository
                .findByConversationMessageConversationIdOrderByAnalysisTimestampAsc(conversationId);
        
        // null 방어
        return list.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    private static double safe(Double x, double def) {
        return (x == null || x.isNaN()) ? def : Math.max(0.0, Math.min(1.0, x));
    }

    private Map<String, Double> normalizeToL6(Map<String, ?> raw) {
        double s = 0.0;
        Map<String, Double> out = new LinkedHashMap<>();
        for (String l : L6) {
            double v = 0.0;
            Object o = raw.get(l);
            if (o instanceof Number) v = ((Number) o).doubleValue();
            out.put(l, Math.max(0.0, v));
            s += Math.max(0.0, v);
        }
        if (s <= 1e-12) {
            double u = 1.0 / L6.size();
            for (String l : L6) out.put(l, u);
        } else {
            for (String l : L6) out.put(l, out.get(l) / s);
        }
        return out;
    }

    private Map<String, Double> pseudo(String labelKr, double conf) {
        // conf 높을수록 메인 라벨에 더 몰아줌
        double eps = Math.max(0.1, 1.0 - conf);
        double main = 1.0 - eps;
        double rest = eps / (L6.size() - 1);
        Map<String, Double> m = new LinkedHashMap<>();
        for (String l : L6) m.put(l, rest);
        if (labelKr != null && L6.contains(labelKr)) m.put(labelKr, main);
        return m;
    }

    private List<Map<String, Double>> ema(List<Map<String, Double>> seq, double beta) {
        List<Map<String, Double>> out = new ArrayList<>();
        Map<String, Double> prev = seq.get(0);
        out.add(prev);
        for (int i = 1; i < seq.size(); i++) {
            Map<String, Double> cur = seq.get(i);
            Map<String, Double> m = new LinkedHashMap<>();
            double z = 0;
            for (String l : L6) {
                double v = (1 - beta) * cur.get(l) + beta * prev.get(l);
                m.put(l, v);
                z += v;
            }
            for (String l : L6) m.put(l, m.get(l) / z);
            out.add(m);
            prev = m;
        }
        return out;
    }

    private List<Segment> segmentize(List<Map<String, Double>> pSm, List<Double> confSeq) {
        List<Segment> segs = new ArrayList<>();
        if (pSm.isEmpty()) return segs;

        int segStart = 0, cool = 0;
        String cur = argmax(pSm.get(0));
        Deque<String> recent = new ArrayDeque<>();
        recent.add(cur);

        for (int t = 1; t < pSm.size(); t++) {
            String y = argmax(pSm.get(t));
            recent.addLast(y);
            if (recent.size() > W) recent.removeFirst();

            boolean changed = majority(y, recent) >= TAU && !y.equals(cur);
            boolean longEnough = (t - segStart) >= MIN_SEG;

            if (cool == 0 && changed && longEnough) {
                segs.add(buildSegment(segStart, t - 1, pSm, confSeq));
                segStart = t;
                cur = y;
                cool = COOLDOWN;
            } else {
                if (cool > 0) cool--;
            }
        }
        segs.add(buildSegment(segStart, pSm.size() - 1, pSm, confSeq));
        return segs;
    }

    private Segment buildSegment(int s, int e, List<Map<String, Double>> pSm, List<Double> confSeq) {
        Map<String, Double> acc = new LinkedHashMap<>();
        L6.forEach(l -> acc.put(l, 0.0));
        double confSum = 0;
        double vSum = 0, aSum = 0;
        for (int t = s; t <= e; t++) {
            Map<String, Double> p = pSm.get(t);
            for (String l : L6) acc.put(l, acc.get(l) + p.get(l));
            confSum += confSeq.get(Math.min(t, confSeq.size() - 1));
            String y = argmax(p);
            vSum += VALENCE.getOrDefault(y, 0.0);
            aSum += AROUSAL.getOrDefault(y, 0.0);
        }
        String dom = argmax(acc);
        int len = e - s + 1;
        return new Segment(s, e, dom, confSum / len, vSum / len, aSum / len);
    }

    private Metrics buildMetrics(List<Map<String, Double>> pSm) {
        List<String> y = new ArrayList<>();
        List<Double> v = new ArrayList<>();
        for (Map<String, Double> p : pSm) {
            String lab = argmax(p);
            y.add(lab);
            v.add(VALENCE.getOrDefault(lab, 0.0));
        }

        // flips
        int flips = 0;
        for (int i = 1; i < y.size(); i++) if (!y.get(i).equals(y.get(i - 1))) flips++;

        // positive ratio
        long pos = v.stream().filter(x -> x > 0).count();
        double positiveRatio = (double) pos / Math.max(1, v.size());

        // longest negative run
        int cur = 0, best = 0;
        for (double x : v) {
            if (x < 0) { cur++; best = Math.max(best, cur); }
            else cur = 0;
        }

        // peak arousal turn (라벨 기반 근사)
        int peak = 0; double bestA = -1e9;
        for (int i = 0; i < pSm.size(); i++) {
            double a = AROUSAL.getOrDefault(argmax(pSm.get(i)), 0.0);
            if (a > bestA) { bestA = a; peak = i; }
        }

        // 패턴
        double dv = v.get(v.size()-1) - v.get(0);
        String pattern =
                (v.get(0) < 0 && v.get(v.size()-1) > 0 && flips <= 2) ? "U-shape" :
                (dv >= +0.2) ? "상승형" :
                (dv <= -0.2) ? "하강형" :
                (flips >= 3) ? "급반전형" : "안정형";

        return new Metrics(flips, positiveRatio, best, peak, pattern);
    }

    private static String argmax(Map<String, Double> p) {
        return p.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .get().getKey();
    }

    private static double majority(String y, Deque<String> win) {
        long c = win.stream().filter(s -> s.equals(y)).count();
        return (double) c / win.size();
    }

    private void saveEmpty(Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("conversation not found: " + conversationId));
        Map<String, Object> empty = Map.of(
                "segments", List.of(),
                "metrics", Map.of("pattern", "안정형"),
                "params", Map.of("W", W, "beta", BETA, "tau", TAU, "minSegLen", MIN_SEG, "cooldown", COOLDOWN),
                "inputHash", "sha1:0",
                "generatedAt", Instant.now().toString()
        );
        conv.setEmotionFlow(serialize(empty));
        conv.setFlowPattern("안정형");
        conversationRepository.save(conv);
    }

    private String serialize(Object o) {
        try { return om.writeValueAsString(o); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private String buildInputHash(Long conversationId, List<String> ids, List<Map<String, Double>> pSeq) {
        // 토큰/소수점 절약: 확률은 소수 3자리로 반올림하여 해시에 사용
        StringBuilder sb = new StringBuilder();
        sb.append("conv=").append(conversationId).append("|turns=").append(ids.size()).append('|');
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i)).append(':');
            Map<String, Double> p = pSeq.get(i);
            for (String l : L6) {
                double v = p.getOrDefault(l, 0.0);
                sb.append(l).append('=').append(String.format(java.util.Locale.ROOT, "%.3f", v)).append(',');
            }
            sb.append('|');
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] dig = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return "sha1:" + toHex(dig);
        } catch (Exception e) {
            return "sha1:0";
        }
    }

    private static String toHex(byte[] bytes) {
        char[] HEX = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0, j = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[j++] = HEX[v >>> 4];
            out[j++] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    // ====== 내부 DTO ======
    static class Segment {
        int startTurn, endTurn;
        String dominant;
        double meanConf, valenceMean, arousalMean;

        Segment(int s, int e, String d, double mc, double vm, double am) {
            this.startTurn = s; this.endTurn = e;
            this.dominant = d; this.meanConf = mc;
            this.valenceMean = vm; this.arousalMean = am;
        }
        Map<String, Object> toMap() {
            return Map.of(
                    "start_turn", startTurn,
                    "end_turn", endTurn,
                    "dominant", dominant,
                    "mean_conf", round(meanConf),
                    "valence_mean", round(valenceMean),
                    "arousal_mean", round(arousalMean)
            );
        }
    }

    static class Metrics {
        int flips;
        double positiveRatio;
        int longestNegativeRun;
        int peakArousalTurn;
        String pattern;

        Metrics(int f, double pr, int lnr, int peak, String ptn) {
            this.flips = f; this.positiveRatio = pr;
            this.longestNegativeRun = lnr; this.peakArousalTurn = peak;
            this.pattern = ptn;
        }
        Map<String, Object> toMap() {
            return Map.of(
                    "flips", flips,
                    "positive_ratio", round(positiveRatio),
                    "longest_negative_run", longestNegativeRun,
                    "peak_arousal_turn", peakArousalTurn,
                    "pattern", pattern
            );
        }
    }

    private static double round(double x) {
        return Math.round(x * 1000.0) / 1000.0; // 소수 3자리
    }
}
