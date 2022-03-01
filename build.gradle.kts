import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
    kotlin("jvm") version "1.5.31"

    id("io.kotest") version "0.3.8"
    id("org.jetbrains.kotlinx.kover") version "0.4.2"
}

group = "me.l3n.bot.discord.lod"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // AWS Lambda
    implementation("com.amazonaws:aws-lambda-java-core:1.2.0")

    // Config
    val hopliteVersion = "1.4.15"
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.2.6")

    // DI
    val koinVersion = "3.1.3"
    implementation("io.insert-koin:koin-core:$koinVersion")

    // HTTP
    val ktorVersion = "1.6.3"
    implementation("io.ktor:ktor:$ktorVersion")
    implementation("io.ktor:ktor-client-gson:$ktorVersion")

    // Discord
    implementation("dev.kord:kord-core:0.7.4")

    // Html Parser
    implementation("org.jsoup:jsoup:1.10.2")

    // Tests
    testImplementation(kotlin("test"))

    val kotestVersion = "4.6.3"
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")

    testImplementation("io.insert-koin:koin-core:$koinVersion")
    testImplementation("io.mockk:mockk:1.12.1")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "me.l3n.bot.discord.lod.MainKt"
    }
}

task<Exec>("run") {
    group = "aws lambda"

    dependsOn(getSamBuildTask())

    runCommand("""sam local invoke "LeagueOfDraven-Bot-Kotlin" -e sample-event.json""")
}

task<Exec>("deploy") {
    group = "aws lambda"

    dependsOn(getSamBuildTask("prod"))

    runCommand("sam deploy", "y")
}

fun getSamBuildTask(env: String = ""): Task {
    return task<Exec>("samBuild${env.capitalize()}") {
        group = "aws lambda"

        val fileName = if (env.isEmpty()) "template.yaml" else "template-$env.yaml"

        dependsOn("clean", "kotest", "shadowJar")

        doFirst {
            runCommand("sam build -t $fileName")
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

fun Exec.runCommand(command: String, input: String? = null) {
    val isWindows =
        org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem().isWindows

    lateinit var shell: String
    lateinit var arg: String

    if (isWindows) {
        shell = "cmd"
        arg = "/c"
    } else {
        shell = "bash"
        arg = " -c"
    }

    if (input == null)
        commandLine(shell, arg, command)
    else
        commandLine(shell, arg, "echo $input | $command")
}
