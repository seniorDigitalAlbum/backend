# DearMind API 사용 가이드

## 📋 목차
1. [개요](#개요)
2. [기본 설정](#기본-설정)
3. [API 엔드포인트](#api-엔드포인트)
4. [프론트엔드 통합 예시](#프론트엔드-통합-예시)
5. [에러 처리](#에러-처리)
6. [테스트 방법](#테스트-방법)

## 개요

DearMind는 AI와의 대화를 통한 회상요법 서비스를 제공하는 백엔드 API입니다. 
사용자가 질문을 선택하고 AI와 대화한 후, 대화 내용을 바탕으로 일기와 음악 추천을 제공합니다.

### 주요 기능
- **질문 목록 조회**: 회상요법 질문들 조회
- **통합 대화 시작**: 카메라/마이크 세션 + 대화방 생성
- **음성 대화**: STT → GPT 응답 → TTS 변환
- **감정 분석**: 표정 및 음성 감정 분석
- **일기 생성**: 대화 내용 기반 일기 자동 생성
- **음악 추천**: 감정 기반 음악 추천

## 기본 설정

### Base URL
- **로컬 개발**: `http://localhost:8080/api`
- **운영 환경**: `https://your-domain.com/api`

### Swagger UI
- **로컬**: `http://localhost:8080/swagger-ui.html`
- **운영**: `https://your-domain.com/swagger-ui.html`

### Content-Type
모든 API 요청은 `application/json` 형식을 사용합니다.

## API 엔드포인트

### 1. 질문 목록 조회

#### 엔드포인트
```
GET /api/questions
```

#### 응답
```json
{
  "status": "success",
  "questions": [
    {
      "id": 1,
      "content": "어린 시절 가장 기억에 남는 놀이는 무엇인가요?"
    },
    {
      "id": 2,
      "content": "처음으로 직장에 다니게 되었을 때의 기분은 어땠나요?"
    }
  ],
  "count": 20
}
```

### 2. 대화 시작 (통합 API) ⭐

#### 엔드포인트
```
POST /api/conversations/start
```

#### 요청
```json
{
  "userId": "user123",
  "questionId": 5
}
```

#### 응답
```json
{
  "conversationId": 1,
  "cameraSessionId": "cam_123",
  "microphoneSessionId": "mic_456",
  "status": "ACTIVE",
  "question": {
    "id": 5,
    "content": "어린 시절 가장 기억에 남는 놀이는 무엇인가요?"
  },
  "message": "대화가 성공적으로 시작되었습니다."
}
```

### 3. 발화 시작

#### 엔드포인트
```
POST /api/microphone/speech/start
```

#### 요청
```json
{
  "microphoneSessionId": "mic_456",
  "cameraSessionId": "cam_123",
  "userId": "user123"
}
```

#### 응답
```json
{
  "status": "success",
  "message": "발화가 시작되었습니다.",
  "microphoneSessionId": "mic_456",
  "cameraSessionId": "cam_123",
  "userId": "user123"
}
```

### 4. 발화 종료 (STT + GPT + TTS)

#### 엔드포인트
```
POST /api/microphone/speech/end
```

#### 요청
```json
{
  "microphoneSessionId": "mic_456",
  "cameraSessionId": "cam_123",
  "userId": "user123",
  "audioData": "base64_encoded_audio_data"
}
```

#### 응답
```json
{
  "status": "success",
  "message": "발화가 종료되었습니다.",
  "conversationMessageId": 123,
  "aiResponse": "정말 흥미로운 이야기네요! 그 놀이를 할 때 어떤 기분이었나요?",
  "audioBase64": "base64_encoded_tts_audio"
}
```

### 5. 대화 종료

#### 엔드포인트
```
PUT /api/conversations/{conversationId}/end
```

#### 응답
```json
{
  "conversationId": 1,
  "status": "COMPLETED",
  "processingStatus": "PROCESSING",
  "messages": [
    {
      "id": 1,
      "content": "어릴 때 자주 했던 놀이는 숨바꼭질이었어요.",
      "senderType": "USER",
      "createdAt": "2024-01-15T10:30:00"
    },
    {
      "id": 2,
      "content": "정말 흥미로운 이야기네요! 그 놀이를 할 때 어떤 기분이었나요?",
      "senderType": "AI",
      "createdAt": "2024-01-15T10:30:05"
    }
  ],
  "message": "일기 생성 중입니다..."
}
```

### 6. 처리 상태 확인

#### 엔드포인트
```
GET /api/conversations/{conversationId}/processing-status
```

#### 응답
```json
{
  "conversationId": 1,
  "status": "COMPLETED",
  "summaryCompleted": true,
  "diaryCompleted": true,
  "message": "처리가 완료되었습니다.",
  "success": true
}
```

### 7. 일기 조회

#### 엔드포인트
```
GET /api/conversations/{conversationId}/diary
```

#### 응답
```json
{
  "conversationId": 1,
  "summary": "사용자가 어린 시절 숨바꼭질 놀이에 대한 추억을 나누었습니다.",
  "diary": "오늘은 어린 시절의 소중한 추억에 대해 이야기했습니다...",
  "emotionSummary": {
    "dominantEmotion": "기쁨",
    "averageConfidence": 0.85,
    "analyzedMessageCount": 3,
    "emotionCounts": {
      "기쁨": 2,
      "그리움": 1
    }
  },
  "musicRecommendations": [
    {
      "id": 1,
      "title": "어린 시절",
      "artist": "김광석",
      "mood": "그리움",
      "youtubeLink": "https://www.youtube.com/watch?v=example",
      "youtubeVideoId": "example"
    }
  ],
  "message": "일기를 성공적으로 조회했습니다."
}
```

## 프론트엔드 통합 예시

### React Native 예시

```javascript
// API 서비스 클래스
class DearMindAPI {
  constructor(baseURL = 'http://localhost:8080/api') {
    this.baseURL = baseURL;
  }

  async request(endpoint, options = {}) {
    const url = `${this.baseURL}${endpoint}`;
    const config = {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    };

    try {
      const response = await fetch(url, config);
      const data = await response.json();
      
      if (!response.ok) {
        throw new Error(data.message || 'API 요청 실패');
      }
      
      return data;
    } catch (error) {
      console.error('API Error:', error);
      throw error;
    }
  }

  // 1. 질문 목록 조회
  async getQuestions() {
    return this.request('/questions');
  }

  // 2. 대화 시작 (통합)
  async startConversation(userId, questionId) {
    return this.request('/conversations/start', {
      method: 'POST',
      body: JSON.stringify({ userId, questionId }),
    });
  }

  // 3. 발화 시작
  async startSpeech(microphoneSessionId, cameraSessionId, userId) {
    return this.request('/microphone/speech/start', {
      method: 'POST',
      body: JSON.stringify({
        microphoneSessionId,
        cameraSessionId,
        userId,
      }),
    });
  }

  // 4. 발화 종료
  async endSpeech(microphoneSessionId, cameraSessionId, userId, audioData) {
    return this.request('/microphone/speech/end', {
      method: 'POST',
      body: JSON.stringify({
        microphoneSessionId,
        cameraSessionId,
        userId,
        audioData,
      }),
    });
  }

  // 5. 대화 종료
  async endConversation(conversationId) {
    return this.request(`/conversations/${conversationId}/end`, {
      method: 'PUT',
    });
  }

  // 6. 처리 상태 확인
  async getProcessingStatus(conversationId) {
    return this.request(`/conversations/${conversationId}/processing-status`);
  }

  // 7. 일기 조회
  async getDiary(conversationId) {
    return this.request(`/conversations/${conversationId}/diary`);
  }
}

// 사용 예시
const api = new DearMindAPI();

// 대화 플로우 예시
async function startConversationFlow() {
  try {
    // 1. 질문 목록 조회
    const questionsResponse = await api.getQuestions();
    const questions = questionsResponse.questions;
    
    // 2. 사용자가 질문 선택 (예: 첫 번째 질문)
    const selectedQuestion = questions[0];
    
    // 3. 대화 시작
    const startResponse = await api.startConversation('user123', selectedQuestion.id);
    const { conversationId, cameraSessionId, microphoneSessionId } = startResponse;
    
    // 4. 발화 시작
    await api.startSpeech(microphoneSessionId, cameraSessionId, 'user123');
    
    // 5. 사용자 음성 녹음 후 발화 종료
    const audioData = 'base64_encoded_audio'; // 실제 오디오 데이터
    const endResponse = await api.endSpeech(microphoneSessionId, cameraSessionId, 'user123', audioData);
    
    // 6. AI 응답 재생
    if (endResponse.audioBase64) {
      playAudio(endResponse.audioBase64);
    }
    
    // 7. 대화 종료
    await api.endConversation(conversationId);
    
    // 8. 일기 생성 완료까지 대기 (폴링)
    let processingComplete = false;
    while (!processingComplete) {
      const statusResponse = await api.getProcessingStatus(conversationId);
      if (statusResponse.status === 'COMPLETED') {
        processingComplete = true;
      } else {
        await new Promise(resolve => setTimeout(resolve, 2000)); // 2초 대기
      }
    }
    
    // 9. 일기 조회
    const diaryResponse = await api.getDiary(conversationId);
    console.log('생성된 일기:', diaryResponse.diary);
    console.log('음악 추천:', diaryResponse.musicRecommendations);
    
  } catch (error) {
    console.error('대화 플로우 오류:', error);
  }
}
```

### JavaScript/Web 예시

```javascript
// 오디오 재생 함수
function playAudio(base64Audio) {
  const audio = new Audio(`data:audio/wav;base64,${base64Audio}`);
  audio.play();
}

// YouTube 플레이어 설정 (일기 페이지에서)
function setupYouTubePlayer(videoId) {
  const player = new YT.Player('player', {
    height: '200',
    width: '100%',
    videoId: videoId,
    playerVars: {
      autoplay: 0,
      controls: 1,
    },
    events: {
      'onReady': onPlayerReady,
      'onStateChange': onPlayerStateChange
    }
  });
}
```

## 에러 처리

### 일반적인 에러 응답 형식

```json
{
  "status": "error",
  "message": "에러 메시지",
  "timestamp": "2024-01-15T10:30:00"
}
```

### HTTP 상태 코드

- **200**: 성공
- **400**: 잘못된 요청 (필수 파라미터 누락, 잘못된 형식)
- **404**: 리소스를 찾을 수 없음
- **500**: 서버 내부 오류

### 에러 처리 예시

```javascript
try {
  const response = await api.startConversation('user123', 5);
  // 성공 처리
} catch (error) {
  if (error.message.includes('존재하지 않는 질문')) {
    // 질문 ID 오류 처리
    showError('선택한 질문을 찾을 수 없습니다.');
  } else if (error.message.includes('사용자 ID는 필수')) {
    // 사용자 ID 오류 처리
    showError('사용자 정보가 필요합니다.');
  } else {
    // 일반 오류 처리
    showError('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
  }
}
```

## 테스트 방법

### 1. Swagger UI 사용
1. `http://localhost:8080/swagger-ui.html` 접속
2. 각 API 엔드포인트 클릭
3. "Try it out" 버튼 클릭
4. 요청 데이터 입력 후 "Execute" 클릭

### 2. cURL 명령어

```bash
# 질문 목록 조회
curl -X GET "http://localhost:8080/api/questions"

# 대화 시작
curl -X POST "http://localhost:8080/api/conversations/start" \
  -H "Content-Type: application/json" \
  -d '{"userId": "user123", "questionId": 1}'

# 발화 시작
curl -X POST "http://localhost:8080/api/microphone/speech/start" \
  -H "Content-Type: application/json" \
  -d '{"microphoneSessionId": "mic_456", "cameraSessionId": "cam_123", "userId": "user123"}'
```

### 3. 테스트 데이터 생성

```bash
# 더미 대화 데이터 생성
curl -X POST "http://localhost:8080/api/conversations/dummy/user123"
```

## 주의사항

1. **음성 데이터**: Base64로 인코딩하여 전송
2. **세션 관리**: 각 대화마다 고유한 세션 ID 사용
3. **폴링**: 일기 생성 완료까지 2초 간격으로 상태 확인
4. **에러 처리**: 모든 API 호출에 적절한 에러 처리 구현
5. **CORS**: 개발 환경에서 CORS 설정 확인

## 지원

문제가 발생하거나 추가 기능이 필요한 경우, 백엔드 개발자에게 문의하세요.

---

**마지막 업데이트**: 2024년 1월 15일
**API 버전**: v1.0
