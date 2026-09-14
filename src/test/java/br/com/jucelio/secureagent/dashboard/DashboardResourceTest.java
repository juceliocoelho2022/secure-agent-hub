package br.com.jucelio.secureagent.dashboard;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardResourceTest {

    @Test
    void dashboardExposesAuthenticatedUserAndAgentExecutionControls() throws IOException {
        String html = resource("/static/dashboard.html");
        assertTrue(html.contains("id=\"userIdentity\""));
        assertTrue(html.contains("id=\"agentPromptForm\""));
        assertTrue(html.contains("id=\"agentPrompt\""));
        assertTrue(html.contains("id=\"executionInspector\""));
        assertTrue(html.contains("id=\"decisionSummary\""));
        assertTrue(html.contains("id=\"executionTimeline\""));
    }

    @Test
    void dashboardJavaScriptUsesRealExecutionApprovalAndTimelineEndpoints() throws IOException {
        String javascript = resource("/static/assets/dashboard.js");
        assertTrue(javascript.contains("/api/v1/agents/executions"));
        assertTrue(javascript.contains("/timeline"));
        assertTrue(javascript.contains("/approve"));
        assertTrue(javascript.contains("/reject"));
        assertTrue(javascript.contains("authorities"));
    }

    @Test
    void dashboardRendersDecisionStateAndCanonicalLifecycle() throws IOException {
        String javascript = resource("/static/assets/dashboard.js");
        assertTrue(javascript.contains("Policy decision"));
        assertTrue(javascript.contains("Human decision"));
        assertTrue(javascript.contains("POLICY_EVALUATED"));
        assertTrue(javascript.contains("HUMAN_APPROVED"));
        assertTrue(javascript.contains("TOOL_EXECUTED"));
        assertTrue(javascript.contains("COMPLETED"));
        assertTrue(javascript.contains("future ? 'future'"));
    }

    @Test
    void dashboardInspectorShowsPlannerAndProviderUsageWithoutFabricatingTokens() throws IOException {
        String html = resource("/static/dashboard.html");
        String telemetry = resource("/static/assets/dashboard-telemetry.js");
        assertTrue(html.contains("id=\"plannerTelemetry\""));
        assertTrue(html.contains("/assets/dashboard-telemetry.js"));
        assertTrue(telemetry.contains("plannerSource"));
        assertTrue(telemetry.contains("promptTokens"));
        assertTrue(telemetry.contains("completionTokens"));
        assertTrue(telemetry.contains("totalTokens"));
        assertTrue(telemetry.contains("Provider usage: n/a"));
    }

    @Test
    void dashboardUsesPortfolioCommandCenterLayout() throws IOException {
        String html = resource("/static/dashboard.html");
        assertTrue(html.contains("Operations Command Center"));
        assertTrue(html.contains("class=\"command-center-grid\""));
        assertTrue(html.contains("class=\"panel agents-overview\""));
        assertTrue(html.contains("class=\"panel governance-overview\""));
        assertTrue(html.contains("class=\"panel events-overview\""));
        assertTrue(html.contains("/assets/dashboard-visual.css"));
        assertTrue(html.contains("AI Agent"));
        assertTrue(html.contains("Observability"));
    }

    @Test
    void dashboardExposesLiveAgentAndExecutionAnalytics() throws IOException {
        String html = resource("/static/dashboard.html");
        String analytics = resource("/static/assets/dashboard-analytics.js");
        assertTrue(html.contains("id=\"agentStatusPanel\""));
        assertTrue(html.contains("id=\"executionStatusChart\""));
        assertTrue(html.contains("id=\"plannerUsageChart\""));
        assertTrue(html.contains("id=\"approvalPressure\""));
        assertTrue(html.contains("/assets/dashboard-analytics.js"));
        assertTrue(analytics.contains("state.executions"));
        assertTrue(analytics.contains("plannerSource"));
        assertTrue(analytics.contains("WAITING_APPROVAL"));
        assertTrue(analytics.contains("n/a"));
    }

    @Test
    void dashboardSupportsPersistentDarkAndLightThemes() throws IOException {
        String html = resource("/static/dashboard.html");
        String theme = resource("/static/assets/dashboard-theme.js");
        String visual = resource("/static/assets/dashboard-visual.css");
        assertTrue(html.contains("id=\"themeToggle\""));
        assertTrue(html.contains("/assets/dashboard-theme.js"));
        assertTrue(theme.contains("localStorage"));
        assertTrue(theme.contains("data-theme"));
        assertTrue(theme.contains("light"));
        assertTrue(theme.contains("dark"));
        assertTrue(visual.contains("data-theme=\"light\""));
    }

    @Test
    void dashboardExposesOperationalIntelligenceWithoutFabricatingRiskOrPipelineData() throws IOException {
        String html = resource("/static/dashboard.html");
        String operations = resource("/static/assets/dashboard-operations.js");

        assertTrue(html.contains("id=\"riskOverview\""), "dashboard should expose risk overview");
        assertTrue(html.contains("id=\"eventPipelineHealth\""), "dashboard should expose event pipeline health");
        assertTrue(html.contains("/assets/dashboard-operations.js"), "dashboard should load operational intelligence renderer");
        assertTrue(operations.contains("/api/v1/operations/pipeline-health"), "pipeline data should come from the backend");
        assertTrue(operations.contains("candidate.riskScore"), "risk overview should use persisted backend risk score");
        assertTrue(operations.contains("candidate.riskLevel"), "risk overview should use persisted backend risk level");
        assertTrue(operations.contains("candidate.riskReasons"), "risk overview should show explainable backend reasons");
        assertTrue(operations.contains("n/a"), "unavailable metrics should render neutrally");
    }

    @Test
    void dashboardCanSubmitTrustedStructuredRiskContext() throws IOException {
        String html = resource("/static/dashboard.html");
        String javascript = resource("/static/assets/dashboard.js");

        assertTrue(html.contains("id=\"riskTransactionId\""));
        assertTrue(html.contains("id=\"riskAmount\""));
        assertTrue(html.contains("id=\"riskCountry\""));
        assertTrue(html.contains("id=\"riskUsualCountry\""));
        assertTrue(html.contains("id=\"riskHour\""));
        assertTrue(html.contains("id=\"riskRapidRetry\""));
        assertTrue(html.contains("id=\"riskKnownDevice\""));
        assertTrue(javascript.contains("buildExecutionContext"));
        assertTrue(javascript.contains("JSON.stringify({ agent, prompt, context })"));
    }

    private String resource(String path) throws IOException {
        try (var input = getClass().getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing classpath resource: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
