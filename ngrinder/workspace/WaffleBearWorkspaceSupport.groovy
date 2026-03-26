import static net.grinder.script.Grinder.grinder

import HTTPClient.HTTPResponse
import HTTPClient.NVPair
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import net.grinder.plugin.http.HTTPRequest

import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID

class WaffleBearWorkspaceSupport {

    static final JsonSlurper JSON_SLURPER = new JsonSlurper()
    static final String DEFAULT_BASE_URL = "http://192.100.1.10:8080"
    static final String DEFAULT_ADMIN_EMAIL = "administrator@administrator.adm"
    static final String DEFAULT_ADMIN_PASSWORD = "fweiuhfge2232n12@#xSD23@"

    static String property(String key, String defaultValue = null) {
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

    static String baseUrl() {
        String fullBaseUrl = property("baseUrl", DEFAULT_BASE_URL)
        if (fullBaseUrl == null || fullBaseUrl.trim().isEmpty()) {
            fullBaseUrl = DEFAULT_BASE_URL
        }

        String normalized = fullBaseUrl.trim()
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1)
        }
        return normalized
    }

    static int httpTimeoutMillis() {
        String raw = property("http.timeout.ms", "120000")
        try {
            int value = Integer.parseInt(raw)
            return value > 0 ? value : 120000
        } catch (Exception ignored) {
            return 120000
        }
    }

    static boolean shouldSkipNetworkOnValidation() {
        String context = System.getProperty("ngrinder.context", "")
        String skipRaw = property("validate.skipNetworkOnController", "true")
        boolean skipEnabled = "true".equalsIgnoreCase(skipRaw)
        return skipEnabled && "controller".equalsIgnoreCase(context)
    }

    static String loginAsAdministrator(HTTPRequest request, String baseUrl) {
        String adminEmail = property("loginEmail", property("auth.email", DEFAULT_ADMIN_EMAIL))
        String adminPassword = property("loginPassword", property("auth.password", DEFAULT_ADMIN_PASSWORD))

        Map payload = [
                email   : adminEmail,
                password: adminPassword
        ]

        HTTPResponse loginResponse = postAnonymous(request, baseUrl, "/login", payload)
        int statusCode = loginResponse.getStatusCode()
        String loginStep = "POST /login"

        if (statusCode == 404 || statusCode == 405) {
            loginResponse = postAnonymous(request, baseUrl, "/api/login", payload)
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

    static HTTPResponse get(HTTPRequest request, String baseUrl, String accessToken, String path) {
        request.setHeaders(authJsonHeaders(accessToken))
        return request.GET(buildUrl(baseUrl, path))
    }

    static HTTPResponse post(HTTPRequest request, String baseUrl, String accessToken, String path, Object body) {
        request.setHeaders(authJsonHeaders(accessToken))
        byte[] payload = body == null
                ? new byte[0]
                : JsonOutput.toJson(body).getBytes(StandardCharsets.UTF_8)
        return request.POST(buildUrl(baseUrl, path), payload)
    }

    static HTTPResponse postAnonymous(HTTPRequest request, String baseUrl, String path, Object body) {
        request.setHeaders(jsonHeaders())
        byte[] payload = body == null
                ? new byte[0]
                : JsonOutput.toJson(body).getBytes(StandardCharsets.UTF_8)
        return request.POST(buildUrl(baseUrl, path), payload)
    }

    static Map createWorkspace(HTTPRequest request, String baseUrl, String accessToken, String tag) {
        String suffix = UUID.randomUUID().toString().substring(0, 8)
        Map payload = [
                idx     : null,
                title   : "ngrinder-ws-${tag}-${suffix}",
                contents: "workspace scenario ${tag} @ " + System.currentTimeMillis()
        ]

        HTTPResponse saveResponse = post(request, baseUrl, accessToken, "/workspace/save", payload)
        assertStatus(saveResponse, "POST /workspace/save", [200])

        Object parsed = parseJson(saveResponse.getText())
        Object unwrapped = unwrapApiBody(parsed)
        if (!(unwrapped instanceof Map)) {
            throw new IllegalStateException("Workspace save response body is invalid: " + saveResponse.getText())
        }

        Long idx = asLong(((Map) unwrapped).get("idx"))
        String uuid = ((Map) unwrapped).get("uuid")?.toString()
        if (idx == null) {
            throw new IllegalStateException("Workspace idx was not found in save response: " + saveResponse.getText())
        }

        return [idx: idx, uuid: uuid]
    }

    static void deleteWorkspaceSilently(HTTPRequest request, String baseUrl, String accessToken, Long workspaceIdx) {
        if (workspaceIdx == null) {
            return
        }

        try {
            HTTPResponse response = post(request, baseUrl, accessToken, "/workspace/delete/${workspaceIdx}", null)
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                grinder.logger.warn("workspace cleanup failed. idx={}, status={}, body={}", workspaceIdx, response.getStatusCode(), response.getText())
            }
        } catch (Exception e) {
            grinder.logger.warn("workspace cleanup exception. idx={}, message={}", workspaceIdx, e.getMessage())
        }
    }

    static String buildUrl(String baseUrl, String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Request path must not be empty.")
        }

        String normalized = path.trim()
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized
        }
        return baseUrl + (normalized.startsWith("/") ? normalized : "/" + normalized)
    }

    static NVPair[] jsonHeaders() {
        return [
                new NVPair("Content-Type", "application/json"),
                new NVPair("Accept", "application/json")
        ] as NVPair[]
    }

    static NVPair[] authJsonHeaders(String accessToken) {
        return [
                new NVPair("Content-Type", "application/json"),
                new NVPair("Accept", "application/json"),
                new NVPair("Authorization", "Bearer ${accessToken}")
        ] as NVPair[]
    }

    static void assertStatus(HTTPResponse response, String step, List<Integer> allowedStatuses) {
        if (response == null) {
            throw new IllegalStateException("${step} failed. response is null")
        }

        int statusCode = response.getStatusCode()
        if (!allowedStatuses.contains(statusCode)) {
            grinder.logger.error("{} failed. status={}, body={}", step, statusCode, response.getText())
            throw new IllegalStateException("${step} failed with status ${statusCode}")
        }
    }

    static Object parseJson(String text) {
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

        return JSON_SLURPER.parseText(trimmed)
    }

    static Object unwrapApiBody(Object parsedJson) {
        if (!(parsedJson instanceof Map)) {
            return parsedJson
        }

        Object rootResult = ((Map) parsedJson).get("result")
        if (rootResult instanceof Map && ((Map) rootResult).containsKey("body")) {
            return ((Map) rootResult).get("body")
        }
        return rootResult != null ? rootResult : parsedJson
    }

    static Long asLong(Object value) {
        if (value == null) {
            return null
        }
        if (value instanceof Number) {
            return ((Number) value).longValue()
        }
        try {
            return Long.parseLong(value.toString())
        } catch (Exception ignored) {
            return null
        }
    }

    static String sanitizeToken(String rawToken) {
        if (!rawToken) {
            return null
        }

        String normalized = rawToken.trim()
        if (normalized.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            normalized = normalized.substring(7).trim()
        }

        return normalized.isEmpty() ? null : normalized
    }
}
