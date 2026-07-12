package io.hz.guidegenie.qna;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnaMessageRepository extends JpaRepository<QnaMessage, Long> {

    List<QnaMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
