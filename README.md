# Google Speech-to-Text v2 API Spring Boot Application

## 프로젝트 개요

이 프로젝트는 Google Cloud Speech-to-Text v2 API를 활용한 Spring Boot 기반의 음성 인식 웹 애플리케이션입니다. 웹 브라우저에서 녹음한 음성을 텍스트로 변환하는 기능을 제공합니다.

## 주요 특징

### Google Speech-to-Text v2 API 특징

1. **Recognizer 기반 아키텍처**
   - v1과 달리 v2는 recognizer 리소스를 중심으로 작동
   - 재사용 가능한 음성 인식 설정 저장
   - 더 효율적인 리소스 관리

2. **향상된 인식 모델**
   - `long` 모델: 긴 음성 파일에 최적화
   - 자동 인코딩 감지 (AutoDetectDecodingConfig)
   - 다국어 지원 향상

3. **두 가지 인식 방식 지원**
   - **Inline Recognition**: 임시 recognizer 사용
   - **Recognizer 방식**: 영구 recognizer 리소스 사용

## 아키텍처 및 플로우

### 전체 시스템 플로우

```
[웹 브라우저] → [Spring Boot Server] → [Google Speech v2 API]
      ↓                    ↓                      ↓
   녹음 시작           음성 파일 수신           음성 인식 처리
      ↓                    ↓                      ↓
   녹음 중지           ByteString 변환          텍스트 반환
      ↓                    ↓                      ↓
  파일 업로드          API 요청 생성                ↓
      ↓                    ↓                      ↓
      ←─────────────  텍스트 결과  ←──────────────
```

### 주요 컴포넌트

#### 1. GoogleCloudConfig (`/config/GoogleCloudConfig.java`)
- **역할**: Speech v2 API 클라이언트 설정 및 생성
- **주요 기능**:
  - Base64로 인코딩된 서비스 계정 키 디코딩
  - Location별 endpoint 설정 (global vs regional)
  - SpeechClient Bean 생성 및 관리

#### 2. SpeechRecorderController (`/controller/SpeechRecorderController.java`)
- **엔드포인트**: `/api/speech/upload`
- **방식**: Inline Recognition
- **특징**:
  - `recognizers/_` 특수 경로 사용
  - 권한 없이도 사용 가능
  - 매 요청마다 임시 recognizer 자동 생성

#### 3. RecognizerController (`/controller/RecognizerController.java`)
- **엔드포인트**: `/api/recognizer/upload`
- **방식**: Recognizer 기반
- **특징**:
  - 영구 recognizer 리소스 생성/재사용
  - Speech-to-Text Editor 권한 필요
  - 프로덕션 환경에 적합

### 처리 플로우 상세

#### Inline Recognition 플로우 (SpeechRecorderController)
```
1. 클라이언트 → 음성 파일 업로드 (webm 형식)
2. MultipartFile → byte[] → ByteString 변환
3. RecognitionConfig 생성:
   - 언어: ko-KR (한국어)
   - 모델: long (긴 음성용)
   - 인코딩: 자동 감지
4. RecognizeRequest 생성:
   - recognizer: "projects/{projectId}/locations/{location}/recognizers/_"
   - config: 위에서 생성한 설정
   - content: 음성 데이터
5. speechClient.recognize() 호출
6. 응답에서 첫 번째 결과의 가장 신뢰도 높은 대안 추출
7. JSON 응답 반환
```

#### Recognizer 방식 플로우 (RecognizerController)
```
1. 클라이언트 → 음성 파일 업로드
2. Recognizer 존재 확인:
   - 있으면: 기존 recognizer 사용
   - 없으면: 새로 생성 (최대 5분 대기)
3. Recognition 요청은 Inline과 유사
4. 단, recognizer 경로가 실제 리소스 경로
   예: "projects/{projectId}/locations/{location}/recognizers/permanent-recognizer"
```

## v1 대비 v2 주요 차이점

