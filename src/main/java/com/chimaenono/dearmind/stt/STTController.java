package com.chimaenono.dearmind.stt;

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
import java.util.Map;

@RestController
@RequestMapping("/api/stt")
@CrossOrigin(origins = "*", allowCredentials = "false")
@Tag(name = "STT API", description = "Speech-to-Text 변환 API")
public class STTController {

    @Autowired
    private STTService sttService;

    @PostMapping("/transcribe")
    @Operation(
        summary = "오디오를 텍스트로 변환",
        description = "Whisper API를 사용하여 오디오를 텍스트로 변환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "변환 성공",
            content = @Content(schema = @Schema(implementation = STTResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<STTResponse> transcribeAudio(@RequestBody STTRequest request) {
        try {
            STTResponse response = sttService.transcribeAudio(
                request.getAudioData(),
                request.getFormat() != null ? request.getFormat() : "wav",
                request.getLanguage() != null ? request.getLanguage() : "ko"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            STTResponse errorResponse = new STTResponse(
                null, "ko", 0.0, 0.0, "error", e.getMessage()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/realtime")
    @Operation(
        summary = "실시간 STT 변환",
        description = "실시간 오디오 스트림을 텍스트로 변환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "변환 성공",
            content = @Content(schema = @Schema(implementation = STTResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<STTResponse> transcribeRealtime(
        @Parameter(description = "Base64 인코딩된 오디오 데이터")
        @RequestParam String audioData
    ) {
        try {
            STTResponse response = sttService.transcribeRealtime(audioData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            STTResponse errorResponse = new STTResponse(
                null, "ko", 0.0, 0.0, "error", e.getMessage()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/health")
    @Operation(
        summary = "STT 서비스 상태 확인",
        description = "STT 서비스가 정상적으로 작동하는지 확인합니다."
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
        response.put("message", "STT 서비스가 정상적으로 작동 중입니다.");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
} 