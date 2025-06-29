# Spring Boot STT Application

Google Cloud Speech-to-Text API를 사용하는 Spring Boot 애플리케이션입니다.

## IDE에서 실행하기

IDE에서 애플리케이션을 실행할 때 환경변수 오류가 발생하는 경우:

1. **현재 application.properties는 환경변수가 없어도 기본값을 사용하도록 설정되어 있습니다**
   - `${GOOGLE_CREDENTIALS_JSON:기본값}` 형식으로 구성됨

2. **IntelliJ IDEA 설정 방법**:
   - Run Configuration > Environment Variables에서 설정
   - 또는 `.env` 파일의 내용을 복사하여 설정



## 터미널에서 실행하기

```bash
# 환경변수 로드 후 실행
source .env && export GOOGLE_CREDENTIALS_JSON && ./gradlew bootRun

# 백그라운드 실행
source .env && export GOOGLE_CREDENTIALS_JSON && nohup ./gradlew bootRun > app.log 2>&1 &
```

## 접속하기

- 웹 페이지: http://localhost:8080
- API 엔드포인트: POST http://localhost:8080/api/speech/upload

## 실행 중인 프로세스 확인 및 종료

```bash
# 포트 사용 확인
lsof -i :8080

# 프로세스 종료
kill -9 [PID]
```