package io.hz.guidegenie.qna.adapter.out.persistence;

import io.hz.guidegenie.qna.domain.QnaSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnaSessionJpaRepository extends JpaRepository<QnaSession, Long> {
}
