FROM eclipse-temurin:21-jdk as build
WORKDIR /workspace

# 캐시 효율을 위해 Gradle 래퍼/메타 먼저 복사
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew --version

# 소스 복사 후 빌드
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre as runtime
WORKDIR /app

# 빌드 산출물 복사
COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar /app/app.jar

# 환경변수 (기본값 제공)
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]


