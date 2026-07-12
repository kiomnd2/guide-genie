package io.hz.guidegenie.qna.adapter.out.persistence;

import io.hz.guidegenie.qna.application.port.out.QnaMessageRepositoryPort;
import io.hz.guidegenie.qna.domain.QnaMessage;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QnaMessagePersistenceAdapter implements QnaMessageRepositoryPort {

    private final QnaMessageJpaRepository jpa;

    @Override
    public QnaMessage save(QnaMessage message) {
        return jpa.save(message);
    }

    @Override
    public Optional<QnaMessage> findById(Long id) {
        return jpa.findById(id);
    }
}
