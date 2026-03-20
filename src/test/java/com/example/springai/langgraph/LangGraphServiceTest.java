package com.example.springai.langgraph;

import com.example.springai.langgraph.graph.CompiledGraph;
import com.example.springai.langgraph.graph.GraphState;
import com.example.springai.langgraph.graph.StateGraph;
import com.example.springai.langgraph.model.WorkflowResult;
import com.example.springai.langgraph.service.LangGraphService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LangGraph concepts: {@link GraphState}, {@link StateGraph},
 * {@link CompiledGraph}, and {@link LangGraphService}.
 */
@ExtendWith(MockitoExtension.class)
class LangGraphServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private LangGraphService langGraphService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        langGraphService = new LangGraphService(chatClientBuilder);
    }

    // -------------------------------------------------------------------------
    // GraphState
    // -------------------------------------------------------------------------

    @Test
    void graphState_shouldStoreAndRetrieveValues() {
        GraphState state = new GraphState()
                .with("key1", "value1")
                .with("key2", 42);

        assertThat(state.getString("key1")).isEqualTo("value1");
        assertThat(state.<Integer>get("key2")).isEqualTo(42);
        assertThat(state.getString("missing")).isEmpty();
    }

    @Test
    void graphState_with_shouldReturnNewInstance() {
        GraphState original = new GraphState().with("key", "original");
        GraphState updated = original.with("key", "updated");

        assertThat(original.getString("key")).isEqualTo("original");
        assertThat(updated.getString("key")).isEqualTo("updated");
    }

    @Test
    void graphState_has_shouldReflectPresence() {
        GraphState state = new GraphState().with("present", "yes");
        assertThat(state.has("present")).isTrue();
        assertThat(state.has("absent")).isFalse();
    }

    // -------------------------------------------------------------------------
    // StateGraph / CompiledGraph
    // -------------------------------------------------------------------------

    @Test
    void stateGraph_withoutEntryPoint_shouldThrow() {
        assertThatThrownBy(() -> new StateGraph("no-entry")
                .addNode("node1", s -> s)
                .compile())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no entry point");
    }

    @Test
    void stateGraph_withUnknownEntryPoint_shouldThrow() {
        assertThatThrownBy(() -> new StateGraph("bad-entry")
                .setEntryPoint("nonexistent")
                .compile())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a registered node");
    }

    @Test
    void compiledGraph_shouldExecuteNodesInOrder() {
        CompiledGraph graph = new StateGraph("linear")
                .addNode("step1", state -> state.with("log", "step1"))
                .addNode("step2", state -> state.with("log", state.getString("log") + ",step2"))
                .addNode("step3", state -> state.with("log", state.getString("log") + ",step3"))
                .setEntryPoint("step1")
                .addEdge("step1", "step2")
                .addEdge("step2", "step3")
                .addEdge("step3", StateGraph.END)
                .compile();

        GraphState result = graph.invoke(new GraphState());

        assertThat(result.getString("log")).isEqualTo("step1,step2,step3");
    }

    @Test
    void compiledGraph_shouldFollowConditionalEdge() {
        CompiledGraph graph = new StateGraph("conditional")
                .addNode("router", state -> state.with("route", "path-b"))
                .addNode("path-a", state -> state.with(GraphState.OUTPUT_KEY, "took path A"))
                .addNode("path-b", state -> state.with(GraphState.OUTPUT_KEY, "took path B"))
                .setEntryPoint("router")
                .addConditionalEdge("router", state ->
                        "path-a".equals(state.getString("route")) ? "path-a" : "path-b")
                .addEdge("path-a", StateGraph.END)
                .addEdge("path-b", StateGraph.END)
                .compile();

        GraphState result = graph.invoke(new GraphState());

        assertThat(result.getString(GraphState.OUTPUT_KEY)).isEqualTo("took path B");
    }

    @Test
    void compiledGraph_shouldRecordExecutionPath() {
        CompiledGraph graph = new StateGraph("path-tracking")
                .addNode("a", state -> state)
                .addNode("b", state -> state)
                .setEntryPoint("a")
                .addEdge("a", "b")
                .addEdge("b", StateGraph.END)
                .compile();

        GraphState result = graph.invoke(new GraphState());

        assertThat(result.<java.util.List<String>>get("__execution_path__"))
                .containsExactly("a", "b");
    }

    @Test
    void compiledGraph_shouldRespectMaxStepLimit() {
        // Create a graph that cycles indefinitely (no FINISH condition)
        CompiledGraph graph = new StateGraph("infinite-loop")
                .addNode("loop", state -> state)
                .setEntryPoint("loop")
                .addEdge("loop", "loop")
                .compile();

        assertThatThrownBy(() -> graph.invoke(new GraphState()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maximum step limit");
    }

    // -------------------------------------------------------------------------
    // LangGraphService – Routing Workflow
    // -------------------------------------------------------------------------

    @Test
    void routingWorkflow_question_shouldRouteToHandleQuestion() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content())
                .thenReturn("QUESTION")
                .thenReturn("The capital of France is Paris.");

        WorkflowResult result = langGraphService.runRoutingWorkflow("What is the capital of France?");

        assertThat(result.output()).isEqualTo("The capital of France is Paris.");
        assertThat(result.executionPath()).containsExactly("classify_intent", "handle_question");
    }

    @Test
    void routingWorkflow_task_shouldRouteToHandleTaskRequest() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content())
                .thenReturn("TASK")
                .thenReturn("1. Open your IDE\n2. Create a new project\n3. Add dependencies");

        WorkflowResult result = langGraphService.runRoutingWorkflow("Set up a Spring Boot project.");

        assertThat(result.executionPath()).containsExactly("classify_intent", "handle_task_request");
        assertThat(result.output()).contains("1.");
    }

    // -------------------------------------------------------------------------
    // LangGraphService – Pipeline Workflow
    // -------------------------------------------------------------------------

    @Test
    void pipelineWorkflow_shouldExecuteAllThreeStages() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content())
                .thenReturn("Apple\nTim Cook\nCupertino")
                .thenReturn("POSITIVE - The announcement is positive news.")
                .thenReturn("Report: Positive announcement involving Apple, Tim Cook, and Cupertino.");

        WorkflowResult result = langGraphService.runPipelineWorkflow(
                "Apple CEO Tim Cook announced new products in Cupertino.");

        assertThat(result.executionPath())
                .containsExactly("extract_entities", "analyse_sentiment", "generate_report");
        assertThat(result.finalState()).containsKey("entities");
        assertThat(result.finalState()).containsKey("sentiment");
        assertThat(result.output()).contains("Report:");
    }
}
