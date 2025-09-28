package com.chimaenono.dearmind.microphone;

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
@RequestMapping("/api/microphone")
@CrossOrigin(origins = "*", allowCredentials = "false")
@Tag(name = "Microphone API", description = "마이크 세션 관리 API")
public class MicrophoneController {

    @Autowired
    private MicrophoneService microphoneService;


    @GetMapping("/session/{sessionId}")
    @Operation(
        summary = "세션 정보 조회",
        description = "세션 ID로 마이크 세션 정보를 조회합니다."
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
        Optional<MicrophoneSession> session = microphoneService.getSessionById(sessionId);
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


    @DeleteMapping("/session/{sessionId}")
    @Operation(
        summary = "세션 종료",
        description = "마이크 세션을 종료합니다."
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
        boolean success = microphoneService.endSession(sessionId);
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
        description = "특정 사용자의 모든 마이크 세션을 조회합니다."
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
        @PathVariable Long userId
    ) {
        Map<String, Object> response = new HashMap<>();
        List<MicrophoneSession> sessions = microphoneService.getSessionsByUserId(userId);
        response.put("status", "success");
        response.put("sessions", sessions);
        response.put("count", sessions.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/speech/start")
    @Operation(
        summary = "발화 시작",
        description = "사용자의 발화를 시작합니다. 마이크 세션 상태를 RECORDING으로 변경합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "발화 시작 성공",
            content = @Content(schema = @Schema(implementation = SpeechStartResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 데이터",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "마이크 세션을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<?> startSpeech(@RequestBody SpeechStartRequest request) {
        try {
            SpeechStartResponse response = microphoneService.startSpeech(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "발화 시작 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/speech/end")
    @Operation(
        summary = "발화 종료",
        description = "사용자의 발화를 종료합니다. 마이크 세션 상태를 ACTIVE로 변경하고 ConversationMessage를 생성합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "발화 종료 성공",
            content = @Content(schema = @Schema(implementation = SpeechEndResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 데이터",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "마이크 세션을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<?> endSpeech(@RequestBody SpeechEndRequest request) {
        try {
            SpeechEndResponse response = microphoneService.endSpeech(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "발화 종료 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/health")
    @Operation(
        summary = "마이크 서비스 상태 확인",
        description = "마이크 서비스가 정상적으로 작동하는지 확인합니다."
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
        response.put("message", "마이크 서비스가 정상적으로 작동 중입니다.");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
} 