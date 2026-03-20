package com.example.springai.langgraph.graph;

/**
 * A node in a LangGraph {@link StateGraph}.
 *
 * <p>Each node is a stateless, pure function: it receives the current {@link GraphState}, performs
 * its processing (e.g., calls an LLM, executes a tool, runs validation), and returns an updated
 * state. Nodes must not mutate the input state directly; instead they call
 * {@link GraphState#with(String, Object)} to produce a new state.
 *
 * <p>This mirrors LangGraph's node concept where each node is a callable that takes and returns
 * a state dictionary.
 */
@FunctionalInterface
public interface GraphNode {

    /**
     * Processes the current state and returns a new (possibly modified) state.
     *
     * @param state the current graph state
     * @return the updated graph state after this node's processing
     */
    GraphState process(GraphState state);
}
