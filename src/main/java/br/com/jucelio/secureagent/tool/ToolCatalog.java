package br.com.jucelio.secureagent.tool;

import java.util.Set;

public class ToolCatalog {
    private static final Set<String> ALLOWED = Set.of(
            "getTransaction",
            "getCustomer",
            "calculateRisk",
            "blockCard",
            "deleteAccount"
    );

    public boolean isAllowed(String toolName) {
        return toolName != null && ALLOWED.contains(toolName);
    }

    public Set<String> allowedToolNames() {
        return ALLOWED;
    }
}
