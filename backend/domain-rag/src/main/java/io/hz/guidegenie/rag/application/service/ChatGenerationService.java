package io.hz.guidegenie.rag.application.service;

import io.hz.guidegenie.rag.application.port.in.GenerationPort;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * LLM 생성 어댑터 — Spring AI {@link ChatModel}(Gemini)로 GenerationPort 구현.
 * 'ai' 프로파일이 아니면 ChatModel 빈이 없어 {@link #enabled()}가 false가 되고, 호출자는 stub로 폴백한다.
 */
@Service
public class ChatGenerationService implements GenerationPort {

    private final ChatModel chatModel; // AI 미설정 시 null

    public ChatGenerationService(ObjectProvider<ChatModel> chatModelProvider) {
        this.chatModel = chatModelProvider.getIfAvailable();
    }

    @Override
    public boolean enabled() {
        return chatModel != null;
    }

    @Override
    public String generate(String prompt) {
        if (chatModel == null) {
            throw new IllegalStateException("ChatModel 미구성 — 'ai' 프로파일로 실행하세요(docs/AI-SETUP.md)");
        }
        return chatModel.call(prompt);
    }
}
