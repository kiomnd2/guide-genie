package io.hz.guidegenie.qna.adapter.out.persistence;

import io.hz.guidegenie.qna.domain.QnaMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnaMessageJpaRepository extends JpaRepository<QnaMessage, Long> {
}
