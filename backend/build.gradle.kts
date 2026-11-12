// Versiones del backend. Si IntelliJ sugiere unas más nuevas, actualícenlas en un PR propio.
val ktorVersion = "3.1.3"
val logbackVersion = "1.5.18"
val mongoVersion = "5.4.0"
val bcryptVersion = "0.10.2"

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    application
}

group = "mx.gotadevida"
version = "0.1.0"

application {
    mainClass.set("mx.gotadevida.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // Servidor
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-rate-limit:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")

    // Cliente HTTP (verificar el token de Google)
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // Base de datos
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:$mongoVersion")
    implementation("org.mongodb:bson-kotlinx:$mongoVersion")

    // Seguridad y registros
    implementation("at.favre.lib:bcrypt:$bcryptVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
