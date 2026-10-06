package pl.tomaszko.cheapskountant.receipt.transcription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.ResourceAccessException;

import pl.tomaszko.cheapskountant.config.TranscriptionProperties;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureException;
import pl.tomaszko.cheapskountant.receipt.ReceiptFailureReason;
import pl.tomaszko.cheapskountant.receipt.ReceiptFixtures;
import pl.tomaszko.cheapskountant.receipt.SubmittedImage;
import pl.tomaszko.cheapskountant.receipt.application.ReceiptCompletenessChecker;

class ReceiptTranscriptionServiceTest {

    private final AtomicReference<Prompt> prompt = new AtomicReference<>();

    @Test
    void transcribesOneReceiptFromOrderedImages() {
        ReceiptTranscriptionService service = service(prompt -> {
            this.prompt.set(prompt);
            return json(ReceiptFixtures.JSON);
        }, Duration.ofSeconds(5));

        ReceiptDraft draft = service.transcribe(List.of(
                new SubmittedImage("page-1.jpg", null, new byte[] {1}),
                new SubmittedImage("page-2.png", "image/png", new byte[] {2, 3})));

        assertThat(draft.documentType()).isEqualTo("fiscal_receipt");
        assertThat(draft.seller().tradeName()).isEqualTo("Sklep");
        assertThat(draft.items()).hasSize(1);
        assertThat(draft.taxSummary()).hasSize(1);
        assertThat(draft.payments()).hasSize(1);

        Prompt sent = prompt.get();
        assertThat(sent.getSystemMessage().getText()).contains("fiscal_receipt");
        assertThat(sent.getUserMessage().getMedia()).hasSize(2);
        assertThat(sent.getUserMessage().getMedia().get(0).getMimeType().toString()).isEqualTo("image/jpeg");
        assertThat(sent.getUserMessage().getMedia().get(1).getMimeType().toString()).isEqualTo("image/png");
        assertThat(sent.getOptions()).isInstanceOf(OpenAiChatOptions.class);
        assertThat(((OpenAiChatOptions) sent.getOptions()).getResponseFormat().getJsonSchema()).contains("fiscal_receipt");
    }

    @Test
    void blankResponseIsUnreadable() {
        ReceiptTranscriptionService service = service(prompt -> json("   "), Duration.ofSeconds(5));

        assertThatThrownBy(() -> service.transcribe(List.of(image())))
                .isInstanceOf(ReceiptFailureException.class)
                .extracting(ex -> ((ReceiptFailureException) ex).reason())
                .isEqualTo(ReceiptFailureReason.UNREADABLE);
    }

    @Test
    void nonObjectResponseIsUnreadable() {
        ReceiptTranscriptionService service = service(prompt -> json("[]"), Duration.ofSeconds(5));

        assertThatThrownBy(() -> service.transcribe(List.of(image())))
                .isInstanceOf(ReceiptFailureException.class)
                .extracting(ex -> ((ReceiptFailureException) ex).reason())
                .isEqualTo(ReceiptFailureReason.UNREADABLE);
    }

    @Test
    void missingTaxSummaryIsIncomplete() {
        String json = ReceiptFixtures.JSON.replaceAll("(?s)\"taxSummary\": \\[.*?\\]", "\"taxSummary\": []");
        ReceiptTranscriptionService service = service(prompt -> json(json), Duration.ofSeconds(5));

        assertThatThrownBy(() -> service.transcribe(List.of(image())))
                .isInstanceOf(ReceiptFailureException.class)
                .extracting(ex -> ((ReceiptFailureException) ex).reason())
                .isEqualTo(ReceiptFailureReason.INCOMPLETE);
    }

    @Test
    void providerFailureIsUnavailable() {
        ReceiptTranscriptionService service = service(prompt -> {
            throw new ResourceAccessException("openrouter down");
        }, Duration.ofSeconds(5));

        assertThatThrownBy(() -> service.transcribe(List.of(image())))
                .isInstanceOf(ReceiptFailureException.class)
                .satisfies(ex -> {
                    ReceiptFailureException failure = (ReceiptFailureException) ex;
                    assertThat(failure.reason()).isEqualTo(ReceiptFailureReason.TRANSCRIPTION_UNAVAILABLE);
                    assertThat(failure.timeout()).isFalse();
                });
    }

    @Test
    void timeoutIsUnavailableWithTimeoutFlag() {
        ReceiptTranscriptionService service = service(prompt -> {
            try {
                Thread.sleep(Duration.ofSeconds(5));
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return json(ReceiptFixtures.JSON);
        }, Duration.ofMillis(200));

        assertThatThrownBy(() -> service.transcribe(List.of(image())))
                .isInstanceOf(ReceiptFailureException.class)
                .satisfies(ex -> {
                    ReceiptFailureException failure = (ReceiptFailureException) ex;
                    assertThat(failure.reason()).isEqualTo(ReceiptFailureReason.TRANSCRIPTION_UNAVAILABLE);
                    assertThat(failure.timeout()).isTrue();
                });
    }

    private ReceiptTranscriptionService service(java.util.function.Function<Prompt, ChatResponse> behavior, Duration timeout) {
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                ReceiptTranscriptionServiceTest.this.prompt.set(prompt);
                return behavior.apply(prompt);
            }

            @Override
            public ChatOptions getOptions() {
                return OpenAiChatOptions.builder().model("google/gemini-2.5-flash").build();
            }
        };
        return new ReceiptTranscriptionService(
                ChatClient.builder(model)
                        .defaultOptions(OpenAiChatOptions.builder().model("google/gemini-2.5-flash"))
                        .build(),
                new TranscriptionProperties(
                        "google/gemini-2.5-flash",
                        timeout,
                        new ClassPathResource("prompts/receipt-transcription-system.st")),
                new ReceiptCompletenessChecker());
    }

    private ChatResponse json(String body) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(body))));
    }

    private SubmittedImage image() {
        return new SubmittedImage("page.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }
}
