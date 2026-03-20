package com.gen.ai.model;

import java.util.List;

public class RagRetrievalResponse {

    private String question;
    private int requestedTopK;
    private int effectiveTopK;

    // The filter that was applied
    private double minScoreThreshold;

    // The actual chunks of truth/knowledge
    private List<RetrievedChunk> chunks;

    public RagRetrievalResponse() {
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public int getRequestedTopK() {
        return requestedTopK;
    }

    public void setRequestedTopK(int requestedTopK) {
        this.requestedTopK = requestedTopK;
    }

    public int getEffectiveTopK() {
        return effectiveTopK;
    }

    public void setEffectiveTopK(int effectiveTopK) {
        this.effectiveTopK = effectiveTopK;
    }

    public double getMinScoreThreshold() {
        return minScoreThreshold;
    }

    public void setMinScoreThreshold(double minScoreThreshold) {
        this.minScoreThreshold = minScoreThreshold;
    }

    public List<RetrievedChunk> getChunks() {
        return chunks;
    }

    public void setChunks(List<RetrievedChunk> chunks) {
        this.chunks = chunks;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RagRetrievalResponse target = new RagRetrievalResponse();

        public Builder question(String question) {
            target.setQuestion(question);
            return this;
        }

        public Builder requestedTopK(int requestedTopK) {
            target.setRequestedTopK(requestedTopK);
            return this;
        }

        public Builder effectiveTopK(int effectiveTopK) {
            target.setEffectiveTopK(effectiveTopK);
            return this;
        }

        public Builder minScoreThreshold(double minScoreThreshold) {
            target.setMinScoreThreshold(minScoreThreshold);
            return this;
        }

        public Builder chunks(List<RetrievedChunk> chunks) {
            target.setChunks(chunks);
            return this;
        }

        public RagRetrievalResponse build() {
            return target;
        }
    }

}
