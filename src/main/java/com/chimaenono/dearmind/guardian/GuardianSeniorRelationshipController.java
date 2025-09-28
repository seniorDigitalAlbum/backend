package com.chimaenono.dearmind.guardian;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/relationships")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Guardian-Senior Relationship", description = "보호자-시니어 관계 관리 API")
public class GuardianSeniorRelationshipController {

    private final GuardianSeniorRelationshipService relationshipService;

    /**
     * 보호자-시니어 관계 요청 생성
     */
    @PostMapping("/request")
    @Operation(summary = "보호자-시니어 관계 요청 생성", description = "보호자가 시니어와의 관계를 요청합니다.")
    public ResponseEntity<Map<String, Object>> createRelationship(
            @Parameter(description = "보호자 ID", required = true)
            @RequestParam Long guardianId,
            @Parameter(description = "시니어 ID", required = true)
            @RequestParam Long seniorId) {

        log.info("보호자-시니어 관계 요청: guardianId={}, seniorId={}", guardianId, seniorId);

        try {
            GuardianSeniorRelationship relationship = relationshipService.createRelationship(guardianId, seniorId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "관계 요청이 생성되었습니다");
            response.put("relationship", relationship);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("관계 요청 생성 실패: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 관계 승인
     */
    @PutMapping("/{relationshipId}/approve")
    @Operation(summary = "관계 승인", description = "시니어가 보호자와의 관계를 승인합니다.")
    public ResponseEntity<Map<String, Object>> approveRelationship(
            @Parameter(description = "관계 ID", required = true)
            @PathVariable Long relationshipId,
            @Parameter(description = "시니어 ID", required = true)
            @RequestParam Long seniorId) {

        log.info("관계 승인: relationshipId={}, seniorId={}", relationshipId, seniorId);

        try {
            GuardianSeniorRelationship relationship = relationshipService.approveRelationship(relationshipId, seniorId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "관계가 승인되었습니다");
            response.put("relationship", relationship);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("관계 승인 실패: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 관계 거부
     */
    @PutMapping("/{relationshipId}/reject")
    @Operation(summary = "관계 거부", description = "시니어가 보호자와의 관계를 거부합니다.")
    public ResponseEntity<Map<String, Object>> rejectRelationship(
            @Parameter(description = "관계 ID", required = true)
            @PathVariable Long relationshipId,
            @Parameter(description = "시니어 ID", required = true)
            @RequestParam Long seniorId) {

        log.info("관계 거부: relationshipId={}, seniorId={}", relationshipId, seniorId);

        try {
            GuardianSeniorRelationship relationship = relationshipService.rejectRelationship(relationshipId, seniorId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "관계가 거부되었습니다");
            response.put("relationship", relationship);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("관계 거부 실패: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 보호자의 모든 관계 조회
     */
    @GetMapping("/guardian/{guardianId}")
    @Operation(summary = "보호자 관계 조회", description = "보호자의 모든 관계를 조회합니다.")
    public ResponseEntity<List<GuardianSeniorRelationship>> getGuardianRelationships(
            @Parameter(description = "보호자 ID", required = true)
            @PathVariable Long guardianId) {

        log.info("보호자 관계 조회: guardianId={}", guardianId);

        List<GuardianSeniorRelationship> relationships = relationshipService.getGuardianRelationships(guardianId);
        return ResponseEntity.ok(relationships);
    }

    /**
     * 시니어의 모든 관계 조회
     */
    @GetMapping("/senior/{seniorId}")
    @Operation(summary = "시니어 관계 조회", description = "시니어의 모든 관계를 조회합니다.")
    public ResponseEntity<List<GuardianSeniorRelationship>> getSeniorRelationships(
            @Parameter(description = "시니어 ID", required = true)
            @PathVariable Long seniorId) {

        log.info("시니어 관계 조회: seniorId={}", seniorId);

        List<GuardianSeniorRelationship> relationships = relationshipService.getSeniorRelationships(seniorId);
        return ResponseEntity.ok(relationships);
    }

    /**
     * 보호자의 승인된 관계만 조회
     */
    @GetMapping("/guardian/{guardianId}/approved")
    @Operation(summary = "보호자 승인된 관계 조회", description = "보호자의 승인된 관계만 조회합니다.")
    public ResponseEntity<List<GuardianSeniorRelationship>> getApprovedGuardianRelationships(
            @Parameter(description = "보호자 ID", required = true)
            @PathVariable Long guardianId) {

        log.info("보호자 승인된 관계 조회: guardianId={}", guardianId);

        List<GuardianSeniorRelationship> relationships = relationshipService.getApprovedGuardianRelationships(guardianId);
        return ResponseEntity.ok(relationships);
    }

    /**
     * 시니어의 승인된 관계만 조회
     */
    @GetMapping("/senior/{seniorId}/approved")
    @Operation(summary = "시니어 승인된 관계 조회", description = "시니어의 승인된 관계만 조회합니다.")
    public ResponseEntity<List<GuardianSeniorRelationship>> getApprovedSeniorRelationships(
            @Parameter(description = "시니어 ID", required = true)
            @PathVariable Long seniorId) {

        log.info("시니어 승인된 관계 조회: seniorId={}", seniorId);

        List<GuardianSeniorRelationship> relationships = relationshipService.getApprovedSeniorRelationships(seniorId);
        return ResponseEntity.ok(relationships);
    }

    /**
     * 시니어의 대기 중인 관계 조회
     */
    @GetMapping("/senior/{seniorId}/pending")
    @Operation(summary = "시니어 대기 중인 관계 조회", description = "시니어의 대기 중인 관계를 조회합니다.")
    public ResponseEntity<List<GuardianSeniorRelationship>> getPendingSeniorRelationships(
            @Parameter(description = "시니어 ID", required = true)
            @PathVariable Long seniorId) {

        log.info("시니어 대기 중인 관계 조회: seniorId={}", seniorId);

        List<GuardianSeniorRelationship> relationships = relationshipService.getPendingSeniorRelationships(seniorId);
        return ResponseEntity.ok(relationships);
    }

    /**
     * 관계 삭제
     */
    @DeleteMapping("/{relationshipId}")
    @Operation(summary = "관계 삭제", description = "관계를 삭제합니다.")
    public ResponseEntity<Map<String, Object>> deleteRelationship(
            @Parameter(description = "관계 ID", required = true)
            @PathVariable Long relationshipId,
            @Parameter(description = "사용자 ID", required = true)
            @RequestParam Long userId) {

        log.info("관계 삭제: relationshipId={}, userId={}", relationshipId, userId);

        try {
            boolean deleted = relationshipService.deleteRelationship(relationshipId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "관계가 삭제되었습니다");
            response.put("deleted", deleted);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("관계 삭제 실패: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
