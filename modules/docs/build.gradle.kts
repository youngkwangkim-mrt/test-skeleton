plugins {
    id("org.asciidoctor.jvm.convert") version "4.0.4"
}

repositories {
    mavenCentral()
}

val snippetsDir = file("build/generated-snippets")

tasks {
    named<org.asciidoctor.gradle.jvm.AsciidoctorTask>("asciidoctor") {
        dependsOn("copySnippets")

        setSourceDir(file("src/docs/asciidoc"))
        setOutputDir(file("build/docs"))
        baseDirFollowsSourceFile()

        attributes(
            mapOf(
                "snippets" to snippetsDir.absolutePath,
                "source-highlighter" to "highlightjs",
                "toc" to "left",
                "toclevels" to "3",
                "icons" to "font",
                "sectanchors" to "",
                "sectnums" to ""
            )
        )

        outputOptions {
            backends("html5")
        }
    }

    register("docs") {
        group = "documentation"
        description = "Generate REST Docs HTML documentation"
        dependsOn("asciidoctor")
    }

    named("build") {
        dependsOn("docs")
    }

    register<Copy>("copySnippets") {
        group = "documentation"
        description = "Copy generated snippets from test modules"

        dependsOn(
            ":modules:bootstrap:common-api-app:test",
            ":modules:bootstrap:skeleton-api-app:test"
        )

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        from("${rootProject.projectDir}/modules/bootstrap/common-api-app/build/generated-snippets")
        from("${rootProject.projectDir}/modules/bootstrap/skeleton-api-app/build/generated-snippets")

        into(snippetsDir)

        // Always run this task (don't use cache)
        outputs.upToDateWhen { false }
    }
}
