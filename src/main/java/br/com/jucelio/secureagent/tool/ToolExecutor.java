package br.com.jucelio.secureagent.tool;

import org.springframework.stereotype.Service;

@Service
public class ToolExecutor {
    public String execute(String toolName) {
        return switch (toolName) {
            case "getTransaction" -> "Transaction retrieved successfully (mock).";
            case "getCustomer" -> "Customer retrieved successfully (mock).";
            case "calculateRisk" -> "Risk score calculated: 42 (mock).";
            case "blockCard" -> "Card blocked successfully (mock).";
            default -> throw new IllegalArgumentException("Unsupported tool: " + toolName);
        };
    }
}
