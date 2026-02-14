import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply true
}

val javaVersion = JavaVersion.VERSION_25
val targetGradleVersion = "9.2.1"

// =============================================================================
// All Projects
// =============================================================================
allprojects {
    group = property("projectGroup") as String
    version = property("projectVersion") as String

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://repo.spring.io/snapshot") }
    }
}

// =============================================================================
// Subprojects Common Configuration
// =============================================================================
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "io.spring.dependency-management")

    // Import Spring Boot BOM for version management
    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        }
    }

    // Java Configuration
    configure<JavaPluginExtension> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
    }

    // Kotlin Configuration
    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-Xemit-jvm-type-annotations",
                "-java-parameters"
            )
            jvmTarget = JvmTarget.JVM_25
        }
    }

    // Test Configuration
    tasks.withType<Test> {
        jvmArgs("--enable-native-access=ALL-UNNAMED")
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    }

    // Common Dependencies
    dependencies {
        val implementation by configurations
        val testImplementation by configurations

        implementation(rootProject.libs.bundles.kotlin)
        implementation(rootProject.libs.bundles.jackson)
        testImplementation(rootProject.libs.bundles.test)
    }

    // Add common module to all subprojects (except common itself)
    if (project.name != "common") {
        dependencies {
            val implementation by configurations
            val compileOnly by configurations
            val annotationProcessor by configurations
            val testCompileOnly by configurations
            val testAnnotationProcessor by configurations

            implementation(project(":modules:common"))

            compileOnly(rootProject.libs.lombok)
            annotationProcessor(rootProject.libs.lombok)
            testCompileOnly(rootProject.libs.lombok)
            testAnnotationProcessor(rootProject.libs.lombok)
        }
    }
}

// =============================================================================
// Bootstrap App Modules (*-app)
// =============================================================================
configure(subprojects.filter { it.name.endsWith("-app") }) {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "org.jetbrains.kotlin.kapt")

    tasks.named<BootJar>("bootJar") {
        enabled = true
        archiveClassifier.set("boot")
    }

    tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }

    tasks.named<Jar>("jar") {
        enabled = false
    }

    dependencies {
        val implementation by configurations
        implementation(project(":modules:common-web"))
        implementation(project(":modules:infrastructure"))
        implementation(project(":modules:domain"))
    }
}

// =============================================================================
// Library Modules (non-app)
// =============================================================================
configure(subprojects.filter { !it.name.endsWith("-app") }) {
    afterEvaluate {
        tasks.findByName("bootJar")?.let { (it as BootJar).enabled = false }
    }

    tasks.named<Jar>("jar") {
        enabled = true
    }
}

// =============================================================================
// Dependency Resolution
// =============================================================================
configurations.all {
    resolutionStrategy {
        failOnVersionConflict()
        preferProjectModules()

        force(rootProject.libs.kotlin.reflect)
        force(rootProject.libs.kotlin.stdlib)

        cacheChangingModulesFor(0, TimeUnit.SECONDS)
        cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
    }
}

// =============================================================================
// Custom Tasks
// =============================================================================
tasks.register("cleanBuild") {
    group = "build"
    description = "Clean and build all projects"
    dependsOn(subprojects.map { "${it.path}:clean" })
    dependsOn(subprojects.map { "${it.path}:build" })
}

tasks.register<TestReport>("testReport") {
    group = "verification"
    description = "Generate aggregated test report"
    destinationDirectory.set(layout.buildDirectory.dir("reports/tests"))
    testResults.from(subprojects.map { it.tasks.withType<Test>() })
}

tasks.register("projectInfo") {
    group = "help"
    description = "Display project information"
    doLast {
        println("=".repeat(50))
        println("Project: ${project.name}")
        println("Version: ${project.version}")
        println("Group: ${project.group}")
        println("Java: ${JavaVersion.current()}")
        println("=".repeat(50))
        subprojects.forEach { println("  - ${it.name} (${it.path})") }
    }
}

tasks.wrapper {
    gradleVersion = targetGradleVersion
    distributionType = Wrapper.DistributionType.ALL
}
