# DearMind Backend

Spring Boot 기반의 AI 챗봇 백엔드 서버입니다.

## 프로젝트 구조

```
backend/
├── src/main/java/com/chimaenono/dearmind/
│   ├── DearmindApplication.java          # Spring Boot 메인 클래스
│   │
│   ├── album/                            # 앨범 관리
│   │   ├── AlbumController.java
│   │   ├── AlbumPhoto.java
│   │   └── AlbumComment.java
│   │
│   ├── camera/                           # 카메라 기능
│   │   ├── CameraController.java
│   │   └── CameraService.java
│   │
│   ├── config/                           # 설정 클래스
│   │   ├── SecurityConfig.java
│   │   ├── JwtConfig.java
│   │   ├── WebSocketConfig.java
│   │   └── SwaggerConfig.java
│   │
│   ├── conversation/                     # 대화 관리
│   │   ├── ConversationController.java
│   │   ├── ConversationService.java
│   │   └── EmotionFlowService.java
│   │
│   ├── ConversationMessage/              # 대화 메시지
│   │   ├── ConversationMessageController.java
│   │   └── ConversationMessageService.java
│   │
│   ├── diary/                            # 일기 기능
│   │   ├── DiaryPlan.java
│   │   └── EmotionFlow.java
│   │
│   ├── gpt/                              # GPT AI 서비스
│   │   ├── GPTController.java
│   │   └── GPTService.java
│   │
│   ├── guardian/                         # 보호자 기능
│   │   └── GuardianSeniorRelationship.java
│   │
│   ├── microphone/                       # 마이크 기능
│   │   ├── MicrophoneController.java
│   │   └── MicrophoneService.java
│   │
│   ├── music/                            # 음악 추천
│   │   ├── MusicRecommendation.java
│   │   └── YouTubeSearchService.java
│   │
│   ├── notification/                     # 알림 기능
│   │   ├── NotificationController.java
│   │   └── NotificationWebSocketHandler.java
│   │
│   ├── question/                         # 질문 관리
│   │   └── QuestionController.java
│   │
│   ├── s3/                               # AWS S3 파일 업로드
│   │   ├── S3Controller.java
│   │   └── S3Service.java
│   │
│   ├── stt/                              # Speech-to-Text
│   │   ├── STTController.java
│   │   └── STTService.java
│   │
│   ├── tts/                              # Text-to-Speech
│   │   ├── TTSController.java
│   │   └── TTSService.java
│   │
│   ├── user/                             # 사용자 관리
│   │   ├── UserController.java
│   │   ├── KakaoAuthController.java
│   │   └── User.java
│   │
│   └── userEmotionAnalysis/              # 감정 분석
│       ├── UserEmotionAnalysisController.java
│       └── CombineEmotionService.java
│
├── src/main/resources/
│   ├── application.yml                   # 기본 설정
│   ├── application-local.yml             # 로컬 환경 설정
│   ├── application-prod.yml              # 프로덕션 환경 설정
│   └── db/migration/                     # 데이터베이스 마이그레이션
│
├── build.gradle                          # Gradle 빌드 설정
├── Dockerfile                            # Docker 설정
└── README.md                             # 프로젝트 문서
```

## 주요 기능

- **AI 대화**: GPT 기반 자연스러운 대화
- **음성 처리**: STT/TTS 음성 인식 및 합성
- **감정 분석**: 얼굴 표정과 음성을 통한 감정 분석
- **앨범 관리**: 사진 업로드 및 관리 (AWS S3)
- **사용자 관리**: 카카오 로그인, JWT 인증
- **보호자 연동**: 시니어-보호자 연결 기능
- **실시간 알림**: WebSocket 기반 알림 시스템

## 기술 스택

- **Spring Boot 3.x** + **Spring Security**
- **MySQL** + **Spring Data JPA**
- **AWS S3** + **WebSocket**
- **JWT** + **Docker**

## 실행 방법

```bash
# 빌드 및 실행
./gradlew bootRun

# Docker 실행
docker build -t dearmind-backend .
docker run -p 8080:8080 dearmind-backend
```

## API 문서

- Swagger UI: `/swagger-ui.html`