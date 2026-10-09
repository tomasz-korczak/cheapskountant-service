package pl.tomaszko.cheapskountant.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ModelCallLoggingAdvisor implements CallAdvisor {

    private final List<String> secrets;

    public ModelCallLoggingAdvisor(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.datasource.password:}") String databasePassword) {
        this.secrets = secrets(apiKey, databasePassword);
    }

    @Override
    public String getName() {
        return "modelCallLogging";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        logRequest(request.prompt());
        ChatClientResponse response = chain.nextCall(request);
        logResponse(response.chatResponse());
        return response;
    }

    private void logRequest(Prompt prompt) {
        StringBuilder message = new StringBuilder();
        message.append("model request");
        for (SystemMessage systemMessage : prompt.getSystemMessages()) {
            message.append("\nsystem prompt: ").append(systemMessage.getText());
        }
        message.append("\ntool definitions: ").append(toolDefinitions(prompt));
        UserMessage userMessage = prompt.getUserMessage();
        message.append("\nuser text: ").append(userMessage.getText());
        List<Media> media = userMessage.getMedia();
        message.append("\nimage count: ").append(media.size());
        for (Media item : media) {
            message.append("\nimage media type: ")
                    .append(item.getMimeType())
                    .append(", size: ")
                    .append(byteSize(item));
        }
        log.info(redact(message.toString()));
    }

    private void logResponse(ChatResponse chatResponse) {
        StringBuilder message = new StringBuilder("model response");
        if (chatResponse != null && chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
            AssistantMessage output = chatResponse.getResult().getOutput();
            message.append("\nresponse text: ").append(output.getText());
            if (output.getToolCalls() != null) {
                for (AssistantMessage.ToolCall toolCall : output.getToolCalls()) {
                    message.append("\ntool call: name=")
                            .append(toolCall.name())
                            .append(", parameters=")
                            .append(toolCall.arguments())
                            .append(", result=")
                            .append(output.getText());
                }
            }
        }
        log.info(redact(message.toString()));
    }

    private String toolDefinitions(Prompt prompt) {
        if (!(prompt.getOptions() instanceof ToolCallingChatOptions options) || options.getToolCallbacks() == null
                || options.getToolCallbacks().isEmpty()) {
            return "[]";
        }
        List<String> definitions = new ArrayList<>();
        for (ToolCallback callback : options.getToolCallbacks()) {
            var definition = callback.getToolDefinition();
            definitions.add("name=" + definition.name() + ", parameters=" + definition.inputSchema());
        }
        return definitions.toString();
    }

    private long byteSize(Media media) {
        Object data = media.getData();
        if (data instanceof byte[] bytes) {
            return bytes.length;
        }
        if (data instanceof Resource resource) {
            try {
                return resource.contentLength();
            }
            catch (IOException ex) {
                return -1;
            }
        }
        return media.getDataAsByteArray().length;
    }

    private String redact(String text) {
        String redacted = text;
        for (String secret : secrets) {
            redacted = redacted.replace(secret, "[redacted]");
        }
        return redacted;
    }

    private static List<String> secrets(String apiKey, String databasePassword) {
        List<String> values = new ArrayList<>();
        if (apiKey != null && !apiKey.isBlank()) {
            values.add(apiKey);
        }
        if (databasePassword != null && !databasePassword.isBlank()) {
            values.add(databasePassword);
        }
        return List.copyOf(values);
    }
}
