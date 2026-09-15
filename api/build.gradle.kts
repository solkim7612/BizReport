dependencies {
    implementation(project(":core"))
    implementation(project(":batch"))

    runtimeOnly("com.mysql:mysql-connector-j")

    // Web & Validation
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
}