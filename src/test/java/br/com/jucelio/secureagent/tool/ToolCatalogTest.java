package br.com.jucelio.secureagent.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCatalogTest {
    private final ToolCatalog catalog = new ToolCatalog();

    @Test
    void acceptsOnlyBackendKnownTools() {
        assertTrue(catalog.isAllowed("getTransaction"));
        assertTrue(catalog.isAllowed("getCustomer"));
        assertTrue(catalog.isAllowed("calculateRisk"));
        assertTrue(catalog.isAllowed("blockCard"));
        assertTrue(catalog.isAllowed("deleteAccount"));
        assertFalse(catalog.isAllowed("runShellCommand"));
        assertFalse(catalog.isAllowed(null));
    }
}
