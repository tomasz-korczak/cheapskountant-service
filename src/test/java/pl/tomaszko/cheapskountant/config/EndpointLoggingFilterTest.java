package pl.tomaszko.cheapskountant.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.mock.web.MockPart;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class EndpointLoggingFilterTest {

    @Test
    void logsQueryParametersBodyAndResponseWithoutSecrets() throws Exception {
        byte[] body = "{\"total\":\"10\",\"note\":\"sk-test-openrouter-key\"}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/receipt");
        request.setQueryString("from=2026-01-01&token=sk-test-openrouter-key");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(body);
        MockHttpServletResponse response = new MockHttpServletResponse();
        EndpointLoggingFilter filter = filter();

        String logged = capture(() -> filter.doFilter(request, response, (req, res) -> {
            HttpServletResponse http = (HttpServletResponse) res;
            req.getInputStream().readAllBytes();
            http.setStatus(201);
            http.setContentType(MediaType.APPLICATION_JSON_VALUE);
            http.setCharacterEncoding(StandardCharsets.UTF_8.name());
            http.getWriter().write("{\"id\":1,\"password\":\"db-password-secret\"}");
        }));

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo("{\"id\":1,\"password\":\"db-password-secret\"}");
        assertThat(logged).contains("endpoint POST /api/receipt");
        assertThat(logged).contains("parameters: from=2026-01-01&token=[redacted]");
        assertThat(logged).contains("request body: {\"total\":\"10\",\"note\":\"[redacted]\"}");
        assertThat(logged).contains("response status: 201");
        assertThat(logged).contains("response body: {\"id\":1,\"password\":\"[redacted]\"}");
        assertThat(logged).doesNotContain("sk-test-openrouter-key");
        assertThat(logged).doesNotContain("db-password-secret");
        assertThat(logged).doesNotContain("api-key-secret");
    }

    @Test
    void logsMultipartParametersWithoutFileBytes() throws Exception {
        byte[] image = "IMAGE_BYTES_DO_NOT_LOG".getBytes(StandardCharsets.UTF_8);
        MockPart file = new MockPart("images", "receipt.jpg", image);
        file.getHeaders().setContentType(MediaType.IMAGE_JPEG);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/transcription");
        request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
        request.addPart(file);
        request.addPart(new MockPart("note", "hello".getBytes(StandardCharsets.UTF_8)));
        MockHttpServletResponse response = new MockHttpServletResponse();

        String logged = capture(() -> filter().doFilter(request, response, (req, res) -> {
            HttpServletResponse http = (HttpServletResponse) res;
            http.setStatus(200);
            http.setContentType(MediaType.APPLICATION_JSON_VALUE);
            http.setCharacterEncoding(StandardCharsets.UTF_8.name());
            http.getWriter().write("{\"documentType\":\"fiscal_receipt\"}");
        }));

        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("{\"documentType\":\"fiscal_receipt\"}");
        assertThat(logged).contains("endpoint POST /api/transcription");
        assertThat(logged).contains("request part: name=images, filename=receipt.jpg, content type=image/jpeg, size=" + image.length);
        assertThat(logged).contains("request parameter: note=hello");
        assertThat(logged).contains("response status: 200");
        assertThat(logged).contains("response body: {\"documentType\":\"fiscal_receipt\"}");
        assertThat(logged).doesNotContain("IMAGE_BYTES_DO_NOT_LOG");
    }

    @Test
    void logsMultipartFilesAndRequestParameters() throws Exception {
        byte[] image = "IMAGE_BYTES_DO_NOT_LOG".getBytes(StandardCharsets.UTF_8);
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/transcription");
        request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
        request.addFile(new MockMultipartFile("images", "receipt.jpg", MediaType.IMAGE_JPEG_VALUE, image));
        request.addParameter("from", "2026-10-10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String logged = capture(() -> filter().doFilter(request, response, (req, res) -> {
            HttpServletResponse http = (HttpServletResponse) res;
            http.setStatus(200);
            http.setContentType(MediaType.APPLICATION_JSON_VALUE);
            http.getWriter().write("{\"ok\":true}");
        }));

        assertThat(logged).contains("parameters: from=2026-10-10");
        assertThat(logged).contains("request part: name=images, filename=receipt.jpg, content type=image/jpeg, size=" + image.length);
        assertThat(logged).contains("response body: {\"ok\":true}");
        assertThat(logged).doesNotContain("IMAGE_BYTES_DO_NOT_LOG");
    }

    @Test
    void logsFailureAndStillReturnsTheResponseBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/expense");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("{\"amount\":\"1\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        String logged = capture(() -> assertThatThrownBy(() -> filter().doFilter(request, response, (req, res) -> {
            HttpServletResponse http = (HttpServletResponse) res;
            req.getInputStream().readAllBytes();
            http.setStatus(500);
            http.setContentType(MediaType.APPLICATION_JSON_VALUE);
            http.setCharacterEncoding(StandardCharsets.UTF_8.name());
            http.getWriter().write("{\"reason\":\"storage failed\"}");
            throw new ServletException("database down");
        })).isInstanceOf(ServletException.class).hasMessage("database down"));

        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("{\"reason\":\"storage failed\"}");
        assertThat(logged).contains("request body: {\"amount\":\"1\"}");
        assertThat(logged).contains("response status: 500");
        assertThat(logged).contains("response body: {\"reason\":\"storage failed\"}");
        assertThat(logged).contains("failure: ServletException: database down");
    }

    private EndpointLoggingFilter filter() {
        return new EndpointLoggingFilter("api-key-secret", "sk-test-openrouter-key", "db-password-secret");
    }

    private String capture(Checked action) throws Exception {
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(EndpointLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            action.run();
        }
        finally {
            logger.detachAppender(appender);
        }
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", (left, right) -> left + "\n" + right);
    }

    @FunctionalInterface
    private interface Checked {
        void run() throws Exception;
    }
}
