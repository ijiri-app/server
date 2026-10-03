# 빌드: Gradle 로 실행 가능한 jar 를 만든다 (테스트는 CI/로컬에서 따로 돌린다. Testcontainers 는 빌드 안에서 못 돈다)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 소스가 바뀌어도 의존성 레이어는 캐시되도록 빌드 스크립트를 먼저 복사한다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon > /dev/null

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# 실행: JRE 만 담고 root 가 아닌 사용자로 띄운다
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/build/libs/IjiriServer-*-SNAPSHOT.jar app.jar
USER app

ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Seoul
EXPOSE 8080
# 컨테이너 메모리 제한에 맞춰 힙 크기를 정한다
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
