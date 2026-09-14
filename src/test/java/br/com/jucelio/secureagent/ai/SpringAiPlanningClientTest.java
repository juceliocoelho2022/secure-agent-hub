package br.com.jucelio.secureagent.ai;

import br.com.jucelio.secureagent.tool.ToolCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

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
    void returnsStructuredProposalWithProviderUsageWithoutRegisteringTools() {
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage output = mock(AssistantMessage.class);
        ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
        Usage usage = mock(Usage.class);

        when(responseSpec.chatResponse()).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(output);
        when(output.getText()).thenReturn("{\"toolName\":\"blockCard\",\"explanation\":\"Potential fraud detected\"}");
        when(chatResponse.getMetadata()).thenReturn(metadata);
        when(metadata.getUsage()).thenReturn(usage);
        when(usage.getPromptTokens()).thenReturn(21);
        when(usage.getCompletionTokens()).thenReturn(9);
        when(usage.getTotalTokens()).thenReturn(30);

        AiPlanningResult actual = client.propose("Analise esta possível fraude");

        assertEquals("blockCard", actual.proposal().toolName());
        assertEquals("Potential fraud detected", actual.proposal().explanation());
        assertEquals(21, actual.usage().promptTokens());
        assertEquals(9, actual.usage().completionTokens());
        assertEquals(30, actual.usage().totalTokens());
        verify(requestSpec).user("Analise esta possível fraude");
        verify(responseSpec).chatResponse();
        verify(requestSpec, never()).tools(any(Object[].class));
        verify(requestSpec, never()).toolNames(any(String[].class));
    }

    @Test
    void systemPromptDeclaresPlannerOnlyAndAllowedTools() {
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage output = mock(AssistantMessage.class);
        ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
        Usage usage = mock(Usage.class);

        when(responseSpec.chatResponse()).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(output);
        when(output.getText()).thenReturn("{\"toolName\":\"calculateRisk\",\"explanation\":\"baseline\"}");
        when(chatResponse.getMetadata()).thenReturn(metadata);
        when(metadata.getUsage()).thenReturn(usage);
        when(usage.getPromptTokens()).thenReturn(5);
        when(usage.getCompletionTokens()).thenReturn(3);
        when(usage.getTotalTokens()).thenReturn(8);

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
