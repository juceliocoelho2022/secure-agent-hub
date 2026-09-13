package br.com.jucelio.secureagent.dashboard;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardResourceTest {

    @Test
    void dashboardExposesAuthenticatedUserAndAgentExecutionControls() throws IOException {
        String html = resource("/static/dashboard.html");

        assertTrue(html.contains("id=\"userIdentity\""), "dashboard should render the authenticated user identity");
        assertTrue(html.contains("id=\"agentPromptForm\""), "dashboard should expose an Ask Agent form");
        assertTrue(html.contains("id=\"agentPrompt\""), "dashboard should expose the agent prompt input");
    }

    @Test
    void dashboardJavaScriptUsesRealExecutionAndApprovalEndpoints() throws IOException {
        String javascript = resource("/static/assets/dashboard.js");

        assertTrue(javascript.contains("/api/v1/agents/executions"), "dashboard should create and refresh executions through the real API");
        assertTrue(javascript.contains("/approve"), "dashboard should support approval actions");
        assertTrue(javascript.contains("/reject"), "dashboard should support rejection actions");
        assertTrue(javascript.contains("authorities"), "dashboard should render roles returned by /api/v1/users/me");
    }

    private String resource(String path) throws IOException {
        try (var input = getClass().getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Missing classpath resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
