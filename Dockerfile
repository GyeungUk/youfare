# 백엔드(Spring Boot) 배포용 멀티스테이지 이미지. Render/Railway/Fly.io 공용.
# 1단계: Gradle로 실행 가능한 jar 빌드 (테스트는 배포 시 제외해 빌드 단축)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew clean bootJar -x test --no-daemon

# 2단계: 가벼운 JRE 런타임에 jar만 복사
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# 컨테이너는 PaaS가 주입하는 $PORT 로 바인딩한다(application.yml: server.port=${PORT:8080}).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
