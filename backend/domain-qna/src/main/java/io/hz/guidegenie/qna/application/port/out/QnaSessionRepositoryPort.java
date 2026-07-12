package io.hz.guidegenie.qna.application.port.out;

import io.hz.guidegenie.qna.domain.QnaSession;
import java.util.Optional;

public interface QnaSessionRepositoryPort {
    QnaSession save(QnaSession session);
    Optional<QnaSession> findById(Long id);
}
