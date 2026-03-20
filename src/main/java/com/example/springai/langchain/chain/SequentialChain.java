package com.example.springai.langchain.chain;

import com.example.springai.langchain.model.ChainResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sequential Chain – runs multiple {@link LLMChain} instances in order, piping the output of each
 * step as input to the next.
 *
 * <p>This mirrors LangChain's {@code SequentialChain} pattern. Each chain in the sequence receives
 * all variables accumulated so far, including the outputs of previous chains.
 *
 * <p>Example use-case: translate text → summarise the translation → critique the summary.
 */
public class SequentialChain {

    private final String name;
    private final List<LLMChain> chains;

    /**
     * The output key under which each chain's result is stored before being forwarded to the next
     * chain. The key follows the pattern {@code step_N_output} (1-based).
     */
    private static final String OUTPUT_KEY_PATTERN = "step_%d_output";

    public SequentialChain(String name, List<LLMChain> chains) {
        this.name = name;
        this.chains = List.copyOf(chains);
    }

    /**
     * Executes every chain in sequence.
     *
     * <p>The initial {@code input} variables are made available to all chains. Each chain's output
     * is stored under {@code step_N_output} (1-based N) and forwarded to subsequent chains.
     *
     * @param input the initial set of variables
     * @return a {@link ChainResult} containing the final output and all intermediate outputs
     */
    public ChainResult run(Map<String, String> input) {
        Map<String, String> accumulated = new HashMap<>(input);
        List<String> intermediates = new ArrayList<>();
        String lastOutput = "";

        for (int i = 0; i < chains.size(); i++) {
            ChainResult stepResult = chains.get(i).run(accumulated);
            lastOutput = stepResult.output();
            intermediates.add(stepResult.output());
            accumulated.put(String.format(OUTPUT_KEY_PATTERN, i + 1), lastOutput);
        }

        return new ChainResult(name, input, lastOutput, intermediates);
    }

    public String getName() {
        return name;
    }

    public List<LLMChain> getChains() {
        return chains;
    }
}
