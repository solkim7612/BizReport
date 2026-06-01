dependencies {
    implementation(project(":core"))
    implementation(project(":batch"))

    // Web & Validation
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    implementation("com.google.cloud:google-cloud-vision:3.34.0")
}