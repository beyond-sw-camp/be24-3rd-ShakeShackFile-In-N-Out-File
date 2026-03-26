import static net.grinder.script.Grinder.grinder

import HTTPClient.HTTPResponse
import HTTPClient.NVPair
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import net.grinder.plugin.http.HTTPPluginControl
import net.grinder.plugin.http.HTTPRequest
import net.grinder.script.GTest
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import java.nio.charset.StandardCharsets
import java.util.Locale

@RunWith(GrinderRunner)
class WaffleBearNotificationScenario {

    public static final String DEFAULT_BASE_URL = "http://192.100.1.10:8080"
    public static final String DEFAULT_LOGIN_EMAIL = "administrator@administrator.adm"
    public static final String DEFAULT_LOGIN_PASSWORD = "fweiuhfge2232n12@#xSD23@"
    public static final int DEFAULT_TIMEOUT_MS = 120000
    public static final boolean DEFAULT_SKIP_NETWORK_ON_VALIDATE = true

    public static GTest loginTest
    public static GTest endpointTest

    public static HTTPRequest loginRequest
    public static HTTPRequest endpointRequest

    public static JsonSlurper jsonSlurper = new JsonSlurper()
    public static String baseUrl

    String accessToken
    boolean skipNetwork

    @BeforeProcess
    static void beforeProcess() {
        HTTPPluginControl.getConnectionDefaults().timeout = httpTimeoutMillis()
        HTTPPluginControl.getConnectionDefaults().followRedirects = true

        baseUrl = normalizeBaseUrl(property("baseUrl", DEFAULT_BASE_URL))

        loginTest = new GTest(1, "notification-login")
        endpointTest = new GTest(2, "notification-scenario")

        loginRequest = new HTTPRequest()
        endpointRequest = new HTTPRequest()

        loginTest.record(loginRequest)
        endpointTest.record(endpointRequest)
    }

    @BeforeThread
    void beforeThread() {
        grinder.statistics.delayReports = true
    }

    @Before
    void before() {
        skipNetwork = shouldSkipNetworkOnValidation()
        if (skipNetwork) {
            grinder.logger.info("Skipping network calls in validation context.")
            return
        }
        accessToken = loginAsAdministrator()
    }

    @Test
    void runScenario() {
        if (skipNetwork) {
            return
        }

        HTTPResponse listResponse = get(endpointRequest, "/notification/list")
        assertStatus(listResponse, "GET /notification/list", [200])
    }

    @Test
    void testNotificationListRepeated() {
        if (skipNetwork) {
            return
        }

        HTTPResponse first = get(endpointRequest, "/notification/list")
        assertStatus(first, "GET /notification/list (first)", [200])

        HTTPResponse second = get(endpointRequest, "/notification/list")
        assertStatus(second, "GET /notification/list (second)", [200])
    }

    @Test
    void testNotificationListBodyParse() {
        if (skipNetwork) {
            return
        }

        HTTPResponse response = get(endpointRequest, "/notification/list")
        assertStatus(response, "GET /notification/list (parse)", [200])

        Object parsed = parseJson(response.getText())
        Object body = unwrapApiBody(parsed)
        if (body == null) {
            throw new IllegalStateException("Notification list body is null.")
        }
    }

    private String loginAsAdministrator() {
        String adminEmail = property("loginEmail", property("auth.email", DEFAULT_LOGIN_EMAIL))
        String adminPassword = property("loginPassword", property("auth.password", DEFAULT_LOGIN_PASSWORD))

        Map payload = [
                email   : adminEmail,
                password: adminPassword
        ]

        HTTPResponse loginResponse = postAnonymous(loginRequest, "/login", payload)
        int statusCode = loginResponse.getStatusCode()
        String loginStep = "POST /login"

        if (statusCode == 404 || statusCode == 405) {
            loginResponse = postAnonymous(loginRequest, "/api/login", payload)
            loginStep = "POST /api/login"
        }

        assertStatus(loginResponse, loginStep, [200])

        Object parsed = parseJson(loginResponse.getText())
        String token = null
        if (parsed instanceof Map) {
            token = ((Map) parsed).get("accessToken")?.toString()
        }
        if (!token) {
            token = loginResponse.getHeader("Authorization")
        }

        token = sanitizeToken(token)
        if (!token) {
            throw new IllegalStateException("Login succeeded but accessToken was not found.")
        }

        return token
    }

