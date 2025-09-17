-- 1) Conversation
INSERT INTO conversations
(id, user_id, question_id, camera_session_id, microphone_session_id, status, created_at, ended_at, summary, diary, processing_status, flow_pattern, emotion_flow)
VALUES
(1001, 'user123', 1, 'cam_sess_1001', 'mic_sess_1001', 'COMPLETED',
 '2025-09-16 09:00:00', '2025-09-16 09:20:00',
 NULL, NULL, 'READY', NULL, NULL);

-- 2) USER 메시지 9건 (turn=1..9 느낌으로 시간만 1분 간격)
INSERT INTO conversation_messages (id, conversation_id, sender_type, content, timestamp) VALUES
(2001, 1001, 'USER', '요즘 밤에 잠이 잘 안 와요.',                  '2025-09-16 09:01:00'),
(2002, 1001, 'USER', '예전 생각이 자꾸 나서 조금 마음이 아파요.',      '2025-09-16 09:03:00'),
(2003, 1001, 'USER', '그래도 옛날 친구들과 놀던 기억은 좋아요.',        '2025-09-16 09:05:00'),
(2004, 1001, 'USER', '건강 결과를 기다리는 중이라 조금 긴장돼요.',      '2025-09-16 09:07:00'),
(2005, 1001, 'USER', '결과는 괜찮을 것 같기도 하고요.',               '2025-09-16 09:09:00'),
(2006, 1001, 'USER', '어릴 때 골목에서 숨바꼭질을 자주 했죠.',          '2025-09-16 09:12:00'),
(2007, 1001, 'USER', '그때 웃음소리가 아직도 귀에 맴돌아요.',           '2025-09-16 09:14:00'),
(2008, 1001, 'USER', '오늘도 산책을 하니 마음이 한결 편해졌어요.',       '2025-09-16 09:16:00'),
(2009, 1001, 'USER', '내일은 라디오도 들어야겠어요.',                  '2025-09-16 09:18:00');

-- 3) USER 감정 분석 9건 (각 메시지와 1:1, combined_distribution은 6라벨 합=1)
-- 초반(슬픔/상처) → 중반(불안) → 후반(기쁨)
INSERT INTO user_emotion_analysis
(id, conversation_message_id, facial_emotion, speech_emotion, combined_emotion, combined_confidence, combined_distribution, analysis_timestamp)
VALUES
-- turn 1: 슬픔 우세
(3001, 2001,
 '{"finalEmotion":"sad","totalCaptures":4,"emotionCounts":{"sad":2,"neutral":2},"averageConfidence":0.78,
   "emotionDetails":[
     {"emotion":"sad","confidence":0.85,"timestamp":"2025-09-16T09:01:01"},
     {"emotion":"neutral","confidence":0.70,"timestamp":"2025-09-16T09:01:03"},
     {"emotion":"sad","confidence":0.84,"timestamp":"2025-09-16T09:01:05"},
     {"emotion":"neutral","confidence":0.72,"timestamp":"2025-09-16T09:01:07"}
   ]}',
 '{"predicted_label":"슬픔","confidence":0.72,
   "all_probabilities":{"상처":0.12,"슬픔":0.55,"불안":0.20,"당황":0.05,"분노":0.03,"기쁨":0.05}}',
 'sad', 0.55,
 '{"기쁨":0.05,"당황":0.06,"분노":0.04,"불안":0.20,"상처":0.10,"슬픔":0.55}',
 '2025-09-16 09:01:10'),

-- turn 2: 상처 우세
(3002, 2002,
 '{"finalEmotion":"sad","totalCaptures":5,"emotionCounts":{"sad":3,"neutral":2},"averageConfidence":0.80,
   "emotionDetails":[
     {"emotion":"sad","confidence":0.88,"timestamp":"2025-09-16T09:03:01"},
     {"emotion":"neutral","confidence":0.75,"timestamp":"2025-09-16T09:03:04"},
     {"emotion":"sad","confidence":0.82,"timestamp":"2025-09-16T09:03:06"},
     {"emotion":"neutral","confidence":0.74,"timestamp":"2025-09-16T09:03:08"},
     {"emotion":"sad","confidence":0.86,"timestamp":"2025-09-16T09:03:10"}
   ]}',
 '{"predicted_label":"상처","confidence":0.65,
   "all_probabilities":{"상처":0.52,"슬픔":0.28,"불안":0.10,"당황":0.04,"분노":0.02,"기쁨":0.04}}',
 'hurt', 0.52,
 '{"기쁨":0.05,"당황":0.05,"분노":0.03,"불안":0.15,"상처":0.52,"슬픔":0.20}',
 '2025-09-16 09:03:12'),

