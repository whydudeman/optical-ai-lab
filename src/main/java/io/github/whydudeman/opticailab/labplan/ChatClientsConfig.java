package io.github.whydudeman.opticailab.labplan;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class ChatClientsConfig {

    @Bean
    public Map<LlmProvider, ChatClient> chatClients(AnthropicChatModel anthropicChatModel,
                                                    OpenAiChatModel openAiChatModel) {
        return Map.of(
                LlmProvider.ANTHROPIC, ChatClient.create(anthropicChatModel),
                LlmProvider.OPENAI, ChatClient.create(openAiChatModel)
        );
    }
}
