plugins {
    `java-library`
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}
tasks.getByName<Jar>("jar") {
    enabled = true
}

dependencies {
    "api"("org.springframework.boot:spring-boot-starter-data-jpa")
    "api"("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    "api"("com.google.code.gson:gson")

    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.springframework:spring-web")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    testImplementation("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    constraints {
        implementation("io.github.classgraph:classgraph:4.8.165")
    }
}

val generated = file("build/generated/querydsl")

tasks.withType<JavaCompile> {
    options.generatedSourceOutputDirectory.set(generated)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

sourceSets {
    main {
        java {
            srcDirs(generated)
        }
    }
}