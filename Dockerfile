# 사용할 베이스 이미지 (Java 17 버전)
FROM openjdk:21-jdk-slim

# Gradle Wrapper 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# 소스 코드 복사
COPY src src

# JAR 파일 빌드
RUN ./gradlew build -x test

# 빌드된 JAR 파일을 app.jar로 복사
RUN cp build/libs/dearmind-0.0.1-SNAPSHOT.jar app.jar

# 포트 노출
EXPOSE 8080

# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# 컨테이너 실행 시 자바 명령어를 통해 JAR 파일을 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]