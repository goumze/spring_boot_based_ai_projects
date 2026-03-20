package com.example.springai.langgraph.model;

import java.util.List;
import java.util.Map;

/**
 * The result returned after executing a LangGraph workflow.
 *
 * @param workflowName  the name of the executed workflow
 * @param input         the original input passed to the workflow
 * @param output        the final output produced by the workflow
 * @param executionPath the ordered list of node names that were executed
 * @param finalState    a snapshot of all state entries at the end of execution
 */
public record WorkflowResult(
        String workflowName,
        String input,
        String output,
        List<String> executionPath,
        Map<String, Object> finalState) {}
