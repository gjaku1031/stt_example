package com.gco.stt.controller;

import com.google.cloud.speech.v2.*;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/speech")
@RequiredArgsConstructor
@Slf4j
public class SpeechRecorderController {

    private final SpeechClient speechClient;

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.location}")
    private String location;


    public record SpeechResponse(boolean success, String message, String transcript) {
    }

    @PostMapping("/upload")
    public ResponseEntity<SpeechResponse> uploadAudioFile(@RequestParam("audio") MultipartFile audioFile) throws IOException {
        try {
            log.info("음성 파일 수신: {}, 크기: {} bytes", audioFile.getOriginalFilename(), audioFile.getSize());
            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest().body(new SpeechResponse(false, "업로드된 파일이 없음", null));
            }

            byte[] audioBytes = audioFile.getBytes();
            ByteString audioData = ByteString.copyFrom(audioBytes);

            // Speech v2 API inline recognition 사용 (recognizer 생성 불필요)
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .addLanguageCodes("ko-KR")
                    .setModel("long")
                    .setAutoDecodingConfig(AutoDetectDecodingConfig.newBuilder().build())
                    .build();

            // Inline recognition request (recognizer 지정 대신 parent 사용)
            RecognizeRequest request = RecognizeRequest.newBuilder()
                    .setConfig(config)
                    .setContent(audioData)
                    .setRecognizer(String.format("projects/%s/locations/%s/recognizers/_", projectId, location))
                    .build();

            log.info("Google Speech-to-Text v2 API 호출 중...");

            RecognizeResponse response = speechClient.recognize(request);

            if (response.getResultsList().isEmpty()) {
                log.warn("음성 인식 결과가 없음");
                return ResponseEntity.ok(new SpeechResponse(false, "음성 인식 불가", null));
            }
            
            String transcript = response.getResults(0).getAlternatives(0).getTranscript();

            log.info("최종 변환 결과: {}", transcript);
            return ResponseEntity.ok(new SpeechResponse(true, "음성 변환 성공!", transcript));
        } catch (Exception e) {
            log.error("음성 인식 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new SpeechResponse(false, "서버 오류 발생", null));
        }
    }
}













