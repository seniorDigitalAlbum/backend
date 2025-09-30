package com.chimaenono.dearmind.rdp;

import com.chimaenono.dearmind.conversation.Conversation;
import com.chimaenono.dearmind.conversation.ConversationRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * RDP (Reminiscence Data Point) 추출 및 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RdpService {

    private final ConversationRepository conversationRepository;

    /**
     * 대화 종료 시 RDP 데이터를 완성하고 저장합니다.
     * @param conversationId 대화 ID
     * @return 완성된 RDP 데이터
     */
    @Operation(summary = "RDP 완성 및 저장", description = "대화 중 수집된 facet 데이터를 RDP 형식으로 완성합니다")
    @Transactional
    public Map<String, Object> finalizeRdp(Long conversationId) {
        log.info("=== RDP 완성 시작 ===");
        log.info("ConversationId: {}", conversationId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("대화 세션을 찾을 수 없습니다: " + conversationId));
        
        // 현재 저장된 RDP 데이터 조회
        Map<String, Object> rdpData = conversation.getRdpData();
        
        if (rdpData.isEmpty()) {
            log.warn("RDP 데이터가 비어있음, 기본 구조로 초기화");
            rdpData = initializeRdpStructure(conversation.getTargetAnchor());
        }
        
        log.info("완성된 RDP: {}", rdpData);
        
        // Conversation에 저장
        conversation.setRdpData(rdpData);
        conversationRepository.save(conversation);
        
        log.info("=== RDP 완성 및 저장 완료 ===");
        return rdpData;
    }
    
    /**
     * RDP 구조 초기화
     */
    private Map<String, Object> initializeRdpStructure(Map<String, String> targetAnchor) {
        Map<String, Object> rdp = new HashMap<>();
        
        // anchor
        if (targetAnchor != null && !targetAnchor.isEmpty()) {
            rdp.put("anchor", targetAnchor);
        } else {
            Map<String, String> emptyAnchor = new HashMap<>();
            emptyAnchor.put("type", "");
            emptyAnchor.put("text", "");
            rdp.put("anchor", emptyAnchor);
        }
        
        // scene
        Map<String, String> scene = new HashMap<>();
        scene.put("where", "");
        scene.put("who", "");
        scene.put("when", "");
        scene.put("activity", "");
        rdp.put("scene", scene);
        
        // highlight
        Map<String, String> highlight = new HashMap<>();
        highlight.put("moment", "");
        highlight.put("quote", "");
        highlight.put("object_sense", "");
        highlight.put("action_expr", "");
        rdp.put("highlight", highlight);
        
        // meaning
        Map<String, String> meaning = new HashMap<>();
        meaning.put("feeling", "");
        meaning.put("meaning", "");
        meaning.put("impact", "");
        rdp.put("meaning", meaning);
        
        return rdp;
    }
    
    /**
     * RDP 데이터 조회
     */
    @Operation(summary = "RDP 데이터 조회", description = "저장된 RDP 데이터를 조회합니다")
    public Map<String, Object> getRdp(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("대화 세션을 찾을 수 없습니다: " + conversationId));
        
        return conversation.getRdpData();
    }
}
