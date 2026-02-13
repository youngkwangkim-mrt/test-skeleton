// Domain module - business rules and domain models
// Depends only on common module (auto-injected by root build.gradle.kts)

apply(plugin = "org.jetbrains.kotlin.kapt")

dependencies {
    implementation(rootProject.libs.spring.boot.starter)
    implementation(rootProject.libs.spring.boot.starter.data.jpa)

    // QueryDSL
    implementation(rootProject.libs.querydsl.jpa)

    // QueryDSL Annotation Processor (kapt for Kotlin entities)
    "kapt"(rootProject.libs.querydsl.apt.get().toString() + ":jakarta")
    "kapt"("jakarta.annotation:jakarta.annotation-api")
    "kapt"("jakarta.persistence:jakarta.persistence-api")
}

// QueryDSL Q-class generation path
val querydslDir: Provider<Directory> = layout.buildDirectory.dir("generated/source/kapt/main")

configure<JavaPluginExtension> {
    sourceSets {
        getByName("main") {
            java.srcDir(querydslDir)
        }
    }
}
