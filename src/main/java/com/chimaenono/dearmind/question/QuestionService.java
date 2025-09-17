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
            "제일 오래된 기억이 뭐예요?",
            "어디서 자라셨어요?",
            "어릴 적 동네는 어땠나요?",
            "부모님은 어떤 분들이셨어요?",
            "형제자매랑은 사이가 어땠나요?",
            "어릴 때 제일 즐거웠던 건 뭐예요?",
            "좋아했던 밖에서 노는 놀이는 뭐였나요?",
            "가족 여행 간 적 있으세요? 기억나는 건요?",
            "명절 때 가족 모임은 어땠나요?",
            "학교생활은 어땠나요?",
            "기억에 남는 선생님이 있으세요?",
            "고등학교 때 무슨 활동 하셨어요?",
            "10대 때 중요한 순간이 있었다면 뭐예요?",
            "첫사랑은 누구였나요?",
            "배우자는 어떻게 만나셨어요?",
            "결혼 생활 중 가장 소중한 순간은요?",
            "자녀가 있으세요? 키우면서 즐거웠던 점은요?",
            "자녀나 손주와의 추억이 있나요?",
            "반려동물 키워본 적 있으세요?",
            "첫 직장은 어디였고 기분은 어땠나요?",
            "평생 해온 주된 일은 뭐예요?",
            "직장 생활 중 좋았던 점, 힘들었던 점은요?",
            "군 복무 경험이 있으세요?",
            "인생에서 가장 자랑스러운 성취는요?",
            "제일 힘들었던 도전은 뭐였나요?",
            "큰 역사적 사건을 겪은 적 있으세요?",
            "전화기·컴퓨터 같은 기술 변화를 어떻게 보셨어요?",
            "어른이 되고 제일 좋았던 여행지는 어디예요?",
            "삶에 제일 큰 영향을 준 사람은 누구예요?",
            "시기별로 영향을 준 사람은 누구였나요?",
            "인생에서 가장 큰 모험은요?",
            "봉사활동이나 단체 활동 해보신 적 있으세요?",
            "인생에서 배운 가장 큰 교훈은요?",
            "시간이 지나면서 가치관이 바뀐 적 있나요?",
            "종교나 신앙이 삶에 어떤 영향을 줬나요?",
            "다시 살고 싶은 하루가 있다면 언제예요?",
            "삶에서 바꾸고 싶은 게 있다면 뭐예요?",
            "실수 중에서 교훈이 된 게 있다면요?",
            "본인의 가장 큰 강점은 뭐라고 생각하세요?",
            "친구 관계는 어떻게 변해왔나요?",
            "지금 삶에서 중요한 사람은 누구예요?",
            "요즘 가장 행복하게 하는 건 뭐예요?",
            "인생에서 가장 감사한 건 뭐예요?",
            "젊은 시절 자신에게 어떤 말을 해주고 싶으세요?",
            "다음 세대에 남기고 싶은 건 뭐예요?",
            "사람들이 당신을 어떻게 기억했으면 하나요?",
            "인생에서 가장 중요했던 세 가지는 뭐예요?",
            "삶을 돌아보며 새롭게 배운 게 있나요?",
            "지금 인생에서 가장 중요한 건 뭐예요?",
            "가까운 사람들과 연결됐다고 느끼는 순간은 언제예요?"
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