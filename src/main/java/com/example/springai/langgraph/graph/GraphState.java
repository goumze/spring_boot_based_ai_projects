package com.example.springai.langgraph.graph;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the shared mutable state that flows through a LangGraph workflow.
 *
 * <p>State is a key-value store that every node can read from and write to. Each node receives the
 * current state, performs its work, and returns an updated copy. This design mirrors LangGraph's
 * {@code TypedDict} / Annotated state model.
 *
 * <p>Special reserved keys:
 * <ul>
 *   <li>{@code __input__} – the initial user input
 *   <li>{@code __output__} – the final response to be returned
 *   <li>{@code __next_node__} – used by conditional edges to signal the next node
 * </ul>
 */
public class GraphState {

    public static final String INPUT_KEY = "__input__";
    public static final String OUTPUT_KEY = "__output__";
    public static final String NEXT_NODE_KEY = "__next_node__";

    private final Map<String, Object> data;

    public GraphState() {
        this.data = new HashMap<>();
    }

    private GraphState(Map<String, Object> data) {
        this.data = new HashMap<>(data);
    }

    /** Returns the value associated with the given key, or {@code null} if absent. */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    /** Returns the value as a {@link String}, or an empty string if absent. */
    public String getString(String key) {
        Object value = data.get(key);
        return value == null ? "" : value.toString();
    }

    /** Stores a value and returns a new {@link GraphState} with the updated entry. */
    public GraphState with(String key, Object value) {
        Map<String, Object> updated = new HashMap<>(this.data);
        updated.put(key, value);
        return new GraphState(updated);
    }

    /** Returns true if the given key is present in the state. */
    public boolean has(String key) {
        return data.containsKey(key);
    }

    /** Returns a read-only snapshot of the entire state map. */
    public Map<String, Object> toMap() {
        return Map.copyOf(data);
    }

    @Override
    public String toString() {
        return "GraphState" + data;
    }
}
