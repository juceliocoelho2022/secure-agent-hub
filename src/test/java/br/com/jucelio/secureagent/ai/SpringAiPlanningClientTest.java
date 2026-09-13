package br.com.jucelio.secureagent.ai;

import br.com.jucelio.secureagent.tool.ToolCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SpringAiPlanningClientTest {
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;
    private SpringAiPlanningClient client;

    @BeforeEach
    void setup() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        client = new SpringAiPlanningClient(chatClient, new ToolCatalog());
    }

    @Test
    void returnsStructuredProposalWithoutRegisteringTools() {
        AiToolProposal expected = new AiToolProposal(
                "blockCard",
                "Potential fraud detected");
        when(responseSpec.entity(AiToolProposal.class)).thenReturn(expected);

        AiToolProposal actual = client.propose("Analise esta possível fraude");

        assertEquals(expected, actual);
        verify(requestSpec).user("Analise esta possível fraude");
        verify(responseSpec).entity(AiToolProposal.class);
        verify(requestSpec, never()).tools(any(Object[].class));
        verify(requestSpec, never()).toolNames(any(String[].class));
    }

    @Test
    void systemPromptDeclaresPlannerOnlyAndAllowedTools() {
        when(responseSpec.entity(AiToolProposal.class))
                .thenReturn(new AiToolProposal("calculateRisk", "baseline"));

        client.propose("Calcule o risco");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).system(captor.capture());
        String systemPrompt = captor.getValue();

        assertTrue(systemPrompt.contains("intent planner, not an executor"));
        assertTrue(systemPrompt.contains("Never execute tools"));
        assertTrue(systemPrompt.contains("getTransaction"));
        assertTrue(systemPrompt.contains("getCustomer"));
        assertTrue(systemPrompt.contains("calculateRisk"));
        assertTrue(systemPrompt.contains("blockCard"));
        assertTrue(systemPrompt.contains("deleteAccount"));
    }
}
