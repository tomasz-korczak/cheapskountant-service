package pl.tomaszko.cheapskountant.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class ModelCallLoggingAdvisorTest {

    @Test
    void logsPromptAndResponseWithoutSecretsOrImageBytes() {
        byte[] image = "IMAGE_BYTES_DO_NOT_LOG".getBytes(StandardCharsets.UTF_8);
        UserMessage user = UserMessage.builder()
                .text("Transcribe the attached pages, in order, as one Polish fiscal receipt.")
                .media(new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(image)))
                .build();
        SystemMessage system = new SystemMessage("You transcribe photos of one Polish fiscal receipt.");
        Prompt prompt = new Prompt(List.of(system, user));
        ChatClientRequest request = new ChatClientRequest(prompt, Map.of(
                "apiKey", "sk-test-openrouter-key",
                "password", "db-password-secret"));
        AssistantMessage assistant = AssistantMessage.builder()
                .content("{\"documentType\":\"fiscal_receipt\"}")
                .toolCalls(List.of(new AssistantMessage.ToolCall("1", "function", "lookup", "{\"sku\":\"1\"}")))
                .build();
        ChatClientResponse response = new ChatClientResponse(
                new ChatResponse(List.of(new Generation(assistant))),
                Map.of());
        ModelCallLoggingAdvisor advisor = new ModelCallLoggingAdvisor("sk-test-openrouter-key", "db-password-secret");
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(ModelCallLoggingAdvisor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            advisor.adviseCall(request, new CallAdvisorChain() {
                @Override
                public ChatClientResponse nextCall(ChatClientRequest chatClientRequest) {
                    return response;
                }

                @Override
                public List<CallAdvisor> getCallAdvisors() {
                    return List.of();
                }

                @Override
                public CallAdvisorChain copy(CallAdvisor advisor) {
                    return this;
                }
            });
        }
        finally {
            logger.detachAppender(appender);
        }

        String logged = appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", (left, right) -> left + "\n" + right);
        assertThat(logged).contains("You transcribe photos of one Polish fiscal receipt.");
        assertThat(logged).contains("tool definitions: []");
        assertThat(logged).contains("Transcribe the attached pages, in order, as one Polish fiscal receipt.");
        assertThat(logged).contains("image count: 1");
        assertThat(logged).contains("image/jpeg");
        assertThat(logged).contains("size: " + image.length);
        assertThat(logged).contains("{\"documentType\":\"fiscal_receipt\"}");
        assertThat(logged).contains("tool call: name=lookup, parameters={\"sku\":\"1\"}");
        assertThat(logged).doesNotContain("sk-test-openrouter-key");
        assertThat(logged).doesNotContain("db-password-secret");
        assertThat(logged).doesNotContain("IMAGE_BYTES_DO_NOT_LOG");
    }
}
