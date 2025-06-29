package com.gco.stt.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
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
import java.util.Base64;

@Slf4j
@Configuration
public class GoogleCloudConfig {

    @Value("${google.cloud.credentials.json}")
    private String credentialsJson;

    @Value("${gcp.location}")
    private String location;

    @Bean
    public SpeechClient speechClient() throws IOException {
        log.info("v2 SpeechClient Bean 생성. 리전 {}", location);
        byte[] decodedCredentials = Base64.getDecoder().decode(credentialsJson);
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(decodedCredentials)
        );
        CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);

        // Speech v2 API는 location에 따라 다른 endpoint 사용
        String endpoint;
        if ("global".equals(location)) {
            endpoint = "speech.googleapis.com:443";
        } else {
            endpoint = String.format("%s-speech.googleapis.com:443", location);
        }

        SpeechSettings settings = SpeechSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .setEndpoint(endpoint)
                .build();

        return SpeechClient.create(settings);
    }
}