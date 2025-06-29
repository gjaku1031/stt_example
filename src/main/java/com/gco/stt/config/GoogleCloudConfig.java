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

/**
 * Google Cloud Speech-to-Text v2 API 클라이언트 설정
 * 
 * 주요 기능:
 * - Base64로 인코딩된 서비스 계정 키 디코딩
 * - Google Cloud 자격 증명 생성
 * - Speech v2 API endpoint 설정 (location별)
 * - SpeechClient Bean 생성 및 Spring IoC 컨테이너에 등록
 */
@Slf4j
@Configuration
public class GoogleCloudConfig {

    @Value("${google.cloud.credentials.json}")
    private String credentialsJson;  // Base64 인코딩된 서비스 계정 JSON

    @Value("${gcp.location}")
    private String location;  // Speech API 사용 지역 (global, us-central1 등)

    /**
     * Google Speech-to-Text v2 클라이언트 Bean 생성
     * 
     * 처리 플로우:
     * 1. Base64로 인코딩된 자격 증명을 디코딩
     * 2. GoogleCredentials 객체 생성
     * 3. location에 따른 endpoint 결정
     * 4. SpeechSettings 구성
     * 5. SpeechClient 생성 및 반환
     * 
     * @return 설정된 SpeechClient 인스턴스
     * @throws IOException 자격 증명 처리 중 오류 발생 시
     */
    @Bean
    public SpeechClient speechClient() throws IOException {
        log.info("v2 SpeechClient Bean 생성. 리전 {}", location);
        
        // Step 1: Base64 인코딩된 자격 증명 디코딩
        byte[] decodedCredentials = Base64.getDecoder().decode(credentialsJson);
        
        // Step 2: Google Cloud 자격 증명 생성
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(decodedCredentials)
        );
        CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);

        // Step 3: Speech v2 API endpoint 결정
        // v2 API는 location에 따라 다른 endpoint를 사용해야 함
        String endpoint;
        if ("global".equals(location)) {
            // global location은 prefix 없이 사용
            endpoint = "speech.googleapis.com:443";
        } else {
            // 특정 리전은 리전명을 prefix로 사용
            // 예: us-central1-speech.googleapis.com:443
            endpoint = String.format("%s-speech.googleapis.com:443", location);
        }

        // Step 4: SpeechClient 설정 구성
        SpeechSettings settings = SpeechSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)  // 자격 증명 설정
                .setEndpoint(endpoint)                       // API endpoint 설정
                .build();

        // Step 5: SpeechClient 생성 및 반환
        return SpeechClient.create(settings);
    }
}