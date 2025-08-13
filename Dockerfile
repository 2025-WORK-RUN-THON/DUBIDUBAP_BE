FROM eclipse-temurin:21-jre as runtime

WORKDIR /app

# 빌드 산출물 복사 (CI나 로컬에서 bootJar 생성 후 복사)
ARG JAR_FILE=build/libs/*-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# 환경변수 (기본값 제공)
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]


