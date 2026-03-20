package com.example.springai.langgraph.graph;

/**
 * An edge that determines the next node to visit in a {@link StateGraph}.
 *
 * <p>Edges can be:
 * <ul>
 *   <li><b>Static</b> – always transition to the same target node.
 *   <li><b>Conditional</b> – inspect the current {@link GraphState} and return the name of the
 *       next node to execute (or {@link StateGraph#END} to terminate the graph).
 * </ul>
 *
 * <p>Returning {@link StateGraph#END} signals the graph runner to stop execution and return the
 * current state as the final result.
 */
@FunctionalInterface
public interface GraphEdge {

    /**
     * Determines the name of the next node to execute based on the current state.
     *
     * @param state the current graph state
     * @return the name of the next node, or {@link StateGraph#END} to finish the workflow
     */
    String next(GraphState state);
}
