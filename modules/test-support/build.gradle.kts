plugins {
    `java-test-fixtures`
}

dependencies {
    testFixturesApi(rootProject.libs.bundles.test.fixtures)
    testFixturesApi(rootProject.libs.bundles.jackson)
    testFixturesApi(rootProject.libs.bundles.kotlin)

    testFixturesApi(project(":modules:common"))
    testFixturesApi(project(":modules:common-web"))
}
