# 서비스 모듈 하나를 담는 이미지. MODULE 빌드 인자로 고른다 (service-user, service-community, service-car, service-media)
ARG MODULE=service-user

# 빌드: Gradle 로 실행 가능한 jar 를 만든다 (테스트는 CI/로컬에서 따로 돌린다)
FROM eclipse-temurin:25-jdk AS build
ARG MODULE
WORKDIR /workspace

# 소스가 바뀌어도 의존성 레이어는 캐시되도록 빌드 스크립트를 먼저 복사한다
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
COPY common/build.gradle.kts common/
COPY service-user/build.gradle.kts service-user/
COPY service-community/build.gradle.kts service-community/
COPY service-car/build.gradle.kts service-car/
COPY service-media/build.gradle.kts service-media/
RUN ./gradlew :${MODULE}:dependencies --no-daemon > /dev/null

COPY common common
COPY ${MODULE} ${MODULE}
RUN ./gradlew :${MODULE}:bootJar --no-daemon -x test

# 실행: JRE 만 담고 root 가 아닌 사용자로 띄운다
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
USER app

ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Seoul
EXPOSE 8080
# 컨테이너 메모리 제한에 맞춰 힙 크기를 정한다
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

# CI: 앞 단계에서 만든 jar 를 그대로 담는다 (모듈 jar 를 모은 디렉터리를 컨텍스트로 --target ci)
FROM runtime AS ci
ARG MODULE
COPY --chown=app:app ${MODULE}-*-SNAPSHOT.jar app.jar

# 로컬 기본 대상: 이미지 안에서 빌드한다
FROM runtime AS local
ARG MODULE
COPY --from=build --chown=app:app /workspace/${MODULE}/build/libs/${MODULE}-*-SNAPSHOT.jar app.jar