-- turn 3: 아직 부정 우세(슬픔>기쁨)
(3003, 2003,
 '{"finalEmotion":"neutral","totalCaptures":3,"emotionCounts":{"neutral":2,"joy":1},"averageConfidence":0.76,
   "emotionDetails":[
     {"emotion":"neutral","confidence":0.78,"timestamp":"2025-09-16T09:05:01"},
     {"emotion":"joy","confidence":0.72,"timestamp":"2025-09-16T09:05:04"},
     {"emotion":"neutral","confidence":0.77,"timestamp":"2025-09-16T09:05:06"}
   ]}',
 '{"predicted_label":"슬픔","confidence":0.40,
   "all_probabilities":{"상처":0.12,"슬픔":0.40,"불안":0.15,"당황":0.06,"분노":0.03,"기쁨":0.24}}',
 'sad', 0.40,
 '{"기쁨":0.24,"당황":0.06,"분노":0.03,"불안":0.15,"상처":0.12,"슬픔":0.40}',
 '2025-09-16 09:05:10'),

-- turn 4: 불안 상승
(3004, 2004,
 '{"finalEmotion":"neutral","totalCaptures":4,"emotionCounts":{"neutral":3,"anxious":1},"averageConfidence":0.82,
   "emotionDetails":[
     {"emotion":"neutral","confidence":0.84,"timestamp":"2025-09-16T09:07:01"},
     {"emotion":"anxious","confidence":0.79,"timestamp":"2025-09-16T09:07:03"},
     {"emotion":"neutral","confidence":0.83,"timestamp":"2025-09-16T09:07:05"},
     {"emotion":"neutral","confidence":0.82,"timestamp":"2025-09-16T09:07:07"}
   ]}',
 '{"predicted_label":"불안","confidence":0.60,
   "all_probabilities":{"상처":0.06,"슬픔":0.10,"불안":0.60,"당황":0.12,"분노":0.04,"기쁨":0.08}}',
 'anxious', 0.60,
 '{"기쁨":0.08,"당황":0.12,"분노":0.04,"불안":0.60,"상처":0.06,"슬픔":0.10}',
 '2025-09-16 09:07:12'),

-- turn 5: 불안이지만 완화 조짐
(3005, 2005,
 '{"finalEmotion":"neutral","totalCaptures":4,"emotionCounts":{"neutral":3,"joy":1},"averageConfidence":0.80,
   "emotionDetails":[
     {"emotion":"neutral","confidence":0.82,"timestamp":"2025-09-16T09:09:01"},
     {"emotion":"neutral","confidence":0.79,"timestamp":"2025-09-16T09:09:03"},
     {"emotion":"joy","confidence":0.76,"timestamp":"2025-09-16T09:09:05"},
     {"emotion":"neutral","confidence":0.80,"timestamp":"2025-09-16T09:09:07"}
   ]}',
 '{"predicted_label":"불안","confidence":0.45,
   "all_probabilities":{"상처":0.05,"슬픔":0.08,"불안":0.45,"당황":0.10,"분노":0.04,"기쁨":0.28}}',
 'anxious', 0.45,
 '{"기쁨":0.28,"당황":0.10,"분노":0.04,"불안":0.45,"상처":0.05,"슬픔":0.08}',
 '2025-09-16 09:09:10'),

