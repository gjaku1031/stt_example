package com.gco.stt.config;

import com.google.cloud.speech.v2.SpeechClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "google.cloud.credentials.json=${GOOGLE_CREDENTIALS_JSON}",
        "gcp.location=global"
})
class GoogleCloudConfigTest {

    @Autowired(required = false)
    private SpeechClient speechClient;

    @Test
    void contextLoads() {
        // 환경변수가 설정되어 있으면 SpeechClient가 생성되어야 함
        // CI/CD 환경에서는 환경변수가 없을 수 있으므로 required = false
        if (System.getenv("GOOGLE_CREDENTIALS_JSON") != null) {
            assertThat(speechClient).isNotNull();
        }
    }
}