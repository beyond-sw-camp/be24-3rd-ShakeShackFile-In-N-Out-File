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
import java.util.Arrays
import java.util.Locale
import java.util.UUID

@RunWith(GrinderRunner)
class WaffleBearFileAdvancedScenario {

    public static final String DEFAULT_BASE_URL = "http://192.100.1.10:8080"
    public static final String DEFAULT_LOGIN_EMAIL = "administrator@administrator.adm"
    public static final String DEFAULT_LOGIN_PASSWORD = "fweiuhfge2232n12@#xSD23@"
    public static final int DEFAULT_TIMEOUT_MS = 120000
    public static final boolean DEFAULT_SKIP_NETWORK_ON_VALIDATE = true
    public static final long CHUNK_SIZE_BYTES = 80L * 1024L * 1024L

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

        loginTest = new GTest(1, "file-advanced-login")
        endpointTest = new GTest(2, "file-advanced-endpoint")

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
    void testFileListAndFolderCreate() {
        if (skipNetwork) {
            return
        }

        HTTPResponse listBefore = get(endpointRequest, "/file/list")
        assertStatus(listBefore, "GET /file/list (before)", [200])

        String suffix = UUID.randomUUID().toString().substring(0, 8)
        HTTPResponse folderResponse = post(endpointRequest, "/file/folder", [folderName: "ngrinder-adv-folder-${suffix}", parentId: null])
        assertStatus(folderResponse, "POST /file/folder", [200])

        HTTPResponse listAfter = get(endpointRequest, "/file/list")
        assertStatus(listAfter, "GET /file/list (after)", [200])
    }

    @Test
    void testUploadCompleteFlow() {
        if (skipNetwork) {
            return
        }

        String suffix = UUID.randomUUID().toString().substring(0, 8)
        String fileName = "ngrinder-adv-${suffix}.txt"
        byte[] payload = ("advanced upload test @ " + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8)

        def initBody = [[
                fileOriginName: fileName,
                fileFormat    : "txt",
                fileSize      : payload.length,
                contentType   : "text/plain",
                parentId      : null,
                relativePath  : fileName,
                lastModified  : System.currentTimeMillis()
        ]]

        HTTPResponse initResponse = post(endpointRequest, "/file/upload", initBody)
        assertStatus(initResponse, "POST /file/upload", [200])

        List<Map> chunkList = normalizeChunkList(parseJson(initResponse.getText()))
        if (chunkList.isEmpty()) {
            throw new IllegalStateException("Upload init response did not return chunk metadata: " + initResponse.getText())
        }

        uploadChunks(chunkList, payload)

        Map firstChunk = chunkList[0]
        boolean partitioned = Boolean.TRUE.equals(firstChunk.partitioned) || chunkList.size() > 1
        List<String> chunkObjectKeys = partitioned
                ? chunkList.collect { it.objectKey as String }.findAll { it != null && !it.isEmpty() }
                : []

        def completeBody = [
                fileOriginName : fileName,
                fileFormat     : "txt",
                fileSize       : payload.length,
                finalObjectKey : (firstChunk.finalObjectKey ?: firstChunk.objectKey),
                chunkObjectKeys: chunkObjectKeys,
                parentId       : null,
                relativePath   : fileName,
                lastModified   : System.currentTimeMillis()
        ]
        HTTPResponse completeResponse = post(endpointRequest, "/file/upload/complete", completeBody)
        assertStatus(completeResponse, "POST /file/upload/complete", [200])
    }

    @Test
    void testUploadAbortFlow() {
        if (skipNetwork) {
            return
        }

        String suffix = UUID.randomUUID().toString().substring(0, 8)
        String fileName = "ngrinder-adv-abort-${suffix}.txt"
        byte[] payload = ("advanced abort test @ " + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8)

        def initBody = [[
                fileOriginName: fileName,
                fileFormat    : "txt",
                fileSize      : payload.length,
                contentType   : "text/plain",
                parentId      : null,
                relativePath  : fileName,
                lastModified  : System.currentTimeMillis()
        ]]

        HTTPResponse initResponse = post(endpointRequest, "/file/upload", initBody)
        assertStatus(initResponse, "POST /file/upload (abort case)", [200])

        List<Map> chunkList = normalizeChunkList(parseJson(initResponse.getText()))
        if (chunkList.isEmpty()) {
            throw new IllegalStateException("Abort init response did not return chunk metadata: " + initResponse.getText())
        }

        def abortBody = [
                finalObjectKey : (chunkList[0].finalObjectKey ?: chunkList[0].objectKey),
                chunkObjectKeys: chunkList.collect { it.objectKey as String }.findAll { it != null && !it.isEmpty() }
        ]

        HTTPResponse abortResponse = post(endpointRequest, "/file/upload/abort", abortBody)
        assertStatus(abortResponse, "POST /file/upload/abort", [200])
    }

