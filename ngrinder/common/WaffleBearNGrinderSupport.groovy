import static net.grinder.script.Grinder.grinder

import HTTPClient.HTTPResponse
import HTTPClient.NVPair
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import net.grinder.plugin.http.HTTPRequest

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class WaffleBearNGrinderSupport {

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

    static String buildBaseUrl() {
        String fullBaseUrl = property("baseUrl", null)
        if (fullBaseUrl != null && !fullBaseUrl.trim().isEmpty()) {
            String normalized = fullBaseUrl.trim()
            if (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1)
            }
            return normalized
        }

        String scheme = property("target.scheme", "http")
        String host = property("target.host", "localhost")
        String port = property("target.port", "8080")
        String contextPath = property("target.contextPath", "")

        String normalizedContextPath = contextPath == null ? "" : contextPath.trim()
        if (!normalizedContextPath.isEmpty() && !normalizedContextPath.startsWith("/")) {
            normalizedContextPath = "/" + normalizedContextPath
        }
        if (normalizedContextPath.endsWith("/")) {
            normalizedContextPath = normalizedContextPath.substring(0, normalizedContextPath.length() - 1)
        }

        return "${scheme}://${host}:${port}${normalizedContextPath}"
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

    static class ApiResult {
        final int statusCode
        final String body
        final Object json
        final HTTPResponse raw

        ApiResult(int statusCode, String body, Object json, HTTPResponse raw) {
            this.statusCode = statusCode
            this.body = body
            this.json = json
            this.raw = raw
        }

        boolean ok() {
            return statusCode >= 200 && statusCode < 300
        }
    }

    static class Session {
        private static final Pattern REFRESH_COOKIE_PATTERN = Pattern.compile("(?i)(?:^|;\\s*)refresh=([^;]+)")

        private final HTTPRequest request
        private final JsonSlurper jsonSlurper = new JsonSlurper()
        private final String baseUrl

        private String accessToken
        private String refreshCookie

        private final String providedToken
        private final String loginEmail
        private final String loginPassword
        private final String preferredLoginPath

        Session(HTTPRequest request, String baseUrl) {
            this.request = request
            this.baseUrl = baseUrl
            this.providedToken = sanitizeToken(WaffleBearNGrinderSupport.property("auth.token", null))
            this.loginEmail = WaffleBearNGrinderSupport.property(
                    "auth.email",
                    WaffleBearNGrinderSupport.property("loginEmail", DEFAULT_ADMIN_EMAIL)
            )
            this.loginPassword = WaffleBearNGrinderSupport.property(
                    "auth.password",
                    WaffleBearNGrinderSupport.property("loginPassword", DEFAULT_ADMIN_PASSWORD)
            )
            this.preferredLoginPath = WaffleBearNGrinderSupport.property("auth.loginPath", "/login")
        }

        ApiResult get(String path, Map query = [:]) {
            return send("GET", path, null, query, true)
        }

        ApiResult post(String path, Object body = null, Map query = [:]) {
            return send("POST", path, body, query, true)
        }

        ApiResult put(String path, Object body = null, Map query = [:]) {
            return send("PUT", path, body, query, true)
        }

        ApiResult delete(String path, Object body = null, Map query = [:]) {
            return send("DELETE", path, body, query, true)
        }

        ApiResult putRaw(String absoluteUrl, byte[] bodyBytes, String contentType = "application/octet-stream") {
            NVPair[] headers = [
                    new NVPair("Content-Type", contentType)
            ] as NVPair[]

            request.setHeaders(headers)
            HTTPResponse response = bodyBytes == null ? request.PUT(absoluteUrl) : request.PUT(absoluteUrl, bodyBytes)
            Object parsed = parseJsonSafely(response?.getText())
            return new ApiResult(response?.getStatusCode() ?: 0, response?.getText(), parsed, response)
        }

        void ensureAuthenticated() {
            if (accessToken != null && !accessToken.isEmpty()) {
                return
            }

            if (providedToken != null && !providedToken.isEmpty()) {
                accessToken = providedToken
                return
            }

            login()
        }

        private ApiResult send(String method, String path, Object body, Map query, boolean allowReissueRetry) {
            ensureAuthenticated()
            String url = buildUrl(path, query)

            String payload = body == null ? null : JsonOutput.toJson(body)
            NVPair[] headers = buildAuthHeaders()
            HTTPResponse response = execute(method, url, payload, headers)

            captureAuthDataFromHeaders(response)

            int statusCode = response?.getStatusCode() ?: 0
            if ((statusCode == 401 || statusCode == 403) && allowReissueRetry && tryReissue()) {
                return send(method, path, body, query, false)
            }

            Object parsed = parseJsonSafely(response?.getText())
            return new ApiResult(statusCode, response?.getText(), parsed, response)
        }

        private boolean login() {
            if (loginEmail == null || loginPassword == null) {
                throw new IllegalStateException("Set auth.token or both auth.email/auth.password in nGrinder properties.")
            }

            List<String> loginPaths = []
            if (preferredLoginPath != null && !preferredLoginPath.trim().isEmpty()) {
                loginPaths.add(preferredLoginPath.trim())
            }
            if (!loginPaths.contains("/login")) {
                loginPaths.add("/login")
            }
            if (!loginPaths.contains("/api/login")) {
                loginPaths.add("/api/login")
            }

            ApiResult lastLoginResult = null
            for (String loginPath : loginPaths) {
                ApiResult loginResult = sendAnonymous(
                        "POST",
                        loginPath,
                        [email: loginEmail, password: loginPassword],
                        [:],
                        [:]
                )

                lastLoginResult = loginResult
                if (!loginResult.ok()) {
                    // Some deployments only expose one of /login or /api/login.
                    if (loginResult.statusCode == 404 || loginResult.statusCode == 405) {
                        continue
                    }
                    throw new IllegalStateException("Login failed at ${loginPath}. status=${loginResult.statusCode}, body=${loginResult.body}")
                }

                String tokenFromBody = null
                if (loginResult.json instanceof Map) {
                    Object raw = ((Map) loginResult.json).get("accessToken")
                    tokenFromBody = raw == null ? null : raw.toString()
                }

                if ((tokenFromBody == null || tokenFromBody.isEmpty()) && loginResult.raw != null) {
                    String authHeader = loginResult.raw.getHeader("Authorization")
                    tokenFromBody = authHeader
                }

                this.accessToken = sanitizeToken(tokenFromBody)
                if (this.accessToken == null || this.accessToken.isEmpty()) {
                    throw new IllegalStateException("Login succeeded at ${loginPath} but no access token was found in response.")
                }
                grinder.logger.info("Authenticated by {} for {}", loginPath, loginEmail)
                return true
            }

            if (lastLoginResult == null) {
                throw new IllegalStateException("Login failed: no login path configured.")
            }
            throw new IllegalStateException("Login failed. lastStatus=${lastLoginResult.statusCode}, body=${lastLoginResult.body}")
        }

        private boolean tryReissue() {
            if (refreshCookie == null || refreshCookie.isEmpty()) {
                return false
            }

            ApiResult reissueResult = sendAnonymous(
                    "POST",
                    "/auth/reissue",
                    null,
                    [:],
                    ["Cookie": "refresh=${refreshCookie}"]
            )

            if (!reissueResult.ok()) {
                return false
            }

            String authHeader = reissueResult.raw == null ? null : reissueResult.raw.getHeader("Authorization")
            String refreshedAccessToken = sanitizeToken(authHeader)
            if (refreshedAccessToken == null || refreshedAccessToken.isEmpty()) {
                return false
            }

            this.accessToken = refreshedAccessToken
            return true
        }

        private ApiResult sendAnonymous(String method, String path, Object body, Map query, Map extraHeaders) {
            String url = buildUrl(path, query)
            String payload = body == null ? null : JsonOutput.toJson(body)

            List<NVPair> headers = [
                    new NVPair("Accept", "application/json"),
                    new NVPair("Content-Type", "application/json")
            ]
            extraHeaders.each { k, v ->
                if (k != null && v != null) {
                    headers.add(new NVPair(k.toString(), v.toString()))
                }
            }

            HTTPResponse response = execute(method, url, payload, headers as NVPair[])
            captureAuthDataFromHeaders(response)
            Object parsed = parseJsonSafely(response?.getText())
            return new ApiResult(response?.getStatusCode() ?: 0, response?.getText(), parsed, response)
        }

        private NVPair[] buildAuthHeaders() {
            List<NVPair> headers = [
                    new NVPair("Accept", "application/json"),
                    new NVPair("Content-Type", "application/json"),
                    new NVPair("Authorization", "Bearer ${accessToken}")
            ]
            if (refreshCookie != null && !refreshCookie.isEmpty()) {
                headers.add(new NVPair("Cookie", "refresh=${refreshCookie}"))
            }
            return headers as NVPair[]
        }

        private String buildUrl(String path, Map query) {
            if (path == null || path.trim().isEmpty()) {
                throw new IllegalArgumentException("Request path must not be empty.")
            }

            String normalizedPath = path.trim()
            String base = normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")
                    ? normalizedPath
                    : baseUrl + (normalizedPath.startsWith("/") ? normalizedPath : "/" + normalizedPath)

            if (query == null || query.isEmpty()) {
                return base
            }

            List<String> queryParts = []
            query.each { key, value ->
                if (key != null && value != null) {
                    String encodedKey = URLEncoder.encode(key.toString(), StandardCharsets.UTF_8.name())
                    String encodedValue = URLEncoder.encode(value.toString(), StandardCharsets.UTF_8.name())
                    queryParts.add(encodedKey + "=" + encodedValue)
                }
            }

            if (queryParts.isEmpty()) {
                return base
            }
            return base + (base.contains("?") ? "&" : "?") + queryParts.join("&")
        }

        private HTTPResponse execute(String method, String url, String payload, NVPair[] headers) {
            request.setHeaders(headers)

            String normalizedMethod = method == null ? "GET" : method.toUpperCase(Locale.ROOT)
            byte[] payloadBytes = payload == null ? new byte[0] : payload.getBytes(StandardCharsets.UTF_8)

            switch (normalizedMethod) {
                case "GET":
                    return request.GET(url)
                case "POST":
                    return request.POST(url, payloadBytes)
                case "PUT":
                    return request.PUT(url, payloadBytes)
                case "DELETE":
                    if (payload != null && !payload.isEmpty()) {
                        try {
                            return request.DELETE(url, payloadBytes)
                        } catch (MissingMethodException ignored) {
                            grinder.logger.warn("DELETE with body is not supported by current HTTPRequest implementation. Fallback to DELETE without body.")
                        }
                    }
                    return request.DELETE(url)
                default:
                    throw new UnsupportedOperationException("Unsupported HTTP method for this helper: ${normalizedMethod}")
            }
        }

        private Object parseJsonSafely(String text) {
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
            try {
                return jsonSlurper.parseText(trimmed)
            } catch (Exception ignored) {
                return null
            }
        }

        private void captureAuthDataFromHeaders(HTTPResponse response) {
            if (response == null) {
                return
            }

            String authorizationHeader = response.getHeader("Authorization")
            String parsedAccessToken = sanitizeToken(authorizationHeader)
            if (parsedAccessToken != null && !parsedAccessToken.isEmpty()) {
                this.accessToken = parsedAccessToken
            }

            String setCookie = response.getHeader("Set-Cookie")
            if (setCookie != null) {
                String parsedRefreshCookie = extractRefreshCookie(setCookie)
                if (parsedRefreshCookie != null && !parsedRefreshCookie.isEmpty()) {
                    this.refreshCookie = parsedRefreshCookie
                }
            }
        }

        private String sanitizeToken(String rawToken) {
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

        private String extractRefreshCookie(String setCookieHeader) {
            if (setCookieHeader == null || setCookieHeader.isEmpty()) {
                return null
            }

            def matcher = REFRESH_COOKIE_PATTERN.matcher(setCookieHeader)
            if (matcher.find()) {
                return matcher.group(1)
            }
            return null
        }
    }
}


