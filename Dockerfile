FROM eclipse-temurin:21-jdk as build

WORKDIR /workspace

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


