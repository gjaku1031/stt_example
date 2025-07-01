# Google Cloud 자격 증명 설정 방법

## 1. Base64 인코딩이 필요한가?

**네, 직접 인코딩해야 합니다.** 현재 코드에서 Base64 디코딩을 하고 있습니다:

```java
// GoogleCloudConfig.java:31
byte[] decodedCredentials = Base64.getDecoder().decode(credentialsJson);
```

## 2. Base64 인코딩 방법

### 방법 1: 명령줄에서 인코딩
```bash
# Google Cloud에서 다운로드한 JSON 파일을 Base64로 인코딩
base64 -i google-credentials.json -o encoded.txt

# 또는 한 줄로 출력 (macOS/Linux)
base64 -w 0 google-credentials.json > encoded.txt

# macOS에서는 -w 옵션이 없으므로
base64 google-credentials.json | tr -d '\n' > encoded.txt
```

### 방법 2: 온라인 Base64 인코더 사용
1. https://www.base64encode.org/ 접속
2. JSON 파일 내용 붙여넣기
3. Encode 버튼 클릭
4. 결과 복사

### 방법 3: 프로그래밍 방식
```
// Java로 인코딩
String json = Files.readString(Paths.get("google-credentials.json"));
String encoded = Base64.getEncoder().encodeToString(json.getBytes());
System.out.println(encoded);
```

## 3. 어떤 파일이 사용되는가?

### Spring Boot 설정 우선순위 (높은 순서대로):

1. **환경 변수** (.env 파일)
2. **application-{profile}.properties** (프로파일 활성화 시)
3. **application.properties** (기본)

### 현재 상황 분석

```
# application.properties와 application-local.properties에서
google.cloud.credentials.json=${GOOGLE_CREDENTIALS_JSON:기본값...}
```

이 설정은:
- `GOOGLE_CREDENTIALS_JSON` 환경 변수가 있으면 사용
- 없으면 `:` 뒤의 기본값 사용

### localhost:8080 실행 시 실제 사용되는 설정

1. **Gradle로 실행 시** (`./gradlew bootRun`):
   - `.env` 파일의 `GOOGLE_CREDENTIALS_JSON` 값이 사용됨
   - Spring Boot Gradle 플러그인이 자동으로 .env 파일을 읽음

2. **IDE에서 실행 시**:
   - IDE 설정에 따라 다름
   - 환경 변수 설정이 없으면 application.properties의 기본값 사용

3. **확인 방법**:
   ```
   // 실행 중인 설정 확인용 코드 추가
   @PostConstruct
   public void logConfig() {
       log.info("Using credentials from: {}", 
           System.getenv("GOOGLE_CREDENTIALS_JSON") != null ? ".env file" : "application.properties default");
   }
   ```

## 권장 사항

### 보안을 위한 설정 구조:

1. **개발 환경**:
   ```
   # .env (Git에서 제외)
   GOOGLE_CREDENTIALS_JSON=실제_base64_인코딩된_자격증명
   ```

2. **application.properties**:
   ```
   # 기본값 없이 환경 변수만 참조
   google.cloud.credentials.json=${GOOGLE_CREDENTIALS_JSON}
   ```

3. **.gitignore**:
   ```
   .env
   *.json
   google-credentials.json
   ```