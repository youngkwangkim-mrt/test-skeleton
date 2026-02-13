---
title: "HTTP Client Patterns"
description: "Declarative HTTP client implementation using Spring HTTP Interface, RestClient configuration, logging interceptor"
category: "architecture"
order: 8
last_updated: "2026-02-14"
---

# HTTP Client Patterns

## Overview

This document describes HTTP client implementation patterns using Spring 6.1+'s HTTP Interface (HTTP Interface). The `@HttpExchange` annotation-based approach enables type-safe, concise external API integration.

**Traditional vs HTTP Interface:**

```kotlin
// Traditional (RestTemplate)
class TodoClient(private val restTemplate: RestTemplate) {
    fun findAll(): List<TodoDto> {
        return restTemplate.exchange("/todos", HttpMethod.GET, null,
            object : ParameterizedTypeReference<List<TodoDto>>() {}).body ?: emptyList()
    }
}

// HTTP Interface
@HttpExchange("/todos")
interface TodoClient {
    @GetExchange
    fun findAll(): List<TodoDto>
}
```

## Basic Structure

### 1. HTTP Interface Definition

Define HTTP Interface using `@HttpExchange` and method-specific annotations:

```kotlin
@HttpExchange("/todos")
interface TodoClient {
    @GetExchange
    fun findAll(): List<TodoDto>

    @GetExchange("/{id}")
    fun findById(@PathVariable id: Int): TodoDto

    @PostExchange
    fun create(@RequestBody todo: TodoDto): TodoDto

    @PutExchange("/{id}")
    fun update(@PathVariable id: Int, @RequestBody todo: TodoDto): TodoDto

    @DeleteExchange("/{id}")
    fun delete(@PathVariable id: Int)
}
```

**Annotations:**

| Annotation | HTTP Method | Description |
|-----------|-------------|------|
| `@GetExchange` | GET | Retrieve resources |
| `@PostExchange` | POST | Create resources |
| `@PutExchange` | PUT | Full update |
| `@PatchExchange` | PATCH | Partial update |
| `@DeleteExchange` | DELETE | Delete resources |
| `@HttpExchange` | - | Common config (base path) |

### 2. Configuration

Register HTTP Interface as Spring Bean using `@ImportHttpServices`:

```kotlin
@Configuration
@ImportHttpServices(group = "todo", types = [TodoClient::class])
class TodoClientConfig {

    @Bean
    fun todoClientGroupConfigurer(): RestClientHttpServiceGroupConfigurer {
        return RestClientHttpServiceGroupConfigurer { groups ->
            groups.filterByName("todo")
                .forEachClient { _, builder ->
                    builder
                        .baseUrl("https://jsonplaceholder.typicode.com")
                        .requestInterceptor(HttpLoggingInterceptor("TodoClient"))
                }
        }
    }
}
```

**Components:**
- `@ImportHttpServices`: Registers HTTP Interfaces as Beans
- `RestClientHttpServiceGroupConfigurer`: Configures `RestClient` per group

### 3. Service Usage

Inject the HTTP Interface bean into service classes:

```kotlin
@Service
class TodoService(private val todoClient: TodoClient) {
    fun getAllTodos(): List<TodoDto> = todoClient.findAll()
    fun getTodo(id: Int): TodoDto = todoClient.findById(id)
    fun createTodo(request: CreateTodoRequest): TodoDto {
        return todoClient.create(TodoDto(id = 0, title = request.title, completed = false))
    }
}
```

## RestClient Default Configuration

Configure default RestClient settings at the application level:

```kotlin
@Configuration
class RestClientConfig {

    @Bean
    @Primary
    fun defaultRestClient(restClientBuilder: RestClient.Builder): RestClient {
        return restClientBuilder
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .requestInterceptor(HttpLoggingInterceptor())
            .build()
    }
}
```

The configuration sets JSON headers and adds a logging interceptor by default.

## HttpLoggingInterceptor

The `HttpLoggingInterceptor` logs HTTP request and response details:

