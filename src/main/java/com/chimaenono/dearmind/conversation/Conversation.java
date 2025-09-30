package com.chimaenono.dearmind.conversation;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "conversations")
@Data @NoArgsConstructor @AllArgsConstructor
@Schema(description = "대화 세션 엔티티")
public class Conversation {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "대화 세션 고유 ID", example = "1")
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    @Column(name = "question_id", nullable = false)
    @Schema(description = "선택된 질문 ID", example = "1")
    private Long questionId;
    
    @Column(name = "camera_session_id")
    @Schema(description = "카메라 세션 ID", example = "camera_session_123")
    private String cameraSessionId;
    
    @Column(name = "microphone_session_id")
    @Schema(description = "마이크 세션 ID", example = "microphone_session_456")
    private String microphoneSessionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "대화 상태", example = "ACTIVE")
    private ConversationStatus status;
    
    @Column(name = "created_at", nullable = false)
    @Schema(description = "대화 시작 시간")
    private LocalDateTime createdAt;
    
    @Column(name = "ended_at")
    @Schema(description = "대화 종료 시간")
    private LocalDateTime endedAt;
    
    @Column(name = "summary", columnDefinition = "TEXT")
    @Schema(description = "대화 내용 요약", example = "사용자가 어린 시절 추억을 이야기하며...")
    private String summary;
    
    @Column(name = "diary", columnDefinition = "TEXT")
    @Schema(description = "생성된 일기 내용", example = "오늘은 정말 특별한 하루였습니다...")
    private String diary;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status")
    @Schema(description = "처리 상태", example = "READY")
    private ProcessingStatus processingStatus;
    
    @Column(name = "flow_pattern")
    @Schema(description = "대화 감정 패턴", example = "U-shape")
    private String flowPattern;                 // "U-shape" 등
    
    @Column(name = "emotion_flow", columnDefinition = "JSON")
    @Schema(description = "대화 감정 흐름 JSON", example = "{\"segments\":[...], \"metrics\":{...}}")
    private String emotionFlow;                 // segments + metrics 전체 JSON
    
    @Column(name = "is_public", nullable = false)
    @Schema(description = "앨범 공개 여부", example = "false")
    private Boolean isPublic = false;           // 기본값은 비공개
    
    @Column(name = "facet_history", columnDefinition = "JSON")
    @Schema(description = "대화 세부키 히스토리 (JSON 배열)", example = "[\"where\", \"who\", \"when\"]")
    private String facetHistoryJson;            // JSON 배열: ["where", "who", ...]
    
    @Column(name = "target_anchor_type")
    @Schema(description = "대화 앵커 타입", example = "place")
    private String targetAnchorType;            // person, place, event, timepoint, object, activity, quote, lesson
    
    @Column(name = "target_anchor_text")
    @Schema(description = "대화 앵커 텍스트", example = "작은 집")
    private String targetAnchorText;            // "작은 집", "어머니" 등
    
    public enum ConversationStatus {
        ACTIVE, COMPLETED, PAUSED
    }
    
    public enum ProcessingStatus {
        READY, PROCESSING, COMPLETED, ERROR
    }
    
    // JSON 변환을 위한 ObjectMapper (static)
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * facetHistory를 List<String>으로 반환
     */
    public List<String> getFacetHistory() {
        if (facetHistoryJson == null || facetHistoryJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(facetHistoryJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * facetHistory를 JSON 문자열로 저장
     */
    public void setFacetHistory(List<String> facetHistory) {
        try {
            this.facetHistoryJson = objectMapper.writeValueAsString(facetHistory);
        } catch (Exception e) {
            this.facetHistoryJson = "[]";
        }
    }
    
    /**
     * targetAnchor를 Map<String, String>으로 반환
     */
    public Map<String, String> getTargetAnchor() {
        Map<String, String> anchor = new HashMap<>();
        if (targetAnchorType != null && targetAnchorText != null) {
            anchor.put("type", targetAnchorType);
            anchor.put("text", targetAnchorText);
        }
        return anchor;
    }
    
    /**
     * targetAnchor를 개별 필드로 저장
     */
    public void setTargetAnchor(Map<String, String> anchor) {
        if (anchor != null && !anchor.isEmpty()) {
            this.targetAnchorType = anchor.get("type");
            this.targetAnchorText = anchor.get("text");
        }
    }
} 