| 항목 | v1 API | v2 API |
|------|--------|--------|
| 아키텍처 | 직접 인식 요청 | Recognizer 리소스 기반 |
| Endpoint | speech.googleapis.com | location별 endpoint |
| 권한 | 기본 Speech API 권한 | Recognizer 생성 시 추가 권한 필요 |
| 설정 재사용 | 매번 설정 전송 | Recognizer에 설정 저장 가능 |
| 리소스 관리 | 없음 | Recognizer 생성/삭제 관리 |

## 환경 설정

### 필수 설정

1. **Google Cloud 서비스 계정 키**
   - Google Cloud Console에서 서비스 계정 생성
   - JSON 키 다운로드
   - Base64로 인코딩
   ```bash
   base64 google-credentials.json | tr -d '\n' > encoded.txt
   ```

2. **환경 변수 설정** (`.env` 파일)
   ```
   GOOGLE_CREDENTIALS_JSON=<Base64로 인코딩된 서비스 계정 키>
   ```

3. **application.properties**
   ```properties
   google.cloud.credentials.json=${GOOGLE_CREDENTIALS_JSON}
   gcp.project-id=your-project-id
   gcp.location=global
   ```

### IAM 권한 설정

- **Inline Recognition**: Cloud Speech-to-Text User
- **Recognizer 방식**: Cloud Speech-to-Text Editor

## 실행 방법

### 개발 환경에서 실행

```bash
# 의존성 설치 및 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun
```

### IDE에서 실행

IDE에서 실행 시 `.env` 파일이 자동으로 로드됩니다 (DotenvConfig 클래스가 처리).

### 프로덕션 환경

```bash
# JAR 파일 생성
./gradlew bootJar

# JAR 실행
java -jar build/libs/stt-0.0.1-SNAPSHOT.jar
```

## 접속 URL

- **Inline Recognition**: http://localhost:8080/
- **Recognizer 방식**: http://localhost:8080/recognizer
- **API 엔드포인트**:
  - POST `/api/speech/upload` (Inline)
  - POST `/api/recognizer/upload` (Recognizer)

## 보안 고려사항

1. **서비스 계정 키 관리**
   - 실제 키는 `.env` 파일에만 저장
   - `.env` 파일은 `.gitignore`에 추가
   - application.properties에 실제 키 하드코딩 금지

2. **Base64 인코딩**
   - 보안을 위한 것이 아닌 JSON 문자열 처리 편의를 위한 것
   - 실제 보안은 파일 접근 제어로 관리

3. **권한 최소화**
   - Inline Recognition만 사용한다면 User 권한으로 충분
   - Recognizer 생성이 필요할 때만 Editor 권한 부여

## 트러블슈팅

### 404 Not Found 오류
- **원인**: 잘못된 endpoint 설정 또는 존재하지 않는 recognizer
- **해결**: 
  - global location은 `speech.googleapis.com` 사용
  - Inline recognition (`recognizers/_`) 사용

### Permission Denied 오류
- **원인**: recognizer 생성 권한 부족
- **해결**: 
  - Inline recognition 사용 또는
  - 서비스 계정에 Editor 권한 부여

### 환경 변수 오류
- **원인**: `.env` 파일이 로드되지 않음
- **해결**: DotenvConfig가 정상 작동하는지 확인

### 음성 인식 결과 없음
- **원인**: 음성 품질 문제 또는 언어 설정 불일치
- **해결**: 
  - 명확한 발음으로 재녹음
  - 언어 코드 확인 (ko-KR)
  - 오디오 형식 확인 (webm)

## 프로세스 관리

```bash
# 실행 중인 프로세스 확인
lsof -i :8080

# 프로세스 종료
kill -9 [PID]

# 백그라운드 실행
nohup ./gradlew bootRun > app.log 2>&1 &
```

## 참고 문서

- [Google Speech-to-Text v2 API 문서](https://cloud.google.com/speech-to-text/v2/docs)
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [Google Cloud Java Client Libraries](https://cloud.google.com/java/docs/reference)