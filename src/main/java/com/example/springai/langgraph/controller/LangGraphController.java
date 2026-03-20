package com.example.springai.langgraph.controller;

import com.example.springai.langgraph.model.WorkflowResult;
import com.example.springai.langgraph.service.LangGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing LangGraph workflow endpoints.
 *
 * <pre>
 * POST /api/langgraph/routing-workflow  – intent classification + routing graph
 * POST /api/langgraph/react-workflow    – ReAct (reasoning + acting) agent loop
 * POST /api/langgraph/pipeline-workflow – multi-step entity/sentiment processing pipeline
 * </pre>
 */
@RestController
@RequestMapping("/api/langgraph")
public class LangGraphController {

    private final LangGraphService langGraphService;

    public LangGraphController(LangGraphService langGraphService) {
        this.langGraphService = langGraphService;
    }

    /**
     * Runs the intent-routing workflow.
     *
     * <p>The graph classifies the input as a QUESTION or TASK and routes it to the appropriate
     * handler node.
     *
     * <p>Example request body:
     * <pre>{@code { "input": "What is the capital of France?" } }</pre>
     */
    @PostMapping("/routing-workflow")
    public ResponseEntity<WorkflowResult> routing(@RequestBody InputRequest request) {
        WorkflowResult result = langGraphService.runRoutingWorkflow(request.input());
        return ResponseEntity.ok(result);
    }

    /**
     * Runs the ReAct agent-loop workflow.
     *
     * <p>The graph cycles between reasoning and acting until the agent produces a final answer.
     *
     * <p>Example request body:
     * <pre>{@code { "input": "What are the main benefits of microservices architecture?" } }</pre>
     */
    @PostMapping("/react-workflow")
    public ResponseEntity<WorkflowResult> react(@RequestBody InputRequest request) {
        WorkflowResult result = langGraphService.runReActWorkflow(request.input());
        return ResponseEntity.ok(result);
    }

    /**
     * Runs the multi-step processing pipeline workflow.
     *
     * <p>The graph extracts entities, analyses sentiment, and generates a report.
     *
     * <p>Example request body:
     * <pre>{@code { "input": "Apple CEO Tim Cook announced new products in Cupertino today." } }</pre>
     */
    @PostMapping("/pipeline-workflow")
    public ResponseEntity<WorkflowResult> pipeline(@RequestBody InputRequest request) {
        WorkflowResult result = langGraphService.runPipelineWorkflow(request.input());
        return ResponseEntity.ok(result);
    }

    record InputRequest(String input) {}
}
