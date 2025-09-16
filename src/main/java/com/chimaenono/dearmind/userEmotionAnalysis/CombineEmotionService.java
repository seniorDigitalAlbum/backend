package com.chimaenono.dearmind.userEmotionAnalysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Comparator;

@Service
@Tag(name = "CombineEmotion", description = "통합 감정 계산 서비스")
public class CombineEmotionService {
    
    @Autowired
    private UserEmotionAnalysisRepository userEmotionAnalysisRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 감정 매핑 테이블 (영어 ↔ 한국어)
    private final Map<String, String> emotionMapping = Map.of(
        "joy", "기쁨",
        "embarrassed", "당황", 
        "anger", "분노",
        "anxious", "불안",
        "hurt", "상처",
        "sad", "슬픔",
        "neutral", "중립"
    );
    
    // --- 추천 상수(필요시 서비스 톤에 맞게 조정) ---
    private static final List<String> LABELS6_KR = List.of("기쁨","당황","분노","불안","상처","슬픔");
    private static final String NEUTRAL_KR = "중립";
    private static final double ALPHA = 0.5;       // 디리클레 스무딩
    private static final double GAMMA = 1.5;       // frame confidence 가중
    private static final double CONF_MIN = 0.50;   // 너무 낮은 프레임은 제외(선택)
    private static final double LAMBDA_TXT = 0.60; // 모달 기본 가중
    private static final double LAMBDA_FACE = 0.40;
    
