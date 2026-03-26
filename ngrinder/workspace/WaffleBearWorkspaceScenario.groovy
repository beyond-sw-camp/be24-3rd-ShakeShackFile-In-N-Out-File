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
import java.util.UUID

@RunWith(GrinderRunner)
class WaffleBearWorkspaceScenario {

    public static GTest loginTest
    public static GTest listTest
    public static GTest saveTest
    public static GTest readTest
    public static GTest byUuidTest
    public static GTest isSharedTest
    public static GTest loadRoleTest
    public static GTest saveRoleTest
    public static GTest deleteTest

    public static HTTPRequest loginRequest
    public static HTTPRequest listRequest
    public static HTTPRequest saveRequest
    public static HTTPRequest readRequest
    public static HTTPRequest byUuidRequest
    public static HTTPRequest isSharedRequest
    public static HTTPRequest loadRoleRequest
    public static HTTPRequest saveRoleRequest
    public static HTTPRequest deleteRequest
    public static HTTPRequest fixtureRequest

    public static JsonSlurper jsonSlurper = new JsonSlurper()
    public static String baseUrl

    String accessToken

    @BeforeProcess
    static void beforeProcess() {
        HTTPPluginControl.getConnectionDefaults().timeout = 120000
        HTTPPluginControl.getConnectionDefaults().followRedirects = true

        baseUrl = buildBaseUrl()

        loginTest = new GTest(1, "workspace-login")
        listTest = new GTest(2, "workspace-list")
        saveTest = new GTest(3, "workspace-save")
        readTest = new GTest(4, "workspace-read")
        byUuidTest = new GTest(5, "workspace-by-uuid")
        isSharedTest = new GTest(6, "workspace-isShared")
        loadRoleTest = new GTest(7, "workspace-loadRole")
        saveRoleTest = new GTest(8, "workspace-saveRole")
        deleteTest = new GTest(9, "workspace-delete")

        loginRequest = new HTTPRequest()
        listRequest = new HTTPRequest()
        saveRequest = new HTTPRequest()
        readRequest = new HTTPRequest()
        byUuidRequest = new HTTPRequest()
        isSharedRequest = new HTTPRequest()
        loadRoleRequest = new HTTPRequest()
        saveRoleRequest = new HTTPRequest()
        deleteRequest = new HTTPRequest()
        fixtureRequest = new HTTPRequest()

        loginTest.record(loginRequest)
        listTest.record(listRequest)
        saveTest.record(saveRequest)
        readTest.record(readRequest)
        byUuidTest.record(byUuidRequest)
        isSharedTest.record(isSharedRequest)
        loadRoleTest.record(loadRoleRequest)
        saveRoleTest.record(saveRoleRequest)
        deleteTest.record(deleteRequest)
    }

    @BeforeThread
    void beforeThread() {
        grinder.statistics.delayReports = true
    }

    @Before
    void before() {
        accessToken = loginAsAdministrator()
    }

    @Test
    void workspaceList() {
        HTTPResponse response = get(listRequest, "/workspace/list")
        assertStatus(response, "GET /workspace/list", [200])
    }

    @Test
    void workspaceSave() {
        Map created = createWorkspace(saveRequest, "save")
        cleanupWorkspace(created.idx as Long)
    }

    @Test
    void workspaceRead() {
        Map created = createWorkspace(fixtureRequest, "read")
        try {
            HTTPResponse response = get(readRequest, "/workspace/read/${created.idx}")
            assertStatus(response, "GET /workspace/read/{idx}", [200])
        } finally {
            cleanupWorkspace(created.idx as Long)
        }
    }

    @Test
    void workspaceByUuid() {
        Map created = createWorkspace(fixtureRequest, "uuid")
        try {
            String uuid = created.uuid as String
            if (!uuid) {
                throw new IllegalStateException("Workspace uuid was not found in save response.")
            }
            HTTPResponse response = get(byUuidRequest, "/workspace/by-uuid/${uuid}")
            assertStatus(response, "GET /workspace/by-uuid/{uuid}", [200])
        } finally {
            cleanupWorkspace(created.idx as Long)
        }
    }

