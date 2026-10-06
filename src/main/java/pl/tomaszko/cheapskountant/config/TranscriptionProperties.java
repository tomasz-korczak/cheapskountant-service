package pl.tomaszko.cheapskountant.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "app.transcription")
public record TranscriptionProperties(String model, Duration timeout, Resource systemPrompt) {
}
