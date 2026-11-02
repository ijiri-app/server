plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.spring") version "2.4.21"
    kotlin("plugin.jpa") version "2.4.21"
    id("org.springframework.boot") version "3.5.13"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ijiri"
version = "0.0.1-SNAPSHOT"
description = "IjiriServer"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")
    implementation("me.paulschwarz:spring-dotenv:4.0.0")
    // Cloudflare R2 (S3 호환) 사진 저장소: presigned URL, HEAD, 복사, 삭제
    implementation(platform("software.amazon.awssdk:bom:2.55.10"))
    implementation("software.amazon.awssdk:s3") {
        exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
    }
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    // 테스트 DB: Docker 없이 프로세스로 띄우는 PostgreSQL (운영과 같은 17, pg_trgm 포함)
    testImplementation("io.zonky.test:embedded-database-spring-test:2.8.0")
    testImplementation("io.zonky.test:embedded-postgres:2.2.2")
    testImplementation(platform("io.zonky.test.postgres:embedded-postgres-binaries-bom:17.11.0"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        // -Xemit-jvm-type-annotations: List<@Valid ...> 같은 컨테이너 원소 검증이 동작하도록 타입 어노테이션을 남긴다
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xemit-jvm-type-annotations")
    }
}

// Hibernate 지연 로딩 프록시가 엔티티를 상속할 수 있도록 연다
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