    @Test
    void workspaceIsShared() {
        Map created = createWorkspace(fixtureRequest, "share")
        try {
            Map payload = [
                    type  : true,
                    status: "Public"
            ]
            HTTPResponse response = post(isSharedRequest, "/workspace/isShared/${created.idx}", payload)
            assertStatus(response, "POST /workspace/isShared/{idx}", [200])
        } finally {
            cleanupWorkspace(created.idx as Long)
        }
    }

    @Test
    void workspaceLoadRole() {
        Map created = createWorkspace(fixtureRequest, "role-list")
        try {
            HTTPResponse response = get(loadRoleRequest, "/workspace/loadRole/${created.idx}")
            assertStatus(response, "GET /workspace/loadRole/{idx}", [200])
        } finally {
            cleanupWorkspace(created.idx as Long)
        }
    }

    @Test
    void workspaceSaveRole() {
        Map created = createWorkspace(fixtureRequest, "role-save")
        try {
            HTTPResponse response = post(saveRoleRequest, "/workspace/saveRole/${created.idx}", [:])
            assertStatus(response, "POST /workspace/saveRole/{idx}", [200])
        } finally {
            cleanupWorkspace(created.idx as Long)
        }
    }

    @Test
    void workspaceDelete() {
        Map created = createWorkspace(fixtureRequest, "delete")
        HTTPResponse response = post(deleteRequest, "/workspace/delete/${created.idx}", null)
        assertStatus(response, "POST /workspace/delete/{idx}", [200])
    }

    private String loginAsAdministrator() {
        String adminEmail = property("loginEmail", "administrator@administrator.adm")
        String adminPassword = property("loginPassword", "fweiuhfge2232n12@#xSD23@")

        Map payload = [
                email   : adminEmail,
                password: adminPassword
        ]

        HTTPResponse loginResponse = postAnonymous(loginRequest, "/login", payload)
        int statusCode = loginResponse.getStatusCode()
        if (statusCode == 404 || statusCode == 405) {
            loginResponse = postAnonymous(loginRequest, "/api/login", payload)
        }
        assertStatus(loginResponse, "POST /login", [200])

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

    private Map createWorkspace(HTTPRequest req, String tag) {
        String suffix = UUID.randomUUID().toString().substring(0, 8)
        Map payload = [
                idx     : null,
                title   : "ngrinder-ws-${tag}-${suffix}",
                contents: "workspace scenario ${tag} @ " + System.currentTimeMillis()
        ]

        HTTPResponse saveResponse = post(req, "/workspace/save", payload)
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

    private void cleanupWorkspace(Long workspaceIdx) {
        if (workspaceIdx == null) {
            return
        }

        try {
            HTTPResponse response = post(fixtureRequest, "/workspace/delete/${workspaceIdx}", null)
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                grinder.logger.warn("workspace cleanup failed. idx={}, status={}, body={}", workspaceIdx, response.getStatusCode(), response.getText())
            }
        } catch (Exception e) {
            grinder.logger.warn("workspace cleanup exception. idx={}, message={}", workspaceIdx, e.getMessage())
        }
    }

    private HTTPResponse get(HTTPRequest req, String path) {
        req.setHeaders(authJsonHeaders())
        return req.GET(buildUrl(path))
    }

    private HTTPResponse post(HTTPRequest req, String path, Object body) {
        req.setHeaders(authJsonHeaders())
        byte[] payload = body == null
                ? new byte[0]
                : JsonOutput.toJson(body).getBytes(StandardCharsets.UTF_8)
        return req.POST(buildUrl(path), payload)
    }

    private HTTPResponse postAnonymous(HTTPRequest req, String path, Object body) {
        req.setHeaders(jsonHeaders())
        byte[] payload = body == null
                ? new byte[0]
                : JsonOutput.toJson(body).getBytes(StandardCharsets.UTF_8)
        return req.POST(buildUrl(path), payload)
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

    private static String buildBaseUrl() {
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

    private static Long asLong(Object value) {
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
    private static void assertStatus(HTTPResponse response, String step, List<Integer> allowedStatuses) {
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

    private static String sanitizeToken(String rawToken) {
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
