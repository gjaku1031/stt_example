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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Google Speech-to-Text v2 API를 사용한 음성 인식 컨트롤러 (Recognizer 방식)
 * 
 * 이 컨트롤러는 Recognizer 기반 방식을 사용합니다:
 * - 영구적인 recognizer 리소스 생성/재사용
 * - Speech-to-Text Editor 이상의 권한 필요
 * - 한 번 생성하면 계속 재사용 가능 (효율적)
 * - 실제 프로덕션 환경에 적합
 */
@RestController
@RequestMapping("/api/recognizer")
@RequiredArgsConstructor
@Slf4j
public class RecognizerController {

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
     * 음성 파일을 업로드받아 텍스트로 변환하는 엔드포인트 (Recognizer 방식)
     * 
     * 처리 플로우:
     * 1. 클라이언트로부터 음성 파일 수신
     * 2. Recognizer 존재 확인 (없으면 생성)
     * 3. Recognition 설정 구성
     * 4. Recognizer를 사용한 recognition 요청
     * 5. 응답을 텍스트로 변환하여 반환
     * 
     * @param audioFile 업로드된 음성 파일
     * @return SpeechResponse 변환 결과
     */
    @PostMapping("/upload")
    public ResponseEntity<SpeechResponse> uploadAudioFile(@RequestParam("audio") MultipartFile audioFile) throws IOException {
        try {
            // Step 1: 음성 파일 검증 및 로깅
            log.info("음성 파일 수신 (Recognizer 방식): {}, 크기: {} bytes", audioFile.getOriginalFilename(), audioFile.getSize());
            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest().body(new SpeechResponse(false, "업로드된 파일이 없음", null));
            }

            // Step 2: 음성 파일을 ByteString으로 변환
            byte[] audioBytes = audioFile.getBytes();
            ByteString audioData = ByteString.copyFrom(audioBytes);

            // Step 3: Recognizer 설정 (영구적인 리소스 사용)
            // 이 ID는 한 번 생성되면 계속 재사용됨
            String recognizerId = "permanent-recognizer";
            RecognizerName recognizerName = RecognizerName.of(projectId, location, recognizerId);
            
            // Step 4: Recognizer 존재 확인 및 생성
            try {
                // 기존 recognizer 확인
                speechClient.getRecognizer(recognizerName);
                log.info("기존 recognizer 사용: {}", recognizerName);
            } catch (Exception e) {
                // recognizer가 없으면 새로 생성 (첫 실행 시만)
                log.info("새 recognizer 생성 중: {}", recognizerName);
                
                // Recognizer 설정 정의
                Recognizer recognizer = Recognizer.newBuilder()
                        .setDisplayName("Permanent Recognizer for Korean STT")
                        .setDefaultRecognitionConfig(  // 기본 설정 저장
                            RecognitionConfig.newBuilder()
                                .addLanguageCodes("ko-KR")      // 한국어
                                .setModel("long")               // 긴 음성 모델
                                .setAutoDecodingConfig(         // 자동 인코딩
                                    AutoDetectDecodingConfig.newBuilder().build()
                                )
                                .build()
                        )
                        .build();
                
                // Recognizer 생성 요청
                CreateRecognizerRequest createRequest = CreateRecognizerRequest.newBuilder()
                        .setParent(LocationName.of(projectId, location).toString())  // 부모 위치
                        .setRecognizerId(recognizerId)                             // Recognizer ID
                        .setRecognizer(recognizer)                                  // Recognizer 설정
                        .build();
                
                try {
                    // 비동기 생성 (최대 5분 대기)
                    com.google.api.gax.longrunning.OperationFuture<Recognizer, OperationMetadata> future = 
                            speechClient.createRecognizerAsync(createRequest);
                    future.get(5, TimeUnit.MINUTES);  // 생성 완료 대기
                    log.info("Recognizer 생성 완료");
                } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                    log.error("Recognizer 생성 실패: {}", ex.getMessage());
                    throw new RuntimeException("Recognizer 생성 실패", ex);
                }
            }

            // Step 5: Recognition 설정 및 요청 생성
            // 이 설정은 기본 recognizer 설정을 오버라이드할 수 있음
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .addLanguageCodes("ko-KR")
                    .setModel("long")
                    .setAutoDecodingConfig(AutoDetectDecodingConfig.newBuilder().build())
                    .build();

            // Recognition 요청 (영구 recognizer 사용)
            RecognizeRequest request = RecognizeRequest.newBuilder()
                    .setRecognizer(recognizerName.toString())  // 영구 recognizer 경로
                    .setConfig(config)                        // 위에서 정의한 설정
                    .setContent(audioData)                    // 음성 데이터
                    .build();

            // Step 6: Google Speech API 호출
            log.info("Google Speech-to-Text v2 API 호출 중 (Recognizer 방식)...");
            RecognizeResponse response = speechClient.recognize(request);

            // Step 7: 응답 처리
            if (response.getResultsList().isEmpty()) {
                log.warn("음성 인식 결과가 없음");
                return ResponseEntity.ok(new SpeechResponse(false, "음성 인식 불가", null));
            }
            
            // 가장 신뢰도 높은 결과 추출
            String transcript = response.getResults(0).getAlternatives(0).getTranscript();

            log.info("최종 변환 결과: {}", transcript);
            return ResponseEntity.ok(new SpeechResponse(true, "음성 변환 성공!", transcript));
        } catch (Exception e) {
            log.error("음성 인식 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new SpeechResponse(false, "서버 오류 발생", null));
        }
    }
}