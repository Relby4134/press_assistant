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
    implementation("com.formdev:flatlaf:3.4.1")
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

// === Standalone launcher JAR (LauncherApp + FlatLaf bundled) ===
val launcherJar by tasks.registering(Jar::class) {
    archiveClassifier.set("launcher")
    dependsOn(tasks.compileJava)
    manifest {
        attributes["Main-Class"] = "by.presassistant.LauncherApp"
    }
    from(sourceSets.main.get().output) {
        include("by/presassistant/LauncherApp*.class")
    }
    // Bundle FlatLaf so the thin JAR is self-contained
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.contains("flatlaf") }
            .map { zipTree(it) }
    }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
}

// === jpackage — Windows app image with bundled JRE ===
val packageApp by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Build Windows app image with bundled JRE (requires JDK 21+)"
    dependsOn(tasks.bootJar, launcherJar)

    val inputDir  = layout.buildDirectory.dir("jpackage-input").get().asFile
    val outputDir = layout.buildDirectory.dir("app").get().asFile
    val jpackage  = file(System.getProperty("java.home")).resolve("bin/jpackage.exe")
        .takeIf { it.exists() }
        ?: file(System.getProperty("java.home")).resolve("bin/jpackage")

    doFirst {
        delete(inputDir)
        delete(outputDir.resolve("PresAssistant"))
        inputDir.mkdirs()
        outputDir.mkdirs()

        tasks.bootJar.get().archiveFile.get().asFile
            .copyTo(inputDir.resolve("presassistant.jar"), overwrite = true)
        launcherJar.get().archiveFile.get().asFile
            .copyTo(inputDir.resolve("launcher.jar"), overwrite = true)

        listOf("presassistant-ca.crt", "install-cert.ps1", "register-addin.ps1").forEach { name ->
            val f = file(name)
            if (f.exists()) f.copyTo(inputDir.resolve(name), overwrite = true)
        }

        // Bundle bot credentials as defaults (gitignored, only present locally)
        val secrets = file("secrets.properties")
        if (secrets.exists()) secrets.copyTo(inputDir.resolve("launcher.properties"), overwrite = true)
    }

    commandLine(
        jpackage.absolutePath,
        "--type", "app-image",
        "--name", "PresAssistant",
        "--app-version", "1.0.0",
        "--vendor", "University",
        "--runtime-image", file(System.getProperty("java.home")).absolutePath,
        "--input", inputDir.absolutePath,
        "--dest", outputDir.absolutePath,
        "--main-jar", "launcher.jar",
        "--main-class", "by.presassistant.LauncherApp",
        "--java-options", "-Djava.awt.headless=false"
    )
}