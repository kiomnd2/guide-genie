package io.hz.guidegenie.qna.adapter.out.persistence;

import io.hz.guidegenie.qna.application.port.out.QnaSessionRepositoryPort;
import io.hz.guidegenie.qna.domain.QnaSession;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QnaSessionPersistenceAdapter implements QnaSessionRepositoryPort {

    private final QnaSessionJpaRepository jpa;

    @Override
    public QnaSession save(QnaSession session) {
        return jpa.save(session);
    }

    @Override
    public Optional<QnaSession> findById(Long id) {
        return jpa.findById(id);
    }
}