```kotlin
class HttpLoggingInterceptor(
    private val clientName: String = "RestClient",
    private val level: Level = Level.SIMPLE
) : ClientHttpRequestInterceptor {

    enum class Level { SIMPLE, FULL }

    override fun intercept(request: HttpRequest, body: ByteArray,
                          execution: ClientHttpRequestExecution): ClientHttpResponse {
        val tag = "[$clientName#${HttpExchangeMethodContext.get() ?: "unknown"}]"

        // Request logging
        log("$tag ---> ${request.method} ${request.uri}")
        if (level == Level.FULL && body.isNotEmpty()) log("$tag ${body.decodeToString()}")
        log("$tag ---> END (${body.size}-byte body)")

        // Execute and measure
        val startTime = System.currentTimeMillis()
        val response = BufferingClientHttpResponseWrapper(execution.execute(request, body))
        val elapsedTime = System.currentTimeMillis() - startTime

        // Response logging
        log("$tag <--- ${response.statusCode} (${elapsedTime}ms)")
        val responseBody = response.bodyAsString
        if (level == Level.FULL && responseBody.isNotEmpty()) log("$tag $responseBody")
        log("$tag <--- END (${responseBody.length}-byte body)")

        return response
    }
}
```

### Log Output

**SIMPLE (default):**
```
[TodoClient#findAll] ---> GET https://jsonplaceholder.typicode.com/todos
[TodoClient#findAll] ---> END (0-byte body)
[TodoClient#findAll] <--- 200 OK (234ms)
[TodoClient#findAll] <--- END (5843-byte body)
```

**FULL:**
```
[TodoClient#create] ---> POST https://jsonplaceholder.typicode.com/todos
[TodoClient#create] {"title":"New Todo","completed":false}
[TodoClient#create] ---> END (42-byte body)
[TodoClient#create] <--- 201 Created (156ms)
[TodoClient#create] {"id":201,"title":"New Todo","completed":false}
[TodoClient#create] <--- END (51-byte body)
```

## Usage Patterns

### Query Parameters

Use `@RequestParam` to send query parameters:

```kotlin
@HttpExchange("/api/search")
interface SearchApiClient {
    @GetExchange
    fun search(@RequestParam query: String,
               @RequestParam page: Int = 0,
               @RequestParam size: Int = 20): SearchResultDto
}

// Usage: /api/search?query=kotlin&page=0&size=10
val result = searchApiClient.search(query = "kotlin", page = 0, size = 10)
```

### Headers

Use `@RequestHeader` to send custom headers:

```kotlin
@HttpExchange("/api/secure")
interface SecureApiClient {
    @GetExchange("/resource")
    fun getResource(@RequestHeader("Authorization") token: String): ResourceDto
}
```

### Error Handling

Handle HTTP client errors with try-catch blocks:

```kotlin
@Service
class PaymentService(private val paymentApiClient: PaymentApiClient) {
    fun processPayment(request: PaymentRequest): PaymentResult {
        return try {
            PaymentResult.success(paymentApiClient.process(request))
        } catch (e: HttpClientErrorException) {
            when (e.statusCode.value()) {
                400 -> throw InvalidPaymentRequestException(e.message)
                404 -> throw PaymentNotFoundException(e.message)
                else -> throw PaymentApiException("Client error: ${e.message}", e)
            }
        } catch (e: HttpServerErrorException) {
            throw PaymentApiException("Server error: ${e.message}", e)
        } catch (e: ResourceAccessException) {
            throw PaymentApiException("Network error: ${e.message}", e)
        }
    }
}
```

### Async Calls

Support coroutines and reactive types:

```kotlin
@HttpExchange("/api/async")
interface AsyncApiClient {
    @GetExchange
    suspend fun getData(): DataDto  // Coroutine

    @GetExchange
    fun getDataMono(): Mono<DataDto>  // Reactor
}
```

## Advanced Configuration

### Timeout

Configure connection and read timeouts:

