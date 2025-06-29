# Google Speech-to-Text v2 API 오류 해결 과정

## 1. 최초 오류: 404 Not Found

### 오류 메시지
```
io.grpc.StatusRuntimeException: UNIMPLEMENTED: HTTP status code 404
The requested URL /google.cloud.speech.v2.Speech/Recognize was not found on this server.
```

### 원인 분석
1. **잘못된 Endpoint 설정**: `global-speech.googleapis.com:443`로 설정되어 있었음
2. **존재하지 않는 Recognizer 참조**: 동적으로 생성한 recognizer ID를 사용했지만, 실제로는 존재하지 않았음

### 최초 코드 (문제가 있던 부분)

**GoogleCloudConfig.java:**
```java
String endpoint = String.format("%s-speech.googleapis.com:443", location);
```

**SpeechRecorderController.java:**
```java
String recognizerId = "my-dynamic-recognizer-" + System.currentTimeMillis();
RecognizerName recognizerName = RecognizerName.of(projectId, location, recognizerId);

RecognizeRequest request = RecognizeRequest.newBuilder()
    .setRecognizer(recognizerName.toString())
    .setConfig(config)
    .setContent(audioData)
    .build();
```

### 첫 번째 수정 사항

**GoogleCloudConfig.java 수정:**
```java
// Speech v2 API는 location에 따라 다른 endpoint 사용
String endpoint;
if ("global".equals(location)) {
    endpoint = "speech.googleapis.com:443";  // global은 prefix 없이 사용
} else {
    endpoint = String.format("%s-speech.googleapis.com:443", location);
}
```

**수정 이유**: 
- Google Speech v2 API에서 `global` location은 `global-speech.googleapis.com`이 아닌 `speech.googleapis.com`을 사용해야 함
- 다른 region(예: us-central1)은 `{region}-speech.googleapis.com` 형식 사용

**SpeechRecorderController.java 수정:**
```java
// Speech v2 API recognizer 생성
String recognizerId = "default-recognizer";
RecognizerName recognizerName = RecognizerName.of(projectId, location, recognizerId);

// Recognizer가 없으면 생성
try {
    speechClient.getRecognizer(recognizerName);
    log.info("기존 recognizer 사용: {}", recognizerName);
} catch (Exception e) {
    log.info("새 recognizer 생성 중: {}", recognizerName);
    Recognizer recognizer = Recognizer.newBuilder()
        .setDefaultRecognitionConfig(RecognitionConfig.newBuilder()
            .addLanguageCodes("ko-KR")
            .setModel("long")
            .setAutoDecodingConfig(AutoDetectDecodingConfig.newBuilder().build())
            .build())
        .build();
    
    CreateRecognizerRequest createRequest = CreateRecognizerRequest.newBuilder()
        .setParent(LocationName.of(projectId, location).toString())
        .setRecognizerId(recognizerId)
        .setRecognizer(recognizer)
        .build();
    
    speechClient.createRecognizerAsync(createRequest).get();
}
```

**수정 이유**:
- Speech v2 API는 v1과 달리 recognizer 리소스를 먼저 생성해야 함
- recognizer는 음성 인식 설정을 저장하는 재사용 가능한 리소스
- 동적으로 생성된 ID 대신 고정된 ID 사용

## 2. 두 번째 오류: Permission Denied

### 오류 메시지
```
com.google.api.gax.rpc.PermissionDeniedException: 
PERMISSION_DENIED: Permission 'speech.recognizers.create' denied on resource (or it may not exist).
```

### 원인 분석
- Service Account에 `speech.recognizers.create` 권한이 없음
- Speech v2 API의 recognizer 생성은 추가적인 IAM 권한이 필요

### 최종 해결책: Inline Recognition 사용

**최종 수정 코드:**
```java
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
```

### 최종 수정 이유
1. **Inline Recognition 사용**: 
   - `recognizers/_` 특수 경로 사용
   - 이는 임시 recognizer를 자동으로 생성하여 사용
   - 별도의 recognizer 생성 권한 불필요

2. **권한 문제 해결**:
   - `speech.recognizers.create` 권한 없이도 음성 인식 가능
   - 기본 Speech-to-Text API 권한만으로 충분

## 주요 학습 포인트

### 1. Speech API v1 vs v2 차이점
- **v1**: 직접 음성 인식 요청 가능
- **v2**: recognizer 리소스 기반 접근 방식
  - 명시적 recognizer 생성 필요 (권한 필요)
  - 또는 inline recognition 사용 (`recognizers/_`)

### 2. Google Cloud API Endpoint 규칙
- Global 서비스: `{service}.googleapis.com`
- Regional 서비스: `{region}-{service}.googleapis.com`
- Speech v2의 global은 prefix 없이 사용

### 3. IAM 권한 계층
- 기본 Speech-to-Text 사용: `speech.recognize` 권한만 필요
- Recognizer 생성/관리: 추가 권한 필요
  - `speech.recognizers.create`
  - `speech.recognizers.update`
  - `speech.recognizers.delete`

### 4. 오류 해결 전략
1. 404 오류 → Endpoint 및 리소스 경로 확인
2. Permission Denied → 필요한 권한 확인 또는 다른 접근 방식 시도
3. API 문서에서 inline/shorthand 방식 확인

## 참고사항
- Inline recognition은 일회성 음성 인식에 적합
- 동일한 설정으로 반복 인식이 필요한 경우 명시적 recognizer 생성이 효율적
- Production 환경에서는 필요한 IAM 권한을 Service Account에 부여하는 것이 권장됨