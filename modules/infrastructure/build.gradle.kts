// Infrastructure module - Database, Cache, External services

apply(plugin = "org.springframework.boot")
apply(plugin = "org.jetbrains.kotlin.kapt")

tasks.processResources {
    filesMatching("**/redisson-*.yml") {
        expand("projectName" to rootProject.name)
    }
}

dependencies {
    val annotationProcessor by configurations
    val runtimeOnly by configurations

    // Domain module
    api(project(":modules:domain"))

    // Spring Boot (for EntityScan annotation)
    implementation(rootProject.libs.spring.boot.starter)

    // Spring Data & QueryDSL
    implementation(rootProject.libs.bundles.spring.data)

    // Cache (Caffeine, Redisson, LZ4)
    implementation(rootProject.libs.bundles.cache)

    // Apache POI (Excel Export)
    implementation(rootProject.libs.poi.ooxml)

    // Slack SDK
    implementation(rootProject.libs.slack.api.client)

    // RestClient (HTTP Client with tracing support)
    implementation(rootProject.libs.spring.boot.starter.restclient)
    implementation(rootProject.libs.spring.boot.starter.aspectj)
    implementation(rootProject.libs.bundles.metrics)

    // Database
    runtimeOnly(rootProject.libs.h2)
    runtimeOnly(rootProject.libs.mysql)
}

// Pass system properties to tests (for benchmark tests)
tasks.test {
    systemProperty("benchmark", System.getProperty("benchmark") ?: "false")
}
