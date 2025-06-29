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

/**
 * Google Speech-to-Text v2 API를 사용한 음성 인식 컨트롤러
 * 
 * 이 컨트롤러는 Inline Recognition 방식을 사용합니다:
 * - recognizer 생성 권한이 없어도 사용 가능
 * - 매 요청마다 임시 recognizer 사용 (recognizers/_)
 * - 추가 IAM 권한 불필요
 */
@RestController
@RequestMapping("/api/speech")
@RequiredArgsConstructor
@Slf4j
public class SpeechRecorderController {

    private final SpeechClient speechClient;  // Google Cloud Config에서 주입받은 Speech v2 클라이언트

    @Value("${gcp.project-id}")
    private String projectId;  // Google Cloud 프로젝트 ID

    @Value("${gcp.location}")
    private String location;   // 리전 설정 (예: global)

    /**
     * API 응답 형식을 정의하는 Record
     * @param success 성공 여부
     * @param message 처리 메시지
     * @param transcript 음성 인식 결과 텍스트
     */
    public record SpeechResponse(boolean success, String message, String transcript) {
    }

    /**
     * 음성 파일을 업로드받아 텍스트로 변환하는 엔드포인트
     * 
     * 처리 플로우:
     * 1. 클라이언트로부터 음성 파일 수신 (webm 형식)
     * 2. 파일을 ByteString으로 변환
     * 3. Recognition 설정 구성 (한국어, long 모델)
     * 4. Inline recognition 요청 (recognizers/_ 사용)
     * 5. 응답을 텍스트로 변환하여 반환
     * 
     * @param audioFile 업로드된 음성 파일
     * @return SpeechResponse 변환 결과
     */
    @PostMapping("/upload")
    public ResponseEntity<SpeechResponse> uploadAudioFile(@RequestParam("audio") MultipartFile audioFile) throws IOException {
        try {
            // Step 1: 음성 파일 검증 및 로깅
            log.info("음성 파일 수신: {}, 크기: {} bytes", audioFile.getOriginalFilename(), audioFile.getSize());
            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest().body(new SpeechResponse(false, "업로드된 파일이 없음", null));
            }

            // Step 2: 음성 파일을 Google Speech API가 처리할 수 있는 형식으로 변환
            byte[] audioBytes = audioFile.getBytes();
            ByteString audioData = ByteString.copyFrom(audioBytes);

            // Step 3: Speech v2 API 설정 구성
            // inline recognition 사용 (recognizer 생성 불필요)
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .addLanguageCodes("ko-KR")        // 한국어 인식
                    .setModel("long")                 // 긴 음성 파일에 최적화된 모델
                    .setAutoDecodingConfig(           // 자동 인코딩 감지
                        AutoDetectDecodingConfig.newBuilder().build()
                    )
                    .build();

            // Step 4: Inline recognition 요청 생성
            // "recognizers/_" 는 임시 recognizer를 자동 생성하여 사용
            // 권한이 없어도 사용 가능한 특수 경로
            RecognizeRequest request = RecognizeRequest.newBuilder()
                    .setConfig(config)                // 위에서 정의한 설정 사용
                    .setContent(audioData)            // 음성 데이터
                    .setRecognizer(String.format(     // 특수 recognizer 경로
                        "projects/%s/locations/%s/recognizers/_", 
                        projectId, location
                    ))
                    .build();

            // Step 5: Google Speech API 호출
            log.info("Google Speech-to-Text v2 API 호출 중...");
            RecognizeResponse response = speechClient.recognize(request);

            // Step 6: 응답 처리
            if (response.getResultsList().isEmpty()) {
                log.warn("음성 인식 결과가 없음");
                return ResponseEntity.ok(new SpeechResponse(false, "음성 인식 불가", null));
            }
            
            // 첫 번째 결과의 가장 신뢰도 높은 대안 선택
            String transcript = response.getResults(0)  // 첫 번째 결과
                                      .getAlternatives(0)   // 가장 신뢰도 높은 대안
                                      .getTranscript();     // 텍스트 추출

            log.info("최종 변환 결과: {}", transcript);
            return ResponseEntity.ok(new SpeechResponse(true, "음성 변환 성공!", transcript));
        } catch (Exception e) {
            log.error("음성 인식 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new SpeechResponse(false, "서버 오류 발생", null));
        }
    }
}