```kotlin
@Bean
fun customClientGroupConfigurer(): RestClientHttpServiceGroupConfigurer {
    return RestClientHttpServiceGroupConfigurer { groups ->
        groups.filterByName("payment")
            .forEachClient { _, builder ->
                builder
                    .baseUrl("https://payment-api.example.com")
                    .requestFactory(SimpleClientHttpRequestFactory().apply {
                        setConnectTimeout(Duration.ofSeconds(5))
                        setReadTimeout(Duration.ofSeconds(30))
                    })
                    .requestInterceptor(HttpLoggingInterceptor("PaymentClient", Level.FULL))
            }
    }
}
```

**Recommended:** Set `connectTimeout` to 5 seconds and `readTimeout` to 30 seconds.

### Retry

Implement retry logic using a custom interceptor:

```kotlin
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val retryDelay: Duration = Duration.ofMillis(500)
) : ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray,
                          execution: ClientHttpRequestExecution): ClientHttpResponse {
        var attempt = 0
        var lastException: Exception? = null

        while (attempt < maxRetries) {
            try {
                return execution.execute(request, body)
            } catch (e: Exception) {
                lastException = e
                attempt++
                if (attempt < maxRetries) Thread.sleep(retryDelay.toMillis() * attempt)
            }
        }
        throw lastException ?: IllegalStateException("Retry failed")
    }
}
```

### Authentication

Add authentication headers using a custom interceptor:

```kotlin
class AuthInterceptor(private val tokenProvider: () -> String) : ClientHttpRequestInterceptor {
    override fun intercept(request: HttpRequest, body: ByteArray,
                          execution: ClientHttpRequestExecution): ClientHttpResponse {
        request.headers.setBearerAuth(tokenProvider())
        return execution.execute(request, body)
    }
}

@Bean
fun authClientGroupConfigurer(tokenService: TokenService): RestClientHttpServiceGroupConfigurer {
    return RestClientHttpServiceGroupConfigurer { groups ->
        groups.filterByName("auth-required")
            .forEachClient { _, builder ->
                builder
                    .baseUrl("https://secure-api.example.com")
                    .requestInterceptor(AuthInterceptor { tokenService.getAccessToken() })
            }
    }
}
```

### Multi-Client

Configure multiple client groups with different settings:

```kotlin
@Configuration
@ImportHttpServices(group = "payment", types = [PaymentClient::class, RefundClient::class])
@ImportHttpServices(group = "notification", types = [EmailClient::class, SmsClient::class])
class ExternalApiConfig {

    @Bean
    fun paymentClientGroupConfigurer(): RestClientHttpServiceGroupConfigurer {
        return RestClientHttpServiceGroupConfigurer { groups ->
            groups.filterByName("payment")
                .forEachClient { _, builder ->
                    builder
                        .baseUrl("https://payment.example.com")
                        .requestInterceptor(HttpLoggingInterceptor("Payment"))
                        .defaultHeader("X-Api-Key", "payment-api-key")
                }
        }
    }

    @Bean
    fun notificationClientGroupConfigurer(): RestClientHttpServiceGroupConfigurer {
        return RestClientHttpServiceGroupConfigurer { groups ->
            groups.filterByName("notification")
                .forEachClient { _, builder ->
                    builder
                        .baseUrl("https://notification.example.com")
                        .requestInterceptor(HttpLoggingInterceptor("Notification"))
                        .defaultHeader("X-Api-Key", "notification-api-key")
                }
        }
    }
}
```

## Testing

### MockWebServer

Use MockWebServer for integration testing:

```kotlin
@SpringBootTest
class TodoClientTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var todoClient: TodoClient

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer().apply { start() }
        val restClient = RestClient.builder().baseUrl(mockWebServer.url("/").toString()).build()
        val factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build()
        todoClient = factory.createClient(TodoClient::class.java)
    }

    @AfterEach
    fun tearDown() = mockWebServer.shutdown()

    @Test
    fun `should get all todos`() {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""[{"id": 1, "title": "Task 1"}]""")
            .addHeader("Content-Type", "application/json"))

        val todos = todoClient.findAll()

        assertThat(todos).hasSize(1)
        assertThat(todos[0].title).isEqualTo("Task 1")
    }
}
```

