dependencies {
    implementation(project(":core"))
    implementation(project(":api"))

    // Spring Batch
    implementation("org.springframework.boot:spring-boot-starter-batch")

    implementation("org.springframework.boot:spring-boot-starter-json")

    // ShedLock (분산 락)
    implementation("net.javacrumbs.shedlock:shedlock-spring:5.13.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.13.0")

    // Batch Testing
    testImplementation("org.springframework.batch:spring-batch-test")
    testImplementation("com.h2database:h2")
}
