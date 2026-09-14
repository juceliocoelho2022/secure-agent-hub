package br.com.jucelio.secureagent.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiToolProposalTest {

    @Test
    void proposalRequiresToolNameAndExplanation() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiToolProposal("", "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> new AiToolProposal("blockCard", ""));
        assertThrows(IllegalArgumentException.class,
                () -> new AiToolProposal(null, "reason"));
    }

    @Test
    void preservesValidatedStructuredProposal() {
        AiToolProposal proposal = new AiToolProposal(
                "blockCard",
                "Potential fraud detected");

        assertEquals("blockCard", proposal.toolName());
        assertEquals("Potential fraud detected", proposal.explanation());
    }
}