### WireMock

Use WireMock for declarative HTTP stubbing:

```kotlin
@SpringBootTest
@AutoConfigureWireMock(port = 0)
class PaymentClientTest {
    @Autowired
    private lateinit var paymentClient: PaymentClient

    @Test
    fun `should process payment successfully`() {
        stubFor(post(urlEqualTo("/api/payments"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""{"id": 123, "status": "COMPLETED"}""")))

        val result = paymentClient.process(PaymentRequest(amount = 10000, currency = "KRW"))

        assertThat(result.id).isEqualTo(123)
        assertThat(result.status).isEqualTo("COMPLETED")
    }
}
```

## Best Practices

### DO: Clear Naming

Use descriptive names for HTTP Interface classes:

```kotlin
// GOOD
interface PaymentApiClient { }
interface UserApiClient { }

// BAD
interface ApiClient { }
```

### DO: Domain-Based Structure

Organize clients by domain:

```
infrastructure/client/
  ├── payment/
  │   ├── PaymentApiClient.kt
  │   ├── PaymentClientConfig.kt
  │   └── dto/
  ├── notification/
  └── user/
```

### DO: Separate DTOs

Keep external API DTOs separate from internal domain DTOs:

```kotlin
// External API DTO
data class ExternalUserDto(val id: String, val name: String, val email: String)

// Internal domain DTO
data class UserInfo(val userId: Long, val userName: String, val email: Email)

// Conversion
fun ExternalUserDto.toDomain() = UserInfo(
    userId = id.toLong(),
    userName = name,
    email = Email.of(email)
)
```

### DO: Handle Errors

Handle HTTP client errors explicitly:

```kotlin
@Service
class ExternalApiService(private val externalClient: ExternalApiClient) {
    fun getData(id: String): DataDto {
        return try {
            externalClient.getData(id)
        } catch (e: HttpClientErrorException.NotFound) {
            throw DataNotFoundException("External data not found: $id")
        } catch (e: HttpClientErrorException) {
            throw ExternalApiException("Client error: ${e.statusCode}", e)
        } catch (e: HttpServerErrorException) {
            throw ExternalApiException("Server error: ${e.statusCode}", e)
        } catch (e: ResourceAccessException) {
            throw ExternalApiException("Connection failed: ${e.message}", e)
        }
    }
}
```

### DON'T: Log Sensitive Data

Avoid logging sensitive information in request/response bodies:

```kotlin
// BAD - exposes passwords
@PostExchange("/login")
fun login(@RequestBody credentials: Credentials): TokenResponse

// GOOD - use SIMPLE level or custom interceptor to mask sensitive fields
```

### DON'T: Abuse Synchronous Calls

Use parallel execution for independent API calls:

```kotlin
// BAD - sequential
fun getUserWithDetails(userId: Long): UserWithDetails {
    val user = userApiClient.getUser(userId)
    val orders = orderApiClient.getOrders(userId)
    return UserWithDetails(user, orders)
}

// GOOD - parallel
suspend fun getUserWithDetails(userId: Long): UserWithDetails = coroutineScope {
    val user = async { userApiClient.getUser(userId) }
    val orders = async { orderApiClient.getOrders(userId) }
    UserWithDetails(user.await(), orders.await())
}
```

## Summary

### Core Principles

1. Use declarative HTTP Interface for type-safe, concise implementation
2. Group clients with `@ImportHttpServices` for easy configuration
3. Add `HttpLoggingInterceptor` for debugging and monitoring
4. Set timeout and retry per client for stability
5. Package clients by domain for maintainability

### Checklist

- [ ] `@HttpExchange` interface defined
- [ ] Bean registered with `@ImportHttpServices`
- [ ] baseUrl configured
- [ ] `HttpLoggingInterceptor` added
- [ ] Timeout set
- [ ] Error handling implemented
- [ ] Sensitive data not logged

### Related Documents

- `07-error-handling.md`: HTTP client error handling
- `91_project-modules.md`: Infrastructure module
- [Spring HTTP Interface](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-interface)
