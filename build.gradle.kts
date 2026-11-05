import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    kotlin("jvm") version "2.4.21" apply false
    kotlin("plugin.spring") version "2.4.21" apply false
    kotlin("plugin.jpa") version "2.4.21" apply false
    id("org.springframework.boot") version "3.5.13" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "ijiri"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

// 모든 모듈 공통: Kotlin, Spring Boot BOM, JPA 엔티티 open, JUnit
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "io.spring.dependency-management")

    // Spring Boot BOM 이 Kotlin 라이브러리를 BOM 의 Kotlin 버전으로 내리지 않도록 플러그인 버전을 알려 준다
    // (Boot 플러그인을 적용하는 서비스 모듈은 자동으로 하지만 common 은 아니다)
    extra["kotlin.version"] = getKotlinPluginVersion()

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    configure<DependencyManagementExtension> {
        imports {
            mavenBom(SpringBootPlugin.BOM_COORDINATES)
        }
    }

    configure<KotlinJvmProjectExtension> {
        compilerOptions {
            // -Xemit-jvm-type-annotations: List<@Valid ...> 같은 컨테이너 원소 검증이 동작하도록 타입 어노테이션을 남긴다
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xemit-jvm-type-annotations")
        }
    }

    // Hibernate 지연 로딩 프록시가 엔티티를 상속할 수 있도록 연다
    configure<AllOpenExtension> {
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.MappedSuperclass")
        annotation("jakarta.persistence.Embeddable")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// 서비스 모듈 공통: 실행 가능한 Spring Boot 앱, common 라이브러리, 임베디드 PostgreSQL 테스트
configure(subprojects.filter { it.name.startsWith("service-") }) {
    apply(plugin = "org.springframework.boot")

    dependencies {
        "implementation"(project(":common"))
        "implementation"("me.paulschwarz:spring-dotenv:4.0.0")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.springframework.security:spring-security-test")
        // 테스트 DB: Docker 없이 프로세스로 띄우는 PostgreSQL (운영과 같은 17, pg_trgm 포함)
        "testImplementation"("io.zonky.test:embedded-database-spring-test:2.8.0")
        "testImplementation"("io.zonky.test:embedded-postgres:2.2.2")
        "testImplementation"(platform("io.zonky.test.postgres:embedded-postgres-binaries-bom:17.11.0"))
        // 기본은 amd64 바이너리만 들어 있어 Apple Silicon 과 ARM 리눅스에서는 에뮬레이션으로 돈다
        "testRuntimeOnly"("io.zonky.test.postgres:embedded-postgres-binaries-darwin-arm64v8")
        "testRuntimeOnly"("io.zonky.test.postgres:embedded-postgres-binaries-linux-arm64v8")
        "testImplementation"("org.mockito.kotlin:mockito-kotlin:5.4.0")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    // 루트의 .env 를 읽도록 bootRun 작업 디렉터리를 루트로 둔다
    tasks.withType<BootRun> {
        workingDir = rootProject.projectDir
    }

    // 실행 가능한 jar 만 만든다 (-plain.jar 불필요)
    tasks.named<Jar>("jar") {
        enabled = false
    }
}
