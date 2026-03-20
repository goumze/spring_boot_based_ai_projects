package com.example.springai.langgraph.service;

import com.example.springai.langgraph.graph.CompiledGraph;
import com.example.springai.langgraph.graph.GraphState;
import com.example.springai.langgraph.graph.StateGraph;
import com.example.springai.langgraph.model.WorkflowResult;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * LangGraph Concepts Service.
 *
 * <p>Demonstrates how to build LangGraph-style stateful workflow graphs with Spring AI:
 *
 * <ul>
 *   <li><b>Routing workflow</b> – classifies the user input and routes it to a specialist node.
 *   <li><b>ReAct agent loop</b> – cycles between reasoning and acting until the agent decides it
 *       has enough information to give a final answer.
 *   <li><b>Multi-step processing pipeline</b> – a linear sequence of LLM calls where each node
 *       enriches the state before passing it to the next.
 * </ul>
 */
@Service
public class LangGraphService {

    private final ChatClient chatClient;

    public LangGraphService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // -------------------------------------------------------------------------
    // Workflow 1: Intent Routing Graph
    // -------------------------------------------------------------------------

    /**
     * Builds and executes an intent-routing workflow.
     *
     * <p>Graph topology:
     * <pre>
     *                    ┌──────────────────┐
     *   START ──────────► classify_intent  │
     *                    └────────┬─────────┘
     *                   conditional edge
     *              ┌──────────────┴────────────┐
     *              ▼                           ▼
     *   ┌──────────────────┐      ┌────────────────────┐
     *   │ handle_question  │      │ handle_task_request │
     *   └────────┬─────────┘      └──────────┬──────────┘
     *            └──────────┬────────────────┘
     *                       ▼
     *                      END
     * </pre>
     *
     * @param userInput the user's raw message
     * @return a {@link WorkflowResult} containing the routed response
     */
    public WorkflowResult runRoutingWorkflow(String userInput) {
        CompiledGraph graph = new StateGraph("intent-routing")
                .addNode("classify_intent", state -> {
                    String input = state.getString(GraphState.INPUT_KEY);
                    String classification = chatClient.prompt()
                            .system("Classify the user's message as exactly one of: QUESTION or TASK. Reply with only the word.")
                            .user(input)
                            .call()
                            .content();
                    return state.with("intent", classification.trim().toUpperCase());
                })
                .addNode("handle_question", state -> {
                    String input = state.getString(GraphState.INPUT_KEY);
                    String answer = chatClient.prompt()
                            .system("You are a knowledgeable assistant. Answer the question clearly and concisely.")
                            .user(input)
                            .call()
                            .content();
                    return state.with(GraphState.OUTPUT_KEY, answer);
                })
                .addNode("handle_task_request", state -> {
                    String input = state.getString(GraphState.INPUT_KEY);
                    String plan = chatClient.prompt()
                            .system("You are a task planner. Break down the requested task into clear, numbered steps.")
                            .user(input)
                            .call()
                            .content();
                    return state.with(GraphState.OUTPUT_KEY, plan);
                })
                .setEntryPoint("classify_intent")
                .addConditionalEdge("classify_intent", state -> {
                    String intent = state.getString("intent");
                    return intent.contains("QUESTION") ? "handle_question" : "handle_task_request";
                })
                .addEdge("handle_question", StateGraph.END)
                .addEdge("handle_task_request", StateGraph.END)
                .compile();

        GraphState initialState = new GraphState().with(GraphState.INPUT_KEY, userInput);
        GraphState finalState = graph.invoke(initialState);
        return toResult(graph.getName(), userInput, finalState);
    }

    // -------------------------------------------------------------------------
    // Workflow 2: ReAct (Reasoning + Acting) Agent Loop
    // -------------------------------------------------------------------------