    private HTTPResponse get(HTTPRequest request, String path) {
        request.setHeaders(authJsonHeaders())
        return request.GET(buildUrl(path))
    }

    private HTTPResponse postAnonymous(HTTPRequest request, String path, Object body) {
        request.setHeaders(jsonHeaders())
        byte[] payload = body == null
                ? new byte[0]
                : JsonOutput.toJson(body).getBytes(StandardCharsets.UTF_8)
        return request.POST(buildUrl(path), payload)
    }

    private static NVPair[] jsonHeaders() {
        return [
                new NVPair("Content-Type", "application/json"),
                new NVPair("Accept", "application/json")
        ] as NVPair[]
    }

    private NVPair[] authJsonHeaders() {
        return [
                new NVPair("Content-Type", "application/json"),
                new NVPair("Accept", "application/json"),
                new NVPair("Authorization", "Bearer ${accessToken}")
        ] as NVPair[]
    }

    private String buildUrl(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Request path must not be empty.")
        }

        String normalized = path.trim()
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized
        }
        return baseUrl + (normalized.startsWith("/") ? normalized : "/" + normalized)
    }

    private static String normalizeBaseUrl(String rawBaseUrl) {
        String normalized = (rawBaseUrl == null || rawBaseUrl.trim().isEmpty())
                ? DEFAULT_BASE_URL
                : rawBaseUrl.trim()
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1)
        }
        return normalized
    }

    private static int httpTimeoutMillis() {
        String raw = property("http.timeout.ms", String.valueOf(DEFAULT_TIMEOUT_MS))
        try {
            int value = Integer.parseInt(raw)
            return value > 0 ? value : DEFAULT_TIMEOUT_MS
        } catch (Exception ignored) {
            return DEFAULT_TIMEOUT_MS
        }
    }

    private static boolean shouldSkipNetworkOnValidation() {
        String context = System.getProperty("ngrinder.context", "")
        String defaultRaw = DEFAULT_SKIP_NETWORK_ON_VALIDATE ? "true" : "false"
        String skipRaw = property("validate.skipNetworkOnController", defaultRaw)
        boolean skipEnabled = "true".equalsIgnoreCase(skipRaw)
        return skipEnabled && "controller".equalsIgnoreCase(context)
    }

    private static String property(String key, String defaultValue = null) {
        String fromGrinder = grinder?.properties?.getProperty(key)
        if (fromGrinder != null && !fromGrinder.trim().isEmpty()) {
            return fromGrinder.trim()
        }

        String fromSystem = System.getProperty(key)
        if (fromSystem != null && !fromSystem.trim().isEmpty()) {
            return fromSystem.trim()
        }

        String envKey = key.toUpperCase(Locale.ROOT).replace('.', '_')
        String fromEnv = System.getenv(envKey)
        if (fromEnv != null && !fromEnv.trim().isEmpty()) {
            return fromEnv.trim()
        }

        return defaultValue
    }

    private static void assertStatus(HTTPResponse response, String step, List<Integer> allowedStatuses) {
        if (response == null) {
            throw new IllegalStateException("${step} failed. response is null")
        }

        int statusCode = response.getStatusCode()
        if (!allowedStatuses.contains(statusCode)) {
            grinder.logger.error("{} failed. status={}, body={}", step, statusCode, response.getText())
            throw new IllegalStateException("${step} failed with status ${statusCode}")
        }
    }

    private static Object parseJson(String text) {
        if (text == null) {
            return null
        }
        String trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            return null
        }
        return jsonSlurper.parseText(trimmed)
    }

    private static Object unwrapApiBody(Object parsedJson) {
        if (!(parsedJson instanceof Map)) {
            return parsedJson
        }

        Object rootResult = ((Map) parsedJson).get("result")
        if (rootResult instanceof Map && ((Map) rootResult).containsKey("body")) {
            return ((Map) rootResult).get("body")
        }
        return rootResult != null ? rootResult : parsedJson
    }

    private static String sanitizeToken(String rawToken) {
        if (rawToken == null) {
            return null
        }

        String normalized = rawToken.trim()
        if (normalized.isEmpty()) {
            return null
        }
        if (normalized.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            normalized = normalized.substring(7).trim()
        }
        return normalized.isEmpty() ? null : normalized
    }
}
