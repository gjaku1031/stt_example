package com.gco.stt.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v2.SpeechClient;
import com.google.cloud.speech.v2.SpeechSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class GoogleCloudConfig {

    @Value("${google.cloud.credentials.json}")
    private String credentialsJson;

    @Bean
    public SpeechClient speechClient() {
        try {
            SpeechSettings settings = SpeechSettings.newBuilder()
                    .setCredentialsProvider(this::googleCredentials)
                    .build();
            return SpeechClient.create(settings);
        } catch (Exception e) {
            throw new RuntimeException("Speech Client 초기화 실패: " + e.getMessage(), e);
        }
    }

    @Bean
    public GoogleCredentials googleCredentials() {
        ByteArrayInputStream credentialsStream = new ByteArrayInputStream(
                credentialsJson.getBytes(StandardCharsets.UTF_8)
        );
        try {
            return GoogleCredentials.fromStream(credentialsStream);
        } catch (IOException e) {
            throw new RuntimeException("Google Credential 초기화 실패: " + e.getMessage(), e);
        }
    }
}