    @Operation(summary = "통합 감정 계산 및 저장", description = "표정 감정과 말 감정을 통합하여 최종 감정을 계산하고 저장합니다")
    @Transactional
    public UserEmotionAnalysisResponse combineEmotions(Long conversationMessageId) {
        // 1. 기존 감정 분석 데이터 조회
        Optional<UserEmotionAnalysis> analysisOpt = userEmotionAnalysisRepository.findByConversationMessageId(conversationMessageId);
        if (!analysisOpt.isPresent()) {
            throw new RuntimeException("감정 분석 데이터를 찾을 수 없습니다: " + conversationMessageId);
        }
        
        UserEmotionAnalysis analysis = analysisOpt.get();
        
        // 2. 표정 감정과 말 감정 데이터 검증
        if (analysis.getFacialEmotion() == null || analysis.getSpeechEmotion() == null) {
            throw new RuntimeException("표정 감정 또는 말 감정 데이터가 없습니다. 먼저 각각의 감정 분석을 완료해주세요.");
        }
        
        // 3. 통합 감정 계산
        CombinedEmotionResult combinedResult = calculateCombinedEmotion(
            analysis.getFacialEmotion(), 
            analysis.getSpeechEmotion()
        );
        
        // 4. 데이터베이스 업데이트
        analysis.setCombinedEmotion(combinedResult.getEmotion());
        analysis.setCombinedConfidence(combinedResult.getConfidence());
        
        // 통합 확률 분포를 JSON으로 변환하여 저장
        try {
            String combinedDistributionJson = objectMapper.writeValueAsString(combinedResult.getPFused());
            analysis.setCombinedDistribution(combinedDistributionJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("통합 확률 분포 JSON 변환 실패: " + e.getMessage());
        }
        
        UserEmotionAnalysis updatedAnalysis = userEmotionAnalysisRepository.save(analysis);
        
        // 5. 응답 반환
        return UserEmotionAnalysisResponse.from(updatedAnalysis);
    }
    
    // 영어<->한국어 매핑 유틸리티
    private String toKr(String label) {
        if (label == null) return null;
        String v = emotionMapping.get(label); // en->kr
        if (v != null) return v;
        // 이미 KR이거나 알 수 없는 경우 원문 그대로 사용
        return label;
    }
    
    private String toEn(String kr) {
        if (kr == null) return "neutral";
        // 역매핑: 값이 KR인 entry의 key를 찾음
        return emotionMapping.entrySet().stream()
                .filter(e -> kr.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseGet(() -> {
                    // KR 그대로라면 간단 매핑
                    switch (kr) {
                        case "기쁨": return "joy";
                        case "당황": return "surprised";
                        case "분노": return "angry";
                        case "불안": return "fear";
                        case "상처": return "hurt";
                        case "슬픔": return "sad";
                        case "중립": return "neutral";
                        default: return "neutral";
                    }
                });
    }

    private static double clamp01(Double x) {
        if (x == null) return 0.0;
        return Math.max(0.0, Math.min(1.0, x));
    }
    
    private static <T> T argmax(Map<T, Double> m) {
        return m.entrySet().stream().max(Comparator.comparingDouble(Map.Entry::getValue)).get().getKey();
    }

    // 얼굴(프레임 여러 개, top1 라벨만 있음) -> 발화 단위 7라벨(KR, 중립 포함) 분포
    private Map<String, Double> buildFaceDist7(FacialEmotionData facial) {
        // 가중 누적 W_c
        Map<String, Double> Wc = new LinkedHashMap<>();
        // 초기화(안전)
        for (String l : LABELS6_KR) Wc.put(l, 0.0);
        Wc.put(NEUTRAL_KR, 0.0);

        double W = 0.0;

        // 1) 세부 프레임이 있으면 그것으로 가중 누적
        if (facial.getEmotionDetails() != null && !facial.getEmotionDetails().isEmpty()) {
            for (FacialEmotionData.EmotionDetail d : facial.getEmotionDetails()) {
                String kr = toKr(d.getEmotion());
                if (kr == null) continue;
                double conf = clamp01(d.getConfidence());
                if (conf < CONF_MIN) continue; // 선택적 필터
                double w = Math.pow(conf, GAMMA);
                Wc.merge(kr, w, Double::sum);
                W += w;
            }
        } else {
            // 2) 없으면 counts로 근사
            if (facial.getEmotionCounts() != null) {
                for (Map.Entry<String, Integer> e : facial.getEmotionCounts().entrySet()) {
                    String kr = toKr(e.getKey());
                    if (kr == null) continue;
                    double w = Math.max(0, e.getValue());
                    Wc.merge(kr, w, Double::sum);
                    W += w;
                }
            }
        }

        // 3) 디리클레 스무딩 후 정규화
        int K = LABELS6_KR.size() + 1; // 중립 포함
        double denom = W + ALPHA * K;
        Map<String, Double> p7 = new LinkedHashMap<>();
        for (String l : Wc.keySet()) {
            p7.put(l, (Wc.getOrDefault(l, 0.0) + ALPHA) / (denom > 0 ? denom : 1.0));
        }
        return p7;
    }

    // 7라벨(중립 포함) -> 6라벨(중립 제거) 재정규화
    private Map<String, Double> toSixWithoutNeutral(Map<String, Double> p7) {
        double neutral = p7.getOrDefault(NEUTRAL_KR, 0.0);
        double denom = 1.0 - neutral;
        Map<String, Double> p6 = new LinkedHashMap<>();
        if (denom <= 1e-9) {
            // 전부 중립이거나 정보 없음 => 균등분포
            double u = 1.0 / LABELS6_KR.size();
            for (String l : LABELS6_KR) p6.put(l, u);
            return p6;
        }
        for (String l : LABELS6_KR) {
            p6.put(l, p7.getOrDefault(l, 0.0) / denom);
        }
        return p6;
    }

    // map에서 필요한 라벨만 뽑아 정규화(안전장치)
    private Map<String, Double> normalizeTo6(Map<String, Object> allProbsRaw) {
        Map<String, Double> p = new LinkedHashMap<>();
        double sum = 0.0;
        for (String l : LABELS6_KR) {
            double v = 0.0;
            Object o = allProbsRaw.get(l);
            if (o instanceof Number) v = ((Number)o).doubleValue();
            // all_probabilities가 영어 키로 올 수도 있으니 방어적으로 한번 더
            if (v == 0.0) {
                String en = toEn(l); // KR->EN
                Object o2 = allProbsRaw.get(en);
                if (o2 instanceof Number) v = ((Number)o2).doubleValue();
            }
            v = Math.max(0.0, v);
            p.put(l, v);
            sum += v;
        }
        if (sum <= 1e-12) {
            double u = 1.0 / LABELS6_KR.size();
            for (String l : LABELS6_KR) p.put(l, u);
        } else {
            for (String l : LABELS6_KR) p.put(l, p.get(l) / sum);
        }
        return p;
    }

    // 선형 결합(모달 가중 * 품질 보정)
    private CombinedEmotionResult fuseDistributions(
            Map<String, Double> pTxt, Map<String, Double> pFace6,
            double speechConf, double faceQuality /*facial.averageConfidence 활용*/) {

        double wTxt = LAMBDA_TXT * clamp01(speechConf);
        double wFace = LAMBDA_FACE * clamp01(faceQuality);
        if (wTxt + wFace <= 1e-9) { wTxt = 1.0; wFace = 0.0; }

        Map<String, Double> acc = new LinkedHashMap<>();
        double Z = 0.0;
        for (String l : LABELS6_KR) {
            double v = wTxt * pTxt.getOrDefault(l, 0.0) + wFace * pFace6.getOrDefault(l, 0.0);
            acc.put(l, v);
            Z += v;
        }
        if (Z <= 1e-12) {
            double u = 1.0 / LABELS6_KR.size();
            for (String l : LABELS6_KR) acc.put(l, u);
            Z = 1.0;
        } else {
            for (String l : LABELS6_KR) acc.put(l, acc.get(l) / Z);
        }

        String finalKr = argmax(acc);
        double finalConf = acc.get(finalKr);
        String finalEn = toEn(finalKr);

        return new CombinedEmotionResult(finalEn, finalConf, acc);
    }

    // === 테스트용 API: 감정 통합 계산 과정 상세 조회 ===
    @Operation(summary = "감정 통합 계산 테스트", description = "facialEmotion과 speechEmotion을 입력받아 combinedEmotion 계산 과정을 상세히 보여줍니다")
    public Map<String, Object> testCombineEmotionCalculation(String facialEmotionJson, String speechEmotionJson) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            
            // 1) 파싱
            FacialEmotionData facial = objectMapper.readValue(facialEmotionJson, FacialEmotionData.class);
            Map<String, Object> speech = objectMapper.readValue(speechEmotionJson, Map.class);
            
            result.put("1_입력_데이터", Map.of(
                "facialEmotionJson", facialEmotionJson,
                "speechEmotionJson", speechEmotionJson
            ));
            
            result.put("2_파싱_결과", Map.of(
                "facial_finalEmotion", facial.getFinalEmotion(),
                "facial_averageConfidence", facial.getAverageConfidence(),
                "facial_emotionCounts", facial.getEmotionCounts(),
                "facial_emotionDetails", facial.getEmotionDetails(),
                "speech_predictedLabel", speech.get("predicted_label"),
                "speech_confidence", speech.get("confidence"),
                "speech_allProbabilities", speech.get("all_probabilities")
            ));
            
            // 2) 텍스트 분포/신뢰도 - 실제 DB 저장 형식에 맞게 파싱
            Map<String, Object> allProbsRaw;
            double speechConf;
            
            // analysisResult 구조인지 확인
            if (speech.containsKey("analysisResult")) {
                // 새로운 형식: {"text": "...", "analysisResult": {"details": {...}}}
                Map<String, Object> analysisResult = (Map<String, Object>) speech.get("analysisResult");
                Map<String, Object> details = (Map<String, Object>) analysisResult.get("details");
                allProbsRaw = (Map<String, Object>) details.get("all_probabilities");
                speechConf = ((Number) details.getOrDefault("confidence", 0.0)).doubleValue();
            } else {
                // 기존 형식: {"predicted_label": "...", "confidence": ..., "all_probabilities": {...}}
                allProbsRaw = (Map<String, Object>) speech.get("all_probabilities");
                speechConf = ((Number) speech.getOrDefault("confidence", 0.0)).doubleValue();
            }
            
            Map<String, Double> pTxt = normalizeTo6(allProbsRaw);
            
            result.put("3_텍스트_분석", Map.of(
                "speechConfidence", speechConf,
                "normalizedTextDistribution", pTxt
            ));
            
            // 3) 얼굴: 프레임 -> 7라벨 분포 -> 6라벨로 변환
            Map<String, Double> pFace7 = buildFaceDist7(facial);
            Map<String, Double> pFace6 = toSixWithoutNeutral(pFace7);
            double faceQuality = clamp01(facial.getAverageConfidence());
            
            result.put("4_얼굴_분석", Map.of(
                "faceQuality", faceQuality,
                "faceDistribution7Labels", pFace7,
                "faceDistribution6Labels", pFace6
            ));
            
            // 4) 분포 결합 과정
            double wTxt = LAMBDA_TXT * clamp01(speechConf);
            double wFace = LAMBDA_FACE * clamp01(faceQuality);
            if (wTxt + wFace <= 1e-9) { wTxt = 1.0; wFace = 0.0; }
            
            Map<String, Double> acc = new LinkedHashMap<>();
            double Z = 0.0;
            Map<String, Object> detailedCalculation = new LinkedHashMap<>();
            
            for (String l : LABELS6_KR) {
                double txtVal = pTxt.getOrDefault(l, 0.0);
                double faceVal = pFace6.getOrDefault(l, 0.0);
                double weightedTxt = wTxt * txtVal;
                double weightedFace = wFace * faceVal;
                double combined = weightedTxt + weightedFace;
                
                acc.put(l, combined);
                Z += combined;
                
                detailedCalculation.put(l, Map.of(
                    "textValue", txtVal,
                    "faceValue", faceVal,
                    "weightedText", weightedTxt,
                    "weightedFace", weightedFace,
                    "combined", combined
                ));
            }
            
            // 정규화
            Map<String, Double> normalizedAcc = new LinkedHashMap<>();
            if (Z <= 1e-12) {
                double u = 1.0 / LABELS6_KR.size();
                for (String l : LABELS6_KR) normalizedAcc.put(l, u);
            } else {
                for (String l : LABELS6_KR) normalizedAcc.put(l, acc.get(l) / Z);
            }
            
            String finalKr = argmax(normalizedAcc);
            double finalConf = normalizedAcc.get(finalKr);
            String finalEn = toEn(finalKr);
            
            result.put("5_결합_과정", Map.of(
                "weights", Map.of(
                    "lambdaText", LAMBDA_TXT,
                    "lambdaFace", LAMBDA_FACE,
                    "weightedText", wTxt,
                    "weightedFace", wFace
                ),
                "detailedCalculation", detailedCalculation,
                "sumBeforeNormalization", Z,
                "normalizedDistribution", normalizedAcc
            ));
            
            result.put("6_최종_결과", Map.of(
                "finalEmotionKorean", finalKr,
                "finalEmotionEnglish", finalEn,
                "finalConfidence", finalConf,
                "combinedEmotionResult", new CombinedEmotionResult(finalEn, finalConf, normalizedAcc)
            ));
            
            return result;
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("감정 데이터 JSON 파싱 실패: " + e.getMessage());
        }
    }

