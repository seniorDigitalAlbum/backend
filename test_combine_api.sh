#!/bin/bash

# API 테스트 스크립트
BASE_URL="http://localhost:8080/api/emotion-analysis"

echo "=== 감정 분석 API 테스트 ==="

# 1. 서비스 상태 확인
echo "1. 서비스 상태 확인..."
curl -X GET "$BASE_URL/health"
echo -e "\n"

# 2. 표정 감정 분석 결과 저장
echo "2. 표정 감정 분석 결과 저장..."
FACIAL_RESPONSE=$(curl -s -X POST "$BASE_URL/facial" \
  -H "Content-Type: application/json" \
  -d '{
    "conversationMessageId": 123,
    "facialEmotionData": {
      "finalEmotion": "joy",
      "totalCaptures": 3,
      "emotionCounts": {
        "joy": 2,
        "neutral": 1
      },
      "averageConfidence": 0.85,
      "captureDetails": [
        {
          "timestamp": "2024-01-15T10:30:01",
          "emotion": "joy",
          "confidence": 0.92
        },
        {
          "timestamp": "2024-01-15T10:30:04",
          "emotion": "joy",
          "confidence": 0.78
        },
        {
          "timestamp": "2024-01-15T10:30:07",
          "emotion": "neutral",
          "confidence": 0.65
        }
      ]
    }
  }')

echo "표정 감정 저장 응답:"
echo "$FACIAL_RESPONSE"
echo -e "\n"

# 3. 말 감정 분석 결과 저장
echo "3. 말 감정 분석 결과 저장..."
SPEECH_RESPONSE=$(curl -s -X POST "$BASE_URL/speech" \
  -H "Content-Type: application/json" \
  -d '{
    "conversationMessageId": 123,
    "emotion": "기쁨",
    "confidence": 0.78,
    "speechEmotionData": "{\"emotion\":\"기쁨\",\"confidence\":0.78,\"details\":{}}"
  }')

echo "말 감정 저장 응답:"
echo "$SPEECH_RESPONSE"
echo -e "\n"

# 4. 통합 감정 계산
echo "4. 통합 감정 계산..."
COMBINE_RESPONSE=$(curl -s -X POST "$BASE_URL/combine" \
  -H "Content-Type: application/json" \
  -d '{
    "conversationMessageId": 123
  }')

echo "통합 감정 계산 응답:"
echo "$COMBINE_RESPONSE"
echo -e "\n"

# 5. 결과 조회
echo "5. 최종 결과 조회..."
FINAL_RESPONSE=$(curl -s -X GET "$BASE_URL/message/123")

echo "최종 결과:"
echo "$FINAL_RESPONSE"
echo -e "\n"

echo "=== 테스트 완료 ==="
