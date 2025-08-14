package com.chimaenono.dearmind.tts;

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
@RequestMapping("/api/tts")
@CrossOrigin(origins = "*", allowCredentials = "false")
@Tag(name = "TTS API", description = "Text-to-Speech 변환 API")
public class TTSController {

    @Autowired
    private TTSService ttsService;

    @PostMapping("/synthesize")
    @Operation(
        summary = "텍스트를 음성으로 변환",
        description = "Naver CLOVA Voice API를 사용하여 텍스트를 음성으로 변환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "변환 성공",
            content = @Content(schema = @Schema(implementation = TTSResponse.class))
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
    public ResponseEntity<TTSResponse> synthesizeSpeech(@RequestBody TTSRequest request) {
        try {
            TTSResponse response = ttsService.synthesizeSpeech(
                request.getText(),
                request.getVoice(),
                request.getSpeed(),
                request.getPitch(),
                request.getVolume(),
                request.getFormat()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            TTSResponse errorResponse = new TTSResponse(
                null, "mp3", "nara", 0.0, "error", e.getMessage()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/simple")
    @Operation(
        summary = "간단한 TTS 변환",
        description = "기본 설정으로 텍스트를 음성으로 변환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "변환 성공",
            content = @Content(schema = @Schema(implementation = TTSResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<TTSResponse> synthesizeSimple(
        @Parameter(description = "변환할 텍스트", example = "안녕하세요!")
        @RequestParam String text
    ) {
        try {
            TTSResponse response = ttsService.synthesizeSpeech(text);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            TTSResponse errorResponse = new TTSResponse(
                null, "mp3", "nara", 0.0, "error", e.getMessage()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/health")
    @Operation(
        summary = "TTS 서비스 상태 확인",
        description = "TTS 서비스가 정상적으로 작동하는지 확인합니다."
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
        response.put("message", "TTS 서비스가 정상적으로 작동 중입니다.");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
} 