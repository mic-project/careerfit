# 백엔드 전용 Docker 이미지

# === Backend Build ===
FROM openjdk:17-jdk-slim AS backend-build
WORKDIR /app

# Gradle wrapper 복사
COPY gradlew ./
COPY gradle/ ./gradle/

# Gradle 설정 파일들 복사
COPY settings.gradle ./
COPY backend/build.gradle ./backend/

# 권한 설정
RUN chmod +x ./gradlew

# 의존성 다운로드 (캐시됨)
RUN ./gradlew dependencies --no-daemon

# 백엔드 소스 코드 복사 및 빌드
COPY backend/ ./backend/
RUN ./gradlew :backend:bootJar --no-daemon

# === Runtime Image ===
FROM openjdk:17-jdk-slim

# 시스템 패키지 업데이트 및 필요한 도구 설치
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 애플리케이션 디렉토리 생성
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=backend-build /app/backend/build/libs/*.jar app.jar

# 기존 정적 리소스 복사 (backend/src/main/resources/static)
COPY --from=backend-build /app/backend/src/main/resources/static/ /app/resources/static/

# 포트 노출
EXPOSE 8080

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# JVM 최적화 설정
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport"

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]