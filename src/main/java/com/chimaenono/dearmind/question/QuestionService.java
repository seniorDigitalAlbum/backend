package com.chimaenono.dearmind.question;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Service
@Tag(name = "Question Service", description = "회상요법 질문 관련 서비스")
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    @EventListener(ContextRefreshedEvent.class)
    @Operation(summary = "질문 초기화", description = "애플리케이션 시작 시 하드코딩된 회상요법 질문들을 데이터베이스에 저장")
    public void initializeQuestions() {
        try {
            // 이미 질문이 있으면 초기화하지 않음
            if (questionRepository.count() > 0) {
                return;
            }
        } catch (Exception e) {
            // 테이블이 아직 생성되지 않았거나 다른 오류가 발생한 경우
            System.out.println("Questions table not ready yet, skipping initialization: " + e.getMessage());
            return;
        }

        // 회상요법을 위한 하드코딩된 질문 리스트
        List<String> questionContents = Arrays.asList(
            "어린 시절 가장 기억에 남는 놀이는 무엇인가요?",
            "처음으로 직장에 다니게 되었을 때의 기분은 어땠나요?",
            "가장 인상 깊었던 여행은 언제, 어디였나요?",
            "어린 시절 가장 좋아했던 음식은 무엇인가요?",
            "첫사랑을 기억하시나요? 어떤 기분이었나요?",
            "가족과 함께한 가장 행복했던 순간은 언제인가요?",
            "어린 시절 가장 친했던 친구는 누구였나요?",
            "처음으로 집을 마련했을 때의 기분은 어땠나요?",
            "가장 기억에 남는 생일은 언제인가요?",
            "어린 시절 가장 무서웠던 경험은 무엇인가요?",
            "가장 자랑스러웠던 순간은 언제인가요?",
            "어린 시절 가장 좋아했던 장난감은 무엇인가요?",
            "가족과 함께한 가장 맛있었던 식사는 언제인가요?",
            "처음으로 자동차를 운전했을 때의 기분은 어땠나요?",
            "가장 기억에 남는 선생님은 누구였나요?",
            "어린 시절 가장 좋아했던 계절은 언제인가요?",
            "가족과 함께한 가장 재미있었던 게임은 무엇인가요?",
            "처음으로 해외여행을 갔을 때의 경험은 어땠나요?",
            "가장 기억에 남는 선물을 받았던 순간은 언제인가요?",
            "어린 시절 가장 좋아했던 동물은 무엇인가요?"
        );

        // 질문들을 데이터베이스에 저장
        for (String content : questionContents) {
            Question question = new Question();
            question.setContent(content);
            questionRepository.save(question);
        }
    }

    @Operation(summary = "모든 질문 조회", description = "데이터베이스에 저장된 모든 회상요법 질문을 조회")
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    @Operation(summary = "질문 ID로 조회", description = "특정 ID의 질문을 조회")
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id).orElse(null);
    }

    @Operation(summary = "질문 개수 조회", description = "저장된 질문의 총 개수를 조회")
    public long getQuestionCount() {
        return questionRepository.count();
    }

    @Operation(summary = "랜덤 질문 조회", description = "저장된 질문 중 하나를 랜덤으로 조회")
    public Question getRandomQuestion() {
        List<Question> questions = getAllQuestions();
        if (questions.isEmpty()) {
            return null;
        }
        int randomIndex = (int) (Math.random() * questions.size());
        return questions.get(randomIndex);
    }
} 