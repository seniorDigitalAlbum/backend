package com.chimaenono.dearmind.rdp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RDP (Reminiscence Data Point) 관련 API
 */
@RestController
@RequestMapping("/api/rdp")
@Tag(name = "RDP", description = "회상 데이터 포인트 (RDP) 관리 API")
@CrossOrigin(origins = "*", allowCredentials = "false")
@RequiredArgsConstructor
public class RdpController {

    private final RdpService rdpService;

    @GetMapping("/{conversationId}")
    @Operation(summary = "RDP 데이터 조회", 
               description = "특정 대화 세션의 RDP (Reminiscence Data Point) 데이터를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "RDP 조회 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Map<String, Object>> getRdp(
            @Parameter(description = "대화 세션 ID", example = "1") 
            @PathVariable Long conversationId) {
        try {
            Map<String, Object> rdpData = rdpService.getRdp(conversationId);
            return ResponseEntity.ok(rdpData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "RDP 조회 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/finalize/{conversationId}")
    @Operation(summary = "RDP 완성 및 저장", 
               description = "대화 중 수집된 facet 데이터를 RDP 형식으로 완성합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "RDP 완성 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Map<String, Object>> finalizeRdp(
            @Parameter(description = "대화 세션 ID", example = "1") 
            @PathVariable Long conversationId) {
        try {
            Map<String, Object> rdpData = rdpService.finalizeRdp(conversationId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "rdp", rdpData,
                "conversationId", conversationId
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "RDP 완성 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
}
