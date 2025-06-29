// build.gradle.kts
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.4.3"
	id("io.spring.dependency-management") version "1.1.4"
	kotlin("jvm") version "1.9.22"
	kotlin("plugin.spring") version "1.9.22"
}

group = "com.extrabox"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.apache.poi:poi:5.2.3")
	implementation("org.apache.poi:poi-ooxml:5.2.3")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")
	implementation("com.squareup.okhttp3:okhttp:4.11.0")
	implementation("org.postgresql:postgresql")

	implementation("com.newrelic.agent.java:newrelic-api:8.21.0")

	implementation("org.springframework.boot:spring-boot-starter-actuator")

	implementation("org.springframework.boot:spring-boot-starter-amqp")

	implementation(platform("com.itextpdf:itext7-core:7.2.5"))
	implementation("com.itextpdf:kernel:7.2.5")
	implementation("com.itextpdf:io:7.2.5")
	implementation("com.itextpdf:layout:7.2.5")
	implementation("com.itextpdf:html2pdf:4.0.5")
	implementation("com.itextpdf:forms:7.2.5")
	implementation("com.itextpdf:pdfa:7.2.5")

	// Para conversão de Markdown para HTML
	implementation("com.atlassian.commonmark:commonmark:0.17.0")
	implementation("com.atlassian.commonmark:commonmark-ext-gfm-tables:0.17.0")
	implementation("com.atlassian.commonmark:commonmark-ext-heading-anchor:0.17.0")

	implementation("com.mercadopago:sdk-java:2.1.29")

	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
	implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs += "-Xjsr305=strict"
		jvmTarget = "21"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}