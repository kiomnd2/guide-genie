package io.hz.guidegenie.qna.application.port.out;

import io.hz.guidegenie.qna.domain.QnaMessage;
import java.util.Optional;

public interface QnaMessageRepositoryPort {
    QnaMessage save(QnaMessage message);
    Optional<QnaMessage> findById(Long id);
}
