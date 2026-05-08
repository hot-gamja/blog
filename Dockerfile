# 1단계: 빌드
# Gradle + JDK 21 이미지로 JAR 파일 생성
FROM gradle:8.5-jdk21 AS build

# 작업 디렉토리 설정
WORKDIR /app

# 의존성 캐시를 위해 gradle 파일 먼저 복사
# 소스코드가 바뀌어도 의존성이 안 바뀌면 이 레이어는 캐시 사용
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 레이어)
RUN gradle dependencies --no-daemon || true

# 나머지 소스코드 복사
COPY src ./src

# JAR 빌드 (테스트 제외)
RUN gradle bootJar --no-daemon -x test


# 2단계: 실행
# JRE만 있는 가벼운 이미지 사용 (빌드 도구 불필요)
FROM eclipse-temurin:21-jre

# 작업 디렉토리 설정
WORKDIR /app

# 업로드 디렉토리 생성 (사진 업로드용)
RUN mkdir -p /opt/myapp/uploads/photos

# 1단계에서 생성된 JAR만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 8080 포트 열기 (문서용, 실제 포트 바인딩은 docker run에서)
EXPOSE 8080

# prod 프로파일로 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
