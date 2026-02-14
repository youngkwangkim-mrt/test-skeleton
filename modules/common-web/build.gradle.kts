// Common Web module - Spring Web components (Filters, Interceptors, Response)

dependencies {
    api(rootProject.libs.bundles.spring.web)
    api(rootProject.libs.bundles.spring.security)
    api(rootProject.libs.bundles.metrics)

    // Swagger/OpenAPI
    api(rootProject.libs.bundles.openapi)
}
