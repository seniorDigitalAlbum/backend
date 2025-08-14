package com.chimaenono.dearmind.question;

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

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*", allowCredentials = "false")
@Tag(name = "Question API", description = "회상요법 질문 관련 API")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @GetMapping
    @Operation(
        summary = "모든 질문 조회",
        description = "데이터베이스에 저장된 모든 회상요법 질문을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "질문 조회 성공",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<Map<String, Object>> getAllQuestions() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Question> questions = questionService.getAllQuestions();
            response.put("status", "success");
            response.put("questions", questions);
            response.put("count", questions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to get questions: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "특정 질문 조회",
        description = "ID로 특정 질문을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "질문 조회 성공",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "질문을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<Map<String, Object>> getQuestionById(
        @Parameter(description = "질문 ID", example = "1")
        @PathVariable Long id
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            Question question = questionService.getQuestionById(id);
            if (question != null) {
                response.put("status", "success");
                response.put("question", question);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Question not found with id: " + id);
                return ResponseEntity.status(404).body(response);
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to get question: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/count")
    @Operation(
        summary = "질문 개수 조회",
        description = "저장된 질문의 총 개수를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "개수 조회 성공",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<Map<String, Object>> getQuestionCount() {
        Map<String, Object> response = new HashMap<>();
        try {
            long count = questionService.getQuestionCount();
            response.put("status", "success");
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to get question count: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/random")
    @Operation(
        summary = "랜덤 질문 조회",
        description = "저장된 질문 중 하나를 랜덤으로 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "랜덤 질문 조회 성공",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "질문이 없음",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<Map<String, Object>> getRandomQuestion() {
        Map<String, Object> response = new HashMap<>();
        try {
            Question randomQuestion = questionService.getRandomQuestion();
            if (randomQuestion != null) {
                response.put("status", "success");
                response.put("question", randomQuestion);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "No questions available");
                return ResponseEntity.status(404).body(response);
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to get random question: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
} 