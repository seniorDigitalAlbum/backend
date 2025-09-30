package com.chimaenono.dearmind.gpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 새로운 프롬프트 구조를 사용하는 GPT 서비스
 */
@Service
@Slf4j
public class GPTServiceNew {

    @Autowired
    private GPTService gptService;

    @Value("${openai.api.model:gpt-4}")
    private String defaultModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 새로운 프롬프트 구조로 감정 기반 대화 응답을 생성합니다.
     * @return JSON 형태의 응답 (Map<String, Object>)
     */
    public Map<String, Object> generateEmotionBasedResponse(
            String emotion, 
            Double confidence, 
            String prevUser, 
            String prevSys, 
            String currUser,
            String topicRoot,
            int stepIndex,
            int ruleStep,
            List<String> facetHistory,
            Map<String, String> targetAnchor) throws Exception {
        
        // 감정 매핑 (영어 -> 한국어)
        Map<String, String> emotionMapping = Map.of(
            "joy", "기쁨", "sadness", "슬픔", "anger", "분노",
            "fear", "불안", "surprise", "당황", "neutral", "중립",
            "hurt", "상처"
        );
        
        String emotionLabel = emotionMapping.getOrDefault(emotion, "중립");
        double conf = confidence;
        
        // facetHistory가 null이면 빈 리스트로 초기화
        if (facetHistory == null) {
            facetHistory = new ArrayList<>();
        }
        
        // targetAnchor가 null이면 빈 Map으로 초기화 (step_index=1일 때)
        if (targetAnchor == null) {
            targetAnchor = new HashMap<>();
        }
        
        // 새로운 프롬프트 구성
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("[역할]\n");
        promptBuilder.append("당신은 시니어와 대화하는 \"회상 대화 코치\"이자 첫 턴의 \"소재 추출기\"입니다.\n");
        promptBuilder.append("아주 짧고 쉬운 말로 말합니다.\n\n");
        
        promptBuilder.append("[주제]\n");
        promptBuilder.append("topic_root = \"").append(topicRoot).append("\"\n\n");
        
        promptBuilder.append("[모드 전환]\n");
        promptBuilder.append("- 모든 턴의 출력은 JSON 한 덩어리입니다.\n");
        promptBuilder.append("- step_index=1에서는 소재 추출을 추가로 수행합니다.\n\n");
        
        promptBuilder.append("[상태 입력(런타임)]\n");
        promptBuilder.append("- step_index: ").append(stepIndex).append("\n");
        promptBuilder.append("- rule_step: ").append(ruleStep).append("        // 서버가 계산해 주입. LLM은 절대 추정/변경 금지.\n");
        promptBuilder.append("- topic_root: \"").append(topicRoot).append("\"\n");
        promptBuilder.append("- curr_user: \"").append(currUser).append("\"\n");
        if (prevUser != null && !prevUser.trim().isEmpty()) {
            promptBuilder.append("- prev_user: \"").append(prevUser).append("\"\n");
        }
        if (prevSys != null && !prevSys.trim().isEmpty()) {
            promptBuilder.append("- prev_system: \"").append(prevSys).append("\"\n");
        }
        promptBuilder.append("- emotion_label: \"").append(emotionLabel).append("\"\n");
        promptBuilder.append("- confidence: ").append(conf).append("\n");
        if (!targetAnchor.isEmpty()) {
            promptBuilder.append("- target_anchor: {type: \"").append(targetAnchor.get("type"))
                    .append("\", text: \"").append(targetAnchor.get("text")).append("\"}\n");
        }
        promptBuilder.append("- facet_history: ").append(facetHistory.toString()).append("\n\n");
        
        promptBuilder.append("[스텝 매핑]\n");
        promptBuilder.append("- rule_step 값은 입력으로 제공되며, 이 턴의 질문 규칙은 반드시 rule_step에 따릅니다.\n");
        promptBuilder.append("  * rule_step=1 → 장면: where | who | when | activity\n");
        promptBuilder.append("  * rule_step=2 → 하이라이트: moment | quote | object_sense | action_expr\n");
        promptBuilder.append("  * rule_step=3 → 의미: feeling | meaning | impact\n");
        promptBuilder.append("- LLM은 rule_step를 절대 재계산/추정하지 않습니다.\n\n");
        
        promptBuilder.append("[step_index=1 전용]\n");
        promptBuilder.append("- curr_user에서 topic_root에 가장 맞는 target_anchor 1개를 추출합니다.\n");
        promptBuilder.append("     - 우선순위(동점이면 감정 강도 높은 쪽):\n");
        promptBuilder.append("       ① 구체명사/고유명사(사람·장소·이벤트·시간점·사물)\n");
        promptBuilder.append("       ② 장면 복원이 쉬운 것(사람/장소/이벤트)\n");
        promptBuilder.append("       ③ 주제와 직접 연결(키워드/의미)\n");
        promptBuilder.append("- 같은 응답에서 rule_step=1(장면) 규칙으로 문장형을 생성합니다.\n");
        promptBuilder.append("- 출력 JSON에 target_anchor를 포함합니다.\n");
        promptBuilder.append("- 이 턴에서 추출한 target_anchor는 이후 턴에서 변경·재명명 금지(사용자 명시 선택/전환 단계 제외).\n\n");
        
        promptBuilder.append("[문장형 출력 규칙]\n");
        promptBuilder.append("- 감정별 줄 수:\n");
        promptBuilder.append("  * emotion_label == \"기쁨\" → 정확히 3줄\n");
        promptBuilder.append("  * 그 외(불안·당황·분노·슬픔·상처) → 정확히 4줄\n");
        promptBuilder.append("- 각 줄 1문장, 6~18글자 권장, 전체 100~120자 이내.\n");
        promptBuilder.append("- 숫자/괄호/레이블/콜론 금지(예: \"1)\", \"선택지:\", \":\").\n");
        promptBuilder.append("- 질문은 정확히 1줄(보통 2줄 또는 3줄).\n");
        promptBuilder.append("- 존댓말만. 의학 조언/훈계/새 사실 창작 금지.\n\n");
        
        promptBuilder.append("[감정·신뢰도 규칙]\n");
        promptBuilder.append("- conf ≥ 0.7: 단정 어조(\"~하셨겠어요.\")\n");
        promptBuilder.append("- 0.4 ≤ conf < 0.7: 추정+확인(\"맞을까요?\")\n");
        promptBuilder.append("- conf < 0.4: 중립+열어두기(\"여러 감정이 오갔을 듯해요.\")\n\n");
        
        promptBuilder.append("[줄 구조]\n");
        promptBuilder.append("- emotion_label == \"기쁨\":\n");
        promptBuilder.append("  1) 공감+지지 1문장(감정 직호명 포함·밝게)\n");
        promptBuilder.append("  2) 공감 보강 1문장(확장 어휘: 사람/장소)\n");
        promptBuilder.append("  3) 회상질문 1문장(앵커 하위 한 요소)\n\n");
        promptBuilder.append("- emotion_label ∈ {불안, 당황, 분노, 슬픔, 상처}:\n");
        promptBuilder.append("  1) 공감+지지 1문장(감정 직호명 포함)\n");
        promptBuilder.append("  2) 공감 보강 1문장(안전/정리/존중 톤)\n");
        promptBuilder.append("  3) 회상질문 1문장(앵커 하위 한 요소; 한 축만)\n");
        promptBuilder.append("  4) 격려 1문장(안심·지속 의지, 멈춤/중단 뉘앙스 금지)\n\n");
        
        promptBuilder.append("[출력(JSON) 형태]\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"text\": \"<문장형 응답>\",\n");
        promptBuilder.append("  \"facet_key_used\": \"<enum>\",\n");
        promptBuilder.append("  \"facet_history\": [\"...\",\"...\"],\n");
        promptBuilder.append("  \"next_step_index\": <step_index + 1>");
        if (stepIndex == 1) {
            promptBuilder.append(",\n  \"target_anchor\": {\n");
            promptBuilder.append("    \"type\": \"person|event|timepoint|place|object|activity|quote|lesson\",\n");
            promptBuilder.append("    \"text\": \"<핵심 소재 원문>\"\n");
            promptBuilder.append("  }");
        }
        promptBuilder.append("\n}\n\n");
        
        promptBuilder.append("**[CRITICAL: facet_key_used 선택 규칙]**\n");
        promptBuilder.append("현재 rule_step=").append(ruleStep).append("이므로, facet_key_used는 **반드시** 아래 중 하나여야 합니다:\n");
        if (ruleStep == 1) {
            promptBuilder.append("→ where | who | when | activity (이 4개 중 1개만 가능)\n");
            promptBuilder.append("→ 현재 facet_history=").append(facetHistory.toString()).append("\n");
            promptBuilder.append("→ 이 중에서 facet_history에 **없는** 키를 우선 선택하세요.\n");
            promptBuilder.append("→ 4개가 모두 facet_history에 있으면, 아무거나 재사용 가능 (단, 이전 질문과 다른 표현으로).\n");
        } else if (ruleStep == 2) {
            promptBuilder.append("→ moment | quote | object_sense | action_expr (이 4개 중 1개만 가능)\n");
            promptBuilder.append("→ 현재 facet_history=").append(facetHistory.toString()).append("\n");
            promptBuilder.append("→ 이 중에서 facet_history에 **없는** 키를 우선 선택하세요.\n");
            promptBuilder.append("→ 4개가 모두 facet_history에 있으면, 아무거나 재사용 가능 (단, 이전 질문과 다른 표현으로).\n");
        } else if (ruleStep == 3) {
            promptBuilder.append("→ feeling | meaning | impact (이 3개 중 1개만 가능)\n");
            promptBuilder.append("→ 현재 facet_history=").append(facetHistory.toString()).append("\n");
            promptBuilder.append("→ 이 중에서 facet_history에 **없는** 키를 우선 선택하세요.\n");
            promptBuilder.append("→ 3개가 모두 facet_history에 있으면, 아무거나 재사용 가능 (단, 이전 질문과 다른 표현으로).\n");
        }
        promptBuilder.append("**절대 rule_step에 맞지 않는 키를 사용하지 마세요!**\n\n");
        
        promptBuilder.append("- facet_history에는 입력받은 facet_history 배열에 이번 턴의 facet_key_used를 추가한 **최신 배열**을 넣는다.\n");
        if (stepIndex >= 2) {
            promptBuilder.append("- step_index ≥ 2의 모든 질문은 입력으로 받은 target_anchor만 참조합니다(재명명·교체 금지).\n");
            promptBuilder.append("- step_index ≥ 2에서는 target_anchor 필드를 절대 출력하지 않는다(서버가 유지).\n");
        }
        promptBuilder.append("\n");
        
        promptBuilder.append("[3-스텝 질문 규칙(앵커 타입별 하위 축)]\n");
        promptBuilder.append("- rule_step=1 (장면 잡기):\n");
        promptBuilder.append("  - person  → 어디/언제/함께한 사람(관계) 중 1\n");
        promptBuilder.append("  - event   → 어디/누구/언제 중 1\n");
        promptBuilder.append("  - place   → 언제/누구/무엇(활동) 중 1\n");
        promptBuilder.append("  - timepoint → 어디/누구/무엇 중 1\n");
        promptBuilder.append("  - object  → 어디서 받음/누가 줌 중 1\n");
        promptBuilder.append("  - activity→ 어디/누구와 중 1\n");
        promptBuilder.append("  - quote   → 어디서 들음/누가 말함 중 1\n");
        promptBuilder.append("  - lesson  → 어떤 일에서 배움 중 1\n");
        promptBuilder.append("- rule_step=2 (하이라이트):\n");
        promptBuilder.append("  - person/event/place/timepoint → 가장 기억 남는 순간/한마디/표정·행동 중 1\n");
        promptBuilder.append("  - object → 가장 기억 남는 모습/색·감각/쓴 순간 중 1\n");
        promptBuilder.append("  - activity → 가장 기억 남는 장면/느낌/함께한 사람 중 1\n");
        promptBuilder.append("  - quote → 그 말이 남긴 장면/상황 중 1\n");
        promptBuilder.append("  - lesson → 배움이 드러난 순간/사건 중 1\n");
        promptBuilder.append("- rule_step=3 (의미·여운):\n");
        promptBuilder.append("  - 모든 타입 공통 → 지금 마음에 남는 느낌/의미 1\n\n");
        
        
        
        // GPT 요청 생성
        GPTRequest gptRequest = new GPTRequest();
        gptRequest.setModel(defaultModel);
        gptRequest.setMax_tokens(500);
        gptRequest.setTemperature(0.3);  // 규칙 준수를 위해 낮은 temperature 사용
        gptRequest.setStream(false);
        
        // 메시지 구성
        GPTMessage systemMessage = new GPTMessage("system", promptBuilder.toString());
        GPTMessage userMessage = new GPTMessage("user", 
            "위의 [CRITICAL: facet_key_used 선택 규칙]을 반드시 지켜서 JSON 응답을 생성해주세요. " +
            "rule_step=" + ruleStep + "에 해당하는 facet_key만 사용하세요.");
        
        gptRequest.setMessages(List.of(systemMessage, userMessage));
        
        // GPTService의 generateResponse 메서드 사용
        GPTResponse gptResponse = gptService.generateResponse(gptRequest);
        
        // 응답 텍스트 추출 및 JSON 파싱
        String rawResponse = gptResponse.getChoices().get(0).getMessage().getContent();
        
        log.info("GPT 원본 응답: {}", rawResponse);
        
        // JSON 파싱
        @SuppressWarnings("unchecked")
        Map<String, Object> parsedResponse = objectMapper.readValue(rawResponse, Map.class);
        
        return parsedResponse;
    }
}
