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
        assertTrue(html.contains("id=\"executionInspector\""), "dashboard should expose an execution inspector");
        assertTrue(html.contains("id=\"decisionSummary\""), "dashboard should expose a decision summary");
        assertTrue(html.contains("id=\"executionTimeline\""), "dashboard should expose an operational execution timeline");
    }

    @Test
    void dashboardJavaScriptUsesRealExecutionApprovalAndTimelineEndpoints() throws IOException {
        String javascript = resource("/static/assets/dashboard.js");

        assertTrue(javascript.contains("/api/v1/agents/executions"), "dashboard should create and refresh executions through the real API");
        assertTrue(javascript.contains("/timeline"), "dashboard should load the operational timeline for an execution");
        assertTrue(javascript.contains("/approve"), "dashboard should support approval actions");
        assertTrue(javascript.contains("/reject"), "dashboard should support rejection actions");
        assertTrue(javascript.contains("authorities"), "dashboard should render roles returned by /api/v1/users/me");
    }

    @Test
    void dashboardRendersDecisionStateAndCanonicalLifecycle() throws IOException {
        String javascript = resource("/static/assets/dashboard.js");

        assertTrue(javascript.contains("Policy decision"), "inspector should explain the policy decision");
        assertTrue(javascript.contains("Human decision"), "inspector should explain the human decision");
        assertTrue(javascript.contains("POLICY_EVALUATED"), "timeline should include the policy evaluation stage");
        assertTrue(javascript.contains("HUMAN_APPROVED"), "timeline should include the human approval stage");
        assertTrue(javascript.contains("TOOL_EXECUTED"), "timeline should include the controlled tool execution stage");
        assertTrue(javascript.contains("COMPLETED"), "timeline should include the terminal completed stage");
        assertTrue(javascript.contains("future ? 'future'"), "future lifecycle stages should be visibly distinguished");
    }

    @Test
    void dashboardInspectorShowsPlannerAndProviderUsageWithoutFabricatingTokens() throws IOException {
        String html = resource("/static/dashboard.html");
        String telemetry = resource("/static/assets/dashboard-telemetry.js");

        assertTrue(html.contains("id=\"plannerTelemetry\""), "inspector should expose planner telemetry");
        assertTrue(html.contains("/assets/dashboard-telemetry.js"), "dashboard should load the telemetry renderer");
        assertTrue(telemetry.contains("plannerSource"), "dashboard should render the planner source returned by the API");
        assertTrue(telemetry.contains("promptTokens"), "dashboard should render prompt token usage when available");
        assertTrue(telemetry.contains("completionTokens"), "dashboard should render completion token usage when available");
        assertTrue(telemetry.contains("totalTokens"), "dashboard should render total token usage when available");
        assertTrue(telemetry.contains("Provider usage: n/a"), "rule-based executions should not fabricate zero token usage");
    }

    @Test
    void dashboardUsesPortfolioCommandCenterLayout() throws IOException {
        String html = resource("/static/dashboard.html");

        assertTrue(html.contains("Operations Command Center"), "dashboard should use the portfolio command-center heading");
        assertTrue(html.contains("class=\"command-center-grid\""), "dashboard should expose the command-center layout");
        assertTrue(html.contains("class=\"panel agents-overview\""), "dashboard should expose an agents overview panel");
        assertTrue(html.contains("class=\"panel governance-overview\""), "dashboard should expose a governance overview panel");
        assertTrue(html.contains("class=\"panel events-overview\""), "dashboard should expose an events overview panel");
        assertTrue(html.contains("/assets/dashboard-visual.css"), "dashboard should load the portfolio visual layer");
        assertTrue(html.contains("AI Agent"), "architecture should visually identify the AI agent stage");
        assertTrue(html.contains("Observability"), "architecture should visually identify the observability stage");
    }

    @Test
    void dashboardExposesLiveAgentAndExecutionAnalytics() throws IOException {
        String html = resource("/static/dashboard.html");
        String analytics = resource("/static/assets/dashboard-analytics.js");

        assertTrue(html.contains("id=\"agentStatusPanel\""), "dashboard should expose a live agent status panel");
        assertTrue(html.contains("id=\"executionStatusChart\""), "dashboard should expose execution status analytics");
        assertTrue(html.contains("id=\"plannerUsageChart\""), "dashboard should expose planner usage analytics");
        assertTrue(html.contains("id=\"approvalPressure\""), "dashboard should expose approval pressure");
        assertTrue(html.contains("/assets/dashboard-analytics.js"), "dashboard should load the live analytics renderer");
        assertTrue(analytics.contains("state.executions"), "analytics should derive values from loaded backend executions");
        assertTrue(analytics.contains("plannerSource"), "analytics should derive planner usage from execution provenance");
        assertTrue(analytics.contains("WAITING_APPROVAL"), "analytics should derive approval pressure from governed execution state");
        assertTrue(analytics.contains("n/a"), "analytics should use neutral values when real data is unavailable");
    }

    @Test
    void dashboardSupportsPersistentDarkAndLightThemes() throws IOException {
        String html = resource("/static/dashboard.html");
        String theme = resource("/static/assets/dashboard-theme.js");
        String visual = resource("/static/assets/dashboard-visual.css");

        assertTrue(html.contains("id=\"themeToggle\""), "dashboard should expose a theme toggle in the command center");
        assertTrue(html.contains("/assets/dashboard-theme.js"), "dashboard should load the theme controller");
        assertTrue(theme.contains("localStorage"), "dashboard should persist the selected theme in the browser");
        assertTrue(theme.contains("data-theme"), "dashboard should apply the selected theme through a document attribute");
        assertTrue(theme.contains("light"), "dashboard should support a light theme");
        assertTrue(theme.contains("dark"), "dashboard should support a dark theme");
        assertTrue(visual.contains("data-theme=\"light\""), "visual layer should define the white/light palette");
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
