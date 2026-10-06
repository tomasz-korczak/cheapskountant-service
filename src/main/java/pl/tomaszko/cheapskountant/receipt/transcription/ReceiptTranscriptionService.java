package pl.tomaszko.cheapskountant.receipt.transcription;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.client.RestClientException;

import pl.tomaszko.cheapskountant.config.TranscriptionProperties;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.SubmittedImage;
import pl.tomaszko.cheapskountant.receipt.application.ReceiptCompletenessChecker;

@Service
public class ReceiptTranscriptionService {

    private final ChatClient chatClient;
    private final TranscriptionProperties properties;
    private final ReceiptCompletenessChecker completenessChecker;
    private final String systemPrompt;
    private final String receiptSchema;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public ReceiptTranscriptionService(
            ChatClient chatClient,
            TranscriptionProperties properties,
            ReceiptCompletenessChecker completenessChecker) {
        this.chatClient = chatClient;
        this.properties = properties;
        this.completenessChecker = completenessChecker;
        try {
            this.systemPrompt = properties.systemPrompt().getContentAsString(StandardCharsets.UTF_8);
            this.receiptSchema = new ClassPathResource("schemas/receipt-schema.json").getContentAsString(StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public ReceiptDraft transcribe(List<SubmittedImage> images) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            Future<ReceiptDraft> future = executor.submit(() -> transcribeOnce(images));
            try {
                return future.get(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException ex) {
                future.cancel(true);
                throw ReceiptFailureException.transcriptionTimeout(ex);
            }
            catch (ExecutionException ex) {
                throw mapFailure(ex.getCause());
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw ReceiptFailureException.transcriptionUnavailable(ex);
            }
        }
        finally {
            executor.shutdownNow();
        }
    }

    private ReceiptDraft transcribeOnce(List<SubmittedImage> images) {
        try {
            String content = chatClient.prompt()
                    .options(OpenAiChatOptions.builder()
                            .model(properties.model())
                            .timeout(properties.timeout())
                            .maxRetries(0)
                            .responseFormat(OpenAiChatModel.ResponseFormat.builder()
                                    .type(OpenAiChatModel.ResponseFormat.Type.JSON_SCHEMA)
                                    .jsonSchema(receiptSchema)
                                    .build()))
                    .system(systemPrompt)
                    .user(user -> {
                        user.text("Transcribe the attached pages, in order, as one Polish fiscal receipt.");
                        for (SubmittedImage image : images) {
                            user.media(MimeType.valueOf(mediaType(image)), new ByteArrayResource(image.bytes()));
                        }
                    })
                    .call()
                    .content();
            ReceiptDraft draft = readDraft(content);
            completenessChecker.check(draft);
            return draft;
        }
        catch (ReceiptFailureException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            throw mapFailure(ex);
        }
    }

    private ReceiptDraft readDraft(String content) {
        if (content == null || content.isBlank()) {
            throw ReceiptFailureException.unreadable("The images could not be read as one fiscal receipt.");
        }
        try {
            JsonNode node = jsonMapper.readTree(content);
            if (node == null || !node.isObject()) {
                throw ReceiptFailureException.unreadable("The images could not be read as one fiscal receipt.");
            }
            return jsonMapper.treeToValue(node, ReceiptDraft.class);
        }
        catch (JacksonException ex) {
            throw ReceiptFailureException.unreadable("The images could not be read as one fiscal receipt.", ex);
        }
    }

    private String mediaType(SubmittedImage image) {
        if (image.mediaType() == null || image.mediaType().isBlank() || "image/jpg".equalsIgnoreCase(image.mediaType())) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        return image.mediaType();
    }

    private RuntimeException mapFailure(Throwable error) {
        if (error instanceof ReceiptFailureException failure) {
            return failure;
        }
        if (isTimeout(error)) {
            return ReceiptFailureException.transcriptionTimeout(error);
        }
        if (isProviderFailure(error)) {
            return ReceiptFailureException.transcriptionUnavailable(error);
        }
        return ReceiptFailureException.unreadable("The images could not be read as one fiscal receipt.", error);
    }

    private boolean isTimeout(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String name = current.getClass().getName();
            String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
            if (current instanceof TimeoutException
                    || name.contains("Timeout")
                    || message.contains("timed out")
                    || message.contains("timeout")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isProviderFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String name = current.getClass().getName();
            if (current instanceof RestClientException
                    || current instanceof IOException
                    || name.contains("openai")
                    || name.contains("OpenAI")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