-- turn 6: 기쁨으로 전환
(3006, 2006,
 '{"finalEmotion":"joy","totalCaptures":5,"emotionCounts":{"joy":4,"neutral":1},"averageConfidence":0.86,
   "emotionDetails":[
     {"emotion":"joy","confidence":0.90,"timestamp":"2025-09-16T09:12:01"},
     {"emotion":"joy","confidence":0.85,"timestamp":"2025-09-16T09:12:03"},
     {"emotion":"neutral","confidence":0.78,"timestamp":"2025-09-16T09:12:05"},
     {"emotion":"joy","confidence":0.87,"timestamp":"2025-09-16T09:12:07"},
     {"emotion":"joy","confidence":0.88,"timestamp":"2025-09-16T09:12:09"}
   ]}',
 '{"predicted_label":"기쁨","confidence":0.60,
   "all_probabilities":{"상처":0.04,"슬픔":0.06,"불안":0.10,"당황":0.06,"분노":0.02,"기쁨":0.72}}',
 'joy', 0.72,
 '{"기쁨":0.72,"당황":0.06,"분노":0.02,"불안":0.10,"상처":0.04,"슬픔":0.06}',
 '2025-09-16 09:12:12'),

-- turn 7: 기쁨 유지(상승)
(3007, 2007,
 '{"finalEmotion":"joy","totalCaptures":4,"emotionCounts":{"joy":3,"neutral":1},"averageConfidence":0.88,
   "emotionDetails":[
     {"emotion":"joy","confidence":0.92,"timestamp":"2025-09-16T09:14:01"},
     {"emotion":"joy","confidence":0.89,"timestamp":"2025-09-16T09:14:03"},
     {"emotion":"neutral","confidence":0.80,"timestamp":"2025-09-16T09:14:05"},
     {"emotion":"joy","confidence":0.90,"timestamp":"2025-09-16T09:14:07"}
   ]}',
 '{"predicted_label":"기쁨","confidence":0.68,
   "all_probabilities":{"상처":0.03,"슬픔":0.05,"불안":0.07,"당황":0.05,"분노":0.02,"기쁨":0.78}}',
 'joy', 0.78,
 '{"기쁨":0.78,"당황":0.05,"분노":0.02,"불안":0.07,"상처":0.03,"슬픔":0.05}',
 '2025-09-16 09:14:10'),

-- turn 8: 기쁨 + 안정
(3008, 2008,
 '{"finalEmotion":"joy","totalCaptures":4,"emotionCounts":{"joy":3,"neutral":1},"averageConfidence":0.87,
   "emotionDetails":[
     {"emotion":"joy","confidence":0.90,"timestamp":"2025-09-16T09:16:01"},
     {"emotion":"neutral","confidence":0.79,"timestamp":"2025-09-16T09:16:03"},
     {"emotion":"joy","confidence":0.88,"timestamp":"2025-09-16T09:16:05"},
     {"emotion":"joy","confidence":0.89,"timestamp":"2025-09-16T09:16:07"}
   ]}',
 '{"predicted_label":"기쁨","confidence":0.70,
   "all_probabilities":{"상처":0.03,"슬픔":0.05,"불안":0.10,"당황":0.05,"분노":0.02,"기쁨":0.75}}',
 'joy', 0.75,
 '{"기쁨":0.75,"당황":0.05,"분노":0.02,"불안":0.10,"상처":0.03,"슬픔":0.05}',
 '2025-09-16 09:16:10'),

-- turn 9: 기쁨 유지(마무리)
(3009, 2009,
 '{"finalEmotion":"joy","totalCaptures":3,"emotionCounts":{"joy":2,"neutral":1},"averageConfidence":0.85,
   "emotionDetails":[
     {"emotion":"joy","confidence":0.88,"timestamp":"2025-09-16T09:18:01"},
     {"emotion":"neutral","confidence":0.78,"timestamp":"2025-09-16T09:18:03"},
     {"emotion":"joy","confidence":0.87,"timestamp":"2025-09-16T09:18:05"}
   ]}',
 '{"predicted_label":"기쁨","confidence":0.65,
   "all_probabilities":{"상처":0.03,"슬픔":0.05,"불안":0.08,"당황":0.05,"분노":0.02,"기쁨":0.77}}',
 'joy', 0.77,
 '{"기쁨":0.77,"당황":0.05,"분노":0.02,"불안":0.08,"상처":0.03,"슬픔":0.05}',
 '2025-09-16 09:18:10');
