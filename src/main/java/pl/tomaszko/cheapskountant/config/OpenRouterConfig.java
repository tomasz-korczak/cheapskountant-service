package pl.tomaszko.cheapskountant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenRouterConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, ModelCallLoggingAdvisor advisor, TranscriptionProperties properties) {
        return builder
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.model())
                        .timeout(properties.timeout())
                        .maxRetries(0))
                .defaultAdvisors(advisor)
                .build();
    }
}
