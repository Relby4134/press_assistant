plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "by.presassistant"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.telegram:telegrambots-longpolling:7.10.0")
    implementation("org.telegram:telegrambots-client:7.10.0")
    implementation("org.telegram:telegrambots-springboot-longpolling-starter:7.10.0")
    implementation("org.apache.poi:poi-ooxml:5.3.0")
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("com.h2database:h2")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// === Addin build ===
val npm = if (System.getProperty("os.name").lowercase().contains("windows")) "npm.cmd" else "npm"
val addinDir       = file("pres-assistant-addin")
val addinDistDir   = file("pres-assistant-addin/dist")
val staticAddinDir = file("src/main/resources/static/addin")

val npmInstall by tasks.registering(Exec::class) {
    workingDir(addinDir)
    commandLine(npm, "install")
    inputs.file(addinDir.resolve("package.json"))
    outputs.dir(addinDir.resolve("node_modules"))
}

val buildAddin by tasks.registering(Exec::class) {
    dependsOn(npmInstall)
    workingDir(addinDir)
    commandLine(npm, "run", "build")
    inputs.dir(addinDir.resolve("src"))
    inputs.file(addinDir.resolve("webpack.config.js"))
    inputs.file(addinDir.resolve("manifest.xml"))
    outputs.dir(addinDistDir)
}

val copyAddinToResources by tasks.registering(Copy::class) {
    dependsOn(buildAddin)
    from(addinDistDir)
    into(staticAddinDir)
}

tasks.named("processResources") {
    dependsOn(copyAddinToResources)
}