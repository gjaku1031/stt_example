package com.gco.stt;

// Google Cloud Speech API v2 관련 라이브러리 import
import com.google.api.gax.longrunning.OperationFuture; // 비동기 작업 처리를 위한 Future 객체
import com.google.cloud.speech.v2.*; // Speech API v2의 모든 클래스들
import com.google.protobuf.ByteString; // 바이너리 데이터를 Protocol Buffer 형식으로 처리

// Java 표준 라이브러리 import
import java.io.IOException; // 입출력 예외 처리
import java.nio.file.Files; // 파일 읽기 유틸리티
import java.nio.file.Path; // 파일 경로 객체
import java.nio.file.Paths; // 파일 경로 생성 유틸리티
import java.util.List; // 리스트 컬렉션
import java.util.concurrent.ExecutionException; // 비동기 작업 실행 중 발생하는 예외

/**
 * Google Cloud Speech-to-Text API v2를 사용한 음성 인식 샘플 클래스
 * 음성 파일을 읽어서 텍스트로 변환하는 기능을 제공
 */
public class QuickstartSampleV2 {

    /**
     * 프로그램의 진입점 (메인 메서드)
     * 실제 사용 시에는 아래 하드코딩된 값들을 실제 값으로 변경해야 함
     */
    public static void main(String[] args) throws IOException, ExecutionException,
            InterruptedException {
        // Google Cloud 프로젝트 ID (실제 프로젝트 ID로 변경 필요)
        String projectId = "my-project-id";
        
        // 변환할 음성 파일의 경로 (실제 파일 경로로 변경 필요)
        String filePath = "path/to/audioFile.raw";
        
        // 음성 인식기의 고유 식별자 (실제 recognizer ID로 변경 필요)
        String recognizerId = "my-recognizer-id";
        
        // 실제 음성 인식 작업을 수행하는 메서드 호출
        quickstartSampleV2(projectId, filePath, recognizerId);
    }

    /**
     * Google Cloud Speech-to-Text API v2를 사용하여 음성 파일을 텍스트로 변환하는 메서드
     * 
     * @param projectId Google Cloud 프로젝트 ID
     * @param filePath 변환할 음성 파일의 경로
     * @param recognizerId 사용할 음성 인식기의 ID
     * @throws IOException 파일 읽기 중 발생할 수 있는 예외
     * @throws ExecutionException 비동기 작업 실행 중 발생할 수 있는 예외
     * @throws InterruptedException 스레드 중단 시 발생할 수 있는 예외
     */
    public static void quickstartSampleV2(String projectId, String filePath, String recognizerId)
            throws IOException, ExecutionException, InterruptedException {

        // Google Cloud Speech 클라이언트 초기화
        // try-with-resources 구문을 사용하여 자동으로 리소스 정리 보장
        // 이 클라이언트는 한 번만 생성하고 여러 요청에 재사용 가능
        try (SpeechClient speechClient = SpeechClient.create()) {
            
            // === 1단계: 음성 파일을 바이트 배열로 읽기 ===
            Path path = Paths.get(filePath); // 파일 경로 객체 생성
            byte[] data = Files.readAllBytes(path); // 파일의 모든 바이트를 메모리로 읽기
            ByteString audioBytes = ByteString.copyFrom(data); // Google Protocol Buffer 형식으로 변환

            // Google Cloud 리소스의 부모 경로 생성 (전역 위치 사용)
            String parent = String.format("projects/%s/locations/global", projectId);

            // === 2단계: 음성 인식기(Recognizer) 생성 ===
            // 음성 인식기는 음성을 텍스트로 변환하는 데 사용되는 구성 요소
            Recognizer recognizer = Recognizer.newBuilder()
                    .setModel("latest_long") // 최신 장시간 음성 인식 모델 사용
                    .addLanguageCodes("en-US") // 인식할 언어를 영어(미국)로 설정
                    .build();

            // 음성 인식기 생성 요청 객체 구성
            CreateRecognizerRequest createRecognizerRequest = CreateRecognizerRequest.newBuilder()
                    .setParent(parent) // 부모 리소스 경로 설정
                    .setRecognizerId(recognizerId) // 인식기의 고유 ID 설정
                    .setRecognizer(recognizer) // 위에서 구성한 인식기 설정
                    .build();

            // 비동기적으로 음성 인식기 생성 (시간이 걸릴 수 있는 작업)
            OperationFuture<Recognizer, OperationMetadata> operationFuture =
                    speechClient.createRecognizerAsync(createRecognizerRequest);
            
            // 비동기 작업이 완료될 때까지 대기하고 결과 받기
            recognizer = operationFuture.get();

            // === 3단계: 음성 인식 요청 구성 ===
            // 음성 인식 설정 객체 생성
            RecognitionConfig recognitionConfig = RecognitionConfig.newBuilder()
                    // 자동 디코딩 설정: 파일 형식을 자동으로 감지하고 디코딩
                    .setAutoDecodingConfig(AutoDetectDecodingConfig.newBuilder().build())
                    .build();

            // 실제 음성 인식 요청 객체 구성
            RecognizeRequest request = RecognizeRequest.newBuilder()
                    .setConfig(recognitionConfig) // 위에서 설정한 인식 구성
                    .setRecognizer(recognizer.getName()) // 사용할 인식기의 이름
                    .setContent(audioBytes) // 변환할 음성 데이터
                    .build();

            // === 4단계: 음성 인식 실행 및 결과 처리 ===
            // 동기적으로 음성 인식 수행
            RecognizeResponse response = speechClient.recognize(request);
            
            // 인식 결과 리스트 추출
            List<SpeechRecognitionResult> results = response.getResultsList();

            // 각 인식 결과를 순회하며 처리
            for (SpeechRecognitionResult result : results) {
                // 음성의 각 부분에 대해 여러 개의 대안 텍스트가 제공될 수 있음
                // 여기서는 가장 확률이 높은 첫 번째 대안만 사용
                if (result.getAlternativesCount() > 0) {
                    // 첫 번째(가장 가능성이 높은) 대안 텍스트 가져오기
                    SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                    
                    // 변환된 텍스트를 콘솔에 출력
                    System.out.printf("Transcription: %s%n", alternative.getTranscript());
                }
            }
        } // try-with-resources가 자동으로 speechClient.close() 호출
    }
}