package com.chimaenono.dearmind.album;

import com.chimaenono.dearmind.conversation.ConversationRepository;
import com.chimaenono.dearmind.user.User;
import com.chimaenono.dearmind.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlbumCommentService {

    private final AlbumCommentRepository albumCommentRepository;
    private final ConversationRepository conversationRepository;
    private final UserService userService;

    /**
     * 특정 대화의 댓글 목록을 조회합니다.
     */
    public List<AlbumComment> getCommentsByConversationId(Long conversationId) {
        try {
            log.info("🔍 대화 ID {}의 댓글 목록 조회 시작", conversationId);
            
            // 대화 존재 여부 확인
            if (!conversationRepository.existsById(conversationId)) {
                log.error("존재하지 않는 대화 ID: {}", conversationId);
                throw new IllegalArgumentException("존재하지 않는 대화입니다: " + conversationId);
            }
            
            List<AlbumComment> comments = albumCommentRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);
            log.info("✅ 대화 ID {}의 댓글 {}개 조회 완료", conversationId, comments.size());
            return comments;
        } catch (Exception e) {
            log.error("❌ 댓글 목록 조회 중 예외 발생: conversationId={}, error={}", conversationId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 새로운 댓글을 추가합니다.
     */
    @Transactional
    public AlbumComment addComment(Long conversationId, String content, Long userId) {
        log.info("대화 ID {}에 댓글 추가: 사용자ID={}, 내용={}", conversationId, userId, content);

        // 대화 존재 여부 확인
        conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대화입니다: " + conversationId));

        // 댓글 생성 및 저장
        AlbumComment comment = AlbumComment.builder()
                .conversationId(conversationId)
                .userId(userId)
                .content(content)
                .build();

        AlbumComment savedComment = albumCommentRepository.save(comment);
        log.info("댓글 추가 완료: ID={}", savedComment.getId());

        return savedComment;
    }

    /**
     * 댓글을 삭제합니다.
     */
    @Transactional
    public void deleteComment(Long commentId) {
        log.info("댓글 삭제: ID={}", commentId);
        
        AlbumComment comment = albumCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다: " + commentId));

        albumCommentRepository.delete(comment);
        log.info("댓글 삭제 완료: ID={}", commentId);
    }

    /**
     * 특정 대화의 모든 댓글을 삭제합니다.
     */
    @Transactional
    public void deleteCommentsByConversationId(Long conversationId) {
        log.info("대화 ID {}의 모든 댓글 삭제", conversationId);
        albumCommentRepository.deleteByConversationId(conversationId);
        log.info("대화 ID {}의 모든 댓글 삭제 완료", conversationId);
    }

    /**
     * 특정 대화의 댓글 개수를 조회합니다.
     */
    public long getCommentCountByConversationId(Long conversationId) {
        return albumCommentRepository.countByConversationId(conversationId);
    }

    /**
     * 특정 사용자가 작성한 댓글 목록을 조회합니다.
     */
    public List<AlbumComment> getCommentsByUserId(Long userId) {
        log.info("사용자 ID {}의 댓글 목록 조회", userId);
        return albumCommentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 특정 대화의 댓글 목록을 작성자 정보와 함께 조회합니다.
     */
    public List<AlbumCommentResponse> getCommentsWithAuthorInfo(Long conversationId) {
        log.info("대화 ID {}의 댓글 목록을 작성자 정보와 함께 조회", conversationId);
        
        List<AlbumComment> comments = getCommentsByConversationId(conversationId);
        
        return comments.stream()
                .map(comment -> {
                    User author = userService.findById(comment.getUserId())
                            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + comment.getUserId()));
                    
                    return AlbumCommentResponse.from(
                            comment,
                            author.getNickname(),
                            author.getProfileImageUrl()
                    );
                })
                .collect(Collectors.toList());
    }
}