    // === 새로운 통합 감정 계산 메서드 ===
    private CombinedEmotionResult calculateCombinedEmotion(String facialEmotionJson, String speechEmotionJson) {
        try {
            // 1) 파싱
            FacialEmotionData facial = objectMapper.readValue(facialEmotionJson, FacialEmotionData.class);
            Map<String, Object> speech = objectMapper.readValue(speechEmotionJson, Map.class);

            // 2) 텍스트 분포/신뢰도 - 실제 DB 저장 형식에 맞게 파싱
            Map<String, Object> allProbsRaw;
            double speechConf;
            
            // analysisResult 구조인지 확인
            if (speech.containsKey("analysisResult")) {
                // 새로운 형식: {"text": "...", "analysisResult": {"details": {...}}}
                Map<String, Object> analysisResult = (Map<String, Object>) speech.get("analysisResult");
                Map<String, Object> details = (Map<String, Object>) analysisResult.get("details");
                allProbsRaw = (Map<String, Object>) details.get("all_probabilities");
                speechConf = ((Number) details.getOrDefault("confidence", 0.0)).doubleValue();
            } else {
                // 기존 형식: {"predicted_label": "...", "confidence": ..., "all_probabilities": {...}}
                allProbsRaw = (Map<String, Object>) speech.get("all_probabilities");
                speechConf = ((Number) speech.getOrDefault("confidence", 0.0)).doubleValue();
            }
            
            Map<String, Double> pTxt = normalizeTo6(allProbsRaw);

            // 3) 얼굴: 프레임 -> 7라벨 분포 -> 6라벨로 변환
            Map<String, Double> pFace7 = buildFaceDist7(facial);
            Map<String, Double> pFace6 = toSixWithoutNeutral(pFace7);
            double faceQuality = clamp01(facial.getAverageConfidence()); // 품질 프록시

            // 4) 분포 결합
            return fuseDistributions(pTxt, pFace6, speechConf, faceQuality);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("감정 데이터 JSON 파싱 실패: " + e.getMessage());
        }
    }
    
    // 통합 감정 결과를 담는 내부 클래스
    private static class CombinedEmotionResult {
        private final String emotion;
        private final Double confidence;
        private final Map<String, Double> pFused; // 통합 확률 분포
        
        public CombinedEmotionResult(String emotion, Double confidence, Map<String, Double> pFused) {
            this.emotion = emotion;
            this.confidence = confidence;
            this.pFused = pFused;
        }
        
        public String getEmotion() { return emotion; }
        public Double getConfidence() { return confidence; }
        public Map<String, Double> getPFused() { return pFused; }
    }
}