    private void uploadChunks(List<Map> chunkList, byte[] payload) {
        for (int i = 0; i < chunkList.size(); i++) {
            Map chunk = chunkList[i]
            String uploadUrl = chunk.presignedUploadUrl == null ? null : chunk.presignedUploadUrl.toString()
            if (uploadUrl == null || uploadUrl.isEmpty()) {
                throw new IllegalStateException("Chunk upload url is missing: " + chunk)
            }

            int start = (int) Math.min(payload.length, i * CHUNK_SIZE_BYTES)
            int end = (int) Math.min(payload.length, start + CHUNK_SIZE_BYTES)
            byte[] chunkBytes = Arrays.copyOfRange(payload, start, end)

            HTTPResponse putResponse = putRaw(endpointRequest, uploadUrl, chunkBytes, "text/plain")
            assertStatus(putResponse, "PUT presigned part ${i + 1}", [200, 201, 204])
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

    private HTTPResponse get(HTTPRequest request, String path, Map query = [:]) {
        request.setHeaders(authJsonHeaders())
        return request.GET(buildUrl(path, query))
    }

    private HTTPResponse post(HTTPRequest request, String path, Object body) {
        request.setHeaders(authJsonHeaders())
        byte[] payload = body == null
                ? new byte[0]
                : JsonOutput.toJson(body).getBytes(StandardCharsets.UTF_8)
        return request.POST(buildUrl(path), payload)
    }

    private HTTPResponse putRaw(HTTPRequest request, String absoluteUrl, byte[] body, String contentType) {
        request.setHeaders([
                new NVPair("Content-Type", contentType)
        ] as NVPair[])

        byte[] payload = body == null ? new byte[0] : body
        return request.PUT(absoluteUrl, payload)
    }

    private HTTPResponse postAnonymous(HTTPRequest request, String path, Object body) {
        request.setHeaders(jsonHeaders())
        byte[] payload = body == null
                ? new byte[0]
                : JsonOutput.toJson(body).getBytes(StandardCharsets.UTF_8)
        return request.POST(buildUrl(path), payload)
    }

    private static List<Map> normalizeChunkList(Object rawJson) {
        Object unwrapped = unwrapApiBody(rawJson)

        if (unwrapped instanceof List) {
            return ((List) unwrapped).collect { it instanceof Map ? (Map) it : null }.findAll { it != null }
        }

        if (unwrapped instanceof Map) {
            Map mapBody = (Map) unwrapped
            Object data = mapBody.data
            Object result = mapBody.result

            if (data instanceof List) {
                return ((List) data).collect { it instanceof Map ? (Map) it : null }.findAll { it != null }
            }
            if (result instanceof List) {
                return ((List) result).collect { it instanceof Map ? (Map) it : null }.findAll { it != null }
            }
            if (data instanceof Map && ((Map) data).result instanceof List) {
                return ((List) ((Map) data).result).collect { it instanceof Map ? (Map) it : null }.findAll { it != null }
            }
        }

        return []
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

    private String buildUrl(String path, Map query = [:]) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Request path must not be empty.")
        }

        String normalized = path.trim()
        String base = normalized.startsWith("http://") || normalized.startsWith("https://")
                ? normalized
                : baseUrl + (normalized.startsWith("/") ? normalized : "/" + normalized)

        if (query == null || query.isEmpty()) {
            return base
        }

        List<String> queryParts = []
        query.each { k, v ->
            if (k != null && v != null) {
                queryParts.add(java.net.URLEncoder.encode(k.toString(), "UTF-8") + "=" + java.net.URLEncoder.encode(v.toString(), "UTF-8"))
            }
        }

        if (queryParts.isEmpty()) {
            return base
        }
        return base + (base.contains("?") ? "&" : "?") + queryParts.join("&")
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
