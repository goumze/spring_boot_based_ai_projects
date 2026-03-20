package com.example.springai.langgraph.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An immutable, executable LangGraph workflow graph produced by {@link StateGraph#compile()}.
 *
 * <p>Execution model:
 * <ol>
 *   <li>Start at the entry-point node.
 *   <li>Execute the node – pass the current state in, receive an updated state out.
 *   <li>Follow the node's outgoing edge to determine the next node (or {@link StateGraph#END}).
 *   <li>Repeat until {@link StateGraph#END} is reached or the step limit is exceeded.
 * </ol>
 *
 * <p>A step limit ({@value #MAX_STEPS}) guards against infinite cycles.
 */
public class CompiledGraph {

    /** Maximum number of node executions before the graph is forcibly stopped. */
    public static final int MAX_STEPS = 50;

    private final String name;
    private final Map<String, GraphNode> nodes;
    private final Map<String, GraphEdge> edges;
    private final String entryPoint;

    CompiledGraph(String name, Map<String, GraphNode> nodes,
                  Map<String, GraphEdge> edges, String entryPoint) {
        this.name = name;
        this.nodes = nodes;
        this.edges = edges;
        this.entryPoint = entryPoint;
    }

    /**
     * Executes the graph starting from the entry point.
     *
     * @param initialState the initial state (typically contains {@code __input__})
     * @return the final state after all nodes have been executed
     * @throws IllegalStateException if an unknown node name is encountered or the step limit is
     *                               exceeded
     */
    public GraphState invoke(GraphState initialState) {
        GraphState state = initialState;
        String currentNode = entryPoint;
        List<String> executionPath = new ArrayList<>();
        int steps = 0;

        while (!StateGraph.END.equals(currentNode)) {
            if (steps++ >= MAX_STEPS) {
                throw new IllegalStateException(
                        "Graph '" + name + "' exceeded the maximum step limit of " + MAX_STEPS
                        + ". Possible infinite loop. Execution path: " + executionPath);
            }

            GraphNode node = nodes.get(currentNode);
            if (node == null) {
                throw new IllegalStateException(
                        "Unknown node '" + currentNode + "' in graph '" + name + "'.");
            }

            executionPath.add(currentNode);
            state = node.process(state);

            GraphEdge edge = edges.get(currentNode);
            if (edge == null) {
                // No outgoing edge – treat as terminal
                break;
            }
            currentNode = edge.next(state);
        }

        // Attach the execution path to the final state for observability
        return state.with("__execution_path__", executionPath);
    }

    public String getName() {
        return name;
    }
}
