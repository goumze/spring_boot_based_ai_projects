package com.example.springai.langchain.model;

import java.util.Map;

/**
 * A named prompt template with {@code {variable}} placeholders.
 *
 * <p>Variables are resolved by calling {@link #format(Map)}.
 *
 * <p>Example:
 * <pre>{@code
 * var template = new PromptTemplate("Translate the following text to {language}:\n{text}");
 * String prompt = template.format(Map.of("language", "French", "text", "Hello world"));
 * }</pre>
 */
public class PromptTemplate {

    private final String template;

    public PromptTemplate(String template) {
        this.template = template;
    }

    /**
     * Resolves all {@code {key}} placeholders with the supplied variable values.
     *
     * @param variables a map of placeholder name to replacement value
     * @return the fully resolved prompt string
     * @throws IllegalArgumentException if a required variable is missing
     */
    public String format(Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (!result.contains(placeholder)) {
                continue;
            }
            result = result.replace(placeholder, entry.getValue());
        }
        // Check for unresolved placeholders
        if (result.matches(".*\\{[^}]+}.*")) {
            throw new IllegalArgumentException(
                    "Prompt template contains unresolved placeholders: " + result);
        }
        return result;
    }

    public String getTemplate() {
        return template;
    }
}