    /**
     * Runs a simplified ReAct (Reasoning + Acting) agent loop.
     *
     * <p>Graph topology:
     * <pre>
     *   START ──► reason ──► act ──► [conditional]
     *                                   │ CONTINUE
     *                                   └──────────► reason  (cycle)
     *                                   │ FINISH
     *                                   └──────────► END
     * </pre>
     *
     * <p>The agent cycles between reasoning about the problem and deciding on an action until it
     * determines it has a final answer (max {@value CompiledGraph#MAX_STEPS} steps to prevent
     * infinite loops).
     *
     * @param task the task or question for the agent to solve
     * @return a {@link WorkflowResult} containing the agent's final answer and execution path
     */
    public WorkflowResult runReActWorkflow(String task) {
        CompiledGraph graph = new StateGraph("react-agent")
                .addNode("reason", state -> {
                    String taskStr = state.getString(GraphState.INPUT_KEY);
                    String previousThoughts = state.getString("thoughts");
                    String prompt = previousThoughts.isBlank()
                            ? "Task: " + taskStr + "\n\nThink step by step about how to solve this."
                            : "Task: " + taskStr + "\n\nPrevious thoughts:\n" + previousThoughts
                              + "\n\nContinue reasoning. If you have a final answer, start your response with 'FINAL ANSWER:'.";
                    String thought = chatClient.prompt()
                            .system("You are a reasoning agent. Think carefully about the task.")
                            .user(prompt)
                            .call()
                            .content();
                    return state.with("thoughts", previousThoughts + "\n" + thought);
                })
                .addNode("act", state -> {
                    String thoughts = state.getString("thoughts");
                    if (thoughts.contains("FINAL ANSWER:")) {
                        int idx = thoughts.lastIndexOf("FINAL ANSWER:");
                        String finalAnswer = thoughts.substring(idx + "FINAL ANSWER:".length()).trim();
                        return state
                                .with(GraphState.OUTPUT_KEY, finalAnswer)
                                .with("status", "FINISH");
                    }
                    return state.with("status", "CONTINUE");
                })
                .setEntryPoint("reason")
                .addEdge("reason", "act")
                .addConditionalEdge("act", state ->
                        "FINISH".equals(state.getString("status")) ? StateGraph.END : "reason")
                .compile();

        GraphState initialState = new GraphState()
                .with(GraphState.INPUT_KEY, task)
                .with("thoughts", "")
                .with("status", "CONTINUE");
        GraphState finalState = graph.invoke(initialState);
        return toResult(graph.getName(), task, finalState);
    }

    // -------------------------------------------------------------------------
    // Workflow 3: Multi-step Processing Pipeline
    // -------------------------------------------------------------------------

    /**
     * Runs a three-stage linear processing pipeline.
     *
     * <p>Graph topology:
     * <pre>
     *   START ──► extract_entities ──► analyse_sentiment ──► generate_report ──► END
     * </pre>
     *
     * @param text the input text to process
     * @return a {@link WorkflowResult} containing the final report and intermediate state
     */
    public WorkflowResult runPipelineWorkflow(String text) {
        CompiledGraph graph = new StateGraph("processing-pipeline")
                .addNode("extract_entities", state -> {
                    String input = state.getString(GraphState.INPUT_KEY);
                    String entities = chatClient.prompt()
                            .system("Extract the main named entities (people, places, organisations) from the text. List them, one per line.")
                            .user(input)
                            .call()
                            .content();
                    return state.with("entities", entities);
                })
                .addNode("analyse_sentiment", state -> {
                    String input = state.getString(GraphState.INPUT_KEY);
                    String sentiment = chatClient.prompt()
                            .system("Analyse the overall sentiment of the text. Return: POSITIVE, NEGATIVE, or NEUTRAL, followed by a one-sentence explanation.")
                            .user(input)
                            .call()
                            .content();
                    return state.with("sentiment", sentiment);
                })
                .addNode("generate_report", state -> {
                    String entities = state.getString("entities");
                    String sentiment = state.getString("sentiment");
                    String report = chatClient.prompt()
                            .system("You are a report writer. Combine the provided entities and sentiment analysis into a concise, structured summary report.")
                            .user("Entities:\n" + entities + "\n\nSentiment:\n" + sentiment)
                            .call()
                            .content();
                    return state.with(GraphState.OUTPUT_KEY, report);
                })
                .setEntryPoint("extract_entities")
                .addEdge("extract_entities", "analyse_sentiment")
                .addEdge("analyse_sentiment", "generate_report")
                .addEdge("generate_report", StateGraph.END)
                .compile();

        GraphState initialState = new GraphState().with(GraphState.INPUT_KEY, text);
        GraphState finalState = graph.invoke(initialState);
        return toResult(graph.getName(), text, finalState);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private WorkflowResult toResult(String workflowName, String input, GraphState finalState) {
        String output = finalState.getString(GraphState.OUTPUT_KEY);
        List<String> path = (List<String>) finalState
                .<List<String>>get("__execution_path__");
        return new WorkflowResult(workflowName, input, output,
                path != null ? path : List.of(), finalState.toMap());
    }
}
