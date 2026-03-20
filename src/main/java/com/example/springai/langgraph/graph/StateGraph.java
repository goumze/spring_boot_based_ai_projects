package com.example.springai.langgraph.graph;

import java.util.HashMap;
import java.util.Map;

/**
 * A builder for defining LangGraph-style stateful workflow graphs.
 *
 * <p>Usage:
 * <pre>{@code
 * StateGraph graph = new StateGraph("my-workflow")
 *     .addNode("classify", state -> classify(state))
 *     .addNode("handle-question", state -> handleQuestion(state))
 *     .addNode("handle-task", state -> handleTask(state))
 *     .setEntryPoint("classify")
 *     .addConditionalEdge("classify", state ->
 *         "question".equals(state.getString("type")) ? "handle-question" : "handle-task")
 *     .addEdge("handle-question", StateGraph.END)
 *     .addEdge("handle-task", StateGraph.END)
 *     .compile();
 * }</pre>
 *
 * <p>Once {@link #compile()} is called the graph is frozen and can be executed via
 * {@link CompiledGraph#invoke(GraphState)}.
 */
public class StateGraph {

    /** Sentinel value returned by an edge to terminate graph execution. */
    public static final String END = "__end__";

    private final String name;
    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final Map<String, GraphEdge> edges = new HashMap<>();
    private String entryPoint;

    public StateGraph(String name) {
        this.name = name;
    }

    /**
     * Registers a named node.
     *
     * @param name the unique node name
     * @param node the node implementation
     * @return {@code this} for fluent chaining
     */
    public StateGraph addNode(String name, GraphNode node) {
        nodes.put(name, node);
        return this;
    }

    /**
     * Adds a static edge that always transitions to the same target.
     *
     * @param fromNode the source node name
     * @param toNode   the destination node name (or {@link #END})
     * @return {@code this} for fluent chaining
     */
    public StateGraph addEdge(String fromNode, String toNode) {
        edges.put(fromNode, state -> toNode);
        return this;
    }

    /**
     * Adds a conditional edge whose target is determined at runtime by inspecting the state.
     *
     * @param fromNode       the source node name
     * @param conditionEdge  a function that inspects the state and returns the next node name
     * @return {@code this} for fluent chaining
     */
    public StateGraph addConditionalEdge(String fromNode, GraphEdge conditionEdge) {
        edges.put(fromNode, conditionEdge);
        return this;
    }

    /**
     * Sets the entry-point node (the first node executed when the graph is invoked).
     *
     * @param nodeName the name of the entry node
     * @return {@code this} for fluent chaining
     */
    public StateGraph setEntryPoint(String nodeName) {
        this.entryPoint = nodeName;
        return this;
    }

    /**
     * Validates the graph definition and returns an executable {@link CompiledGraph}.
     *
     * @return a compiled, immutable graph ready for execution
     * @throws IllegalStateException if the graph is misconfigured (e.g., missing entry point or
     *                               node references an unknown target)
     */
    public CompiledGraph compile() {
        if (entryPoint == null || entryPoint.isBlank()) {
            throw new IllegalStateException("Graph '" + name + "' has no entry point set.");
        }
        if (!nodes.containsKey(entryPoint)) {
            throw new IllegalStateException(
                    "Entry point '" + entryPoint + "' is not a registered node in graph '" + name + "'.");
        }
        return new CompiledGraph(name, Map.copyOf(nodes), Map.copyOf(edges), entryPoint);
    }

    public String getName() {
        return name;
    }
}
