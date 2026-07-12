package io.hz.guidegenie.qna.application.service;

import io.hz.guidegenie.qna.domain.Citation;
import java.util.List;

/** Q&A 답변 결과(비스트리밍). SSE 스트리밍은 인바운드 어댑터(app-api 컨트롤러)가 담당. */
public record QnaAnswer(Long sessionId, Long messageId, String answer, List<Citation> citations) {
}
