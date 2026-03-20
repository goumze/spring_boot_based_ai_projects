package com.example.springai.langchain.model;

import java.util.List;
import java.util.Map;

/**
 * The result produced by executing a LangChain chain.
 *
 * @param chainName     the name of the chain that produced this result
 * @param input         the input variables that were passed to the chain
 * @param output        the final text output from the LLM
 * @param intermediates named intermediate outputs produced by inner chains (for sequential chains)
 */
public record ChainResult(
        String chainName,
        Map<String, String> input,
        String output,
        List<String> intermediates) {

    /** Creates a simple result with no intermediate steps. */
    public static ChainResult of(String chainName, Map<String, String> input, String output) {
        return new ChainResult(chainName, input, output, List.of());
    }
}
