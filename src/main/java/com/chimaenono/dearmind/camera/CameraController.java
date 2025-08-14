package com.chimaenono.dearmind.camera;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/camera")
@CrossOrigin(origins = "*", allowCredentials = "false")
@Tag(name = "Camera API", description = "카메라 세션 관리 API")
public class CameraController {

    @Autowired
    private CameraService cameraService;

    @PostMapping("/session")
    @Operation(
        summary = "카메라 세션 생성",
        description = "질문 선택 후 카메라 세션을 생성합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "세션 생성 성공",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<Map<String, Object>> createSession(
        @Parameter(description = "사용자 ID", example = "user_123")
        @RequestParam String userId,
        @Parameter(description = "선택된 질문 ID", example = "1")
        @RequestParam Long questionId
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            CameraSession session = cameraService.createSession(userId, questionId);
            response.put("status", "success");
            response.put("session", session);
            response.put("message", "카메라 세션이 생성되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "세션 생성 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/session/{sessionId}")
    @Operation(
        summary = "세션 정보 조회",
        description = "세션 ID로 카메라 세션 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "세션 조회 성공",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "세션을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<Map<String, Object>> getSession(
        @Parameter(description = "세션 ID", example = "session_12345")
        @PathVariable String sessionId
    ) {
        Map<String, Object> response = new HashMap<>();
        Optional<CameraSession> session = cameraService.getSessionById(sessionId);
        if (session.isPresent()) {
            response.put("status", "success");
            response.put("session", session.get());
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "세션을 찾을 수 없습니다.");
            return ResponseEntity.status(404).body(response);
        }
    }

    @PutMapping("/session/{sessionId}/status")
    @Operation(
        summary = "세션 상태 업데이트",
        description = "카메라 세션의 상태를 업데이트합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "상태 업데이트 성공",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "세션을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<Map<String, Object>> updateSessionStatus(
        @Parameter(description = "세션 ID", example = "session_12345")
        @PathVariable String sessionId,
        @Parameter(description = "새로운 상태", example = "RECORDING")
        @RequestParam String status
    ) {
        Map<String, Object> response = new HashMap<>();
        CameraSession session = cameraService.updateSessionStatus(sessionId, status);
        if (session != null) {
            response.put("status", "success");
            response.put("session", session);
            response.put("message", "세션 상태가 업데이트되었습니다.");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "세션을 찾을 수 없습니다.");
            return ResponseEntity.status(404).body(response);
        }
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(
        summary = "세션 종료",
        description = "카메라 세션을 종료합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "세션 종료 성공",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "세션을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<Map<String, Object>> endSession(
        @Parameter(description = "세션 ID", example = "session_12345")
        @PathVariable String sessionId
    ) {
        Map<String, Object> response = new HashMap<>();
        boolean success = cameraService.endSession(sessionId);
        if (success) {
            response.put("status", "success");
            response.put("message", "세션이 종료되었습니다.");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "세션을 찾을 수 없습니다.");
            return ResponseEntity.status(404).body(response);
        }
    }

    @GetMapping("/sessions/user/{userId}")
    @Operation(
        summary = "사용자 세션 목록",
        description = "특정 사용자의 모든 카메라 세션을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "세션 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<Map<String, Object>> getUserSessions(
        @Parameter(description = "사용자 ID", example = "user_123")
        @PathVariable String userId
    ) {
        Map<String, Object> response = new HashMap<>();
        List<CameraSession> sessions = cameraService.getSessionsByUserId(userId);
        response.put("status", "success");
        response.put("sessions", sessions);
        response.put("count", sessions.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(
        summary = "카메라 서비스 상태 확인",
        description = "카메라 서비스가 정상적으로 작동하는지 확인합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "서비스 정상",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "카메라 서비스가 정상적으로 작동 중입니다.");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
} 