package com.gen.ai.model;

import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

/*
 * score       --> cosine similarity (higher value = more relevant)
 * rank        --> 1 = best match
 * metadata    --> document_name, upload_timestamp, file_type, etc.
 * textLength  --> characters — sometimes useful for confidence heuristics
 */
public class RetrievedChunk {

    private String text;
    private double score;
    private int rank;
    private Map<String, String> metadata;

    @JsonIgnore
    private Map<String, Object> metadataObj;

    private int textLength;

    public RetrievedChunk(String text) {
        this.text = text;
        this.score = 0.0;
        this.rank = 0;
        this.metadata = null;
        this.textLength = ((text != null) ? text.length() : 0);
    }

    public RetrievedChunk(String text, double score, int rank, Map<String, Object> metadataObj) {
        this.text = text;
        this.score = score;
        this.rank = rank;
        this.metadataObj = metadataObj;
        if (metadataObj != null) {
            this.metadata = metadataObj.entrySet().stream()
                    .filter(e -> e.getKey() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> (e.getValue() == null) ? null : String.valueOf(e.getValue())));
        } else {
            this.metadata = null;
        }
        this.textLength = ((text != null) ? text.length() : 0);
    }

    public RetrievedChunk(String text, double score, int rank, Map<String, Object> metadataObj, int textLength) {
        this.text = text;
        this.score = score;
        this.rank = rank;
        this.metadataObj = metadataObj;
        if (metadataObj != null) {
            this.metadata = metadataObj.entrySet().stream()
                    .filter(e -> e.getKey() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> (e.getValue() == null) ? null : String.valueOf(e.getValue())));
        } else {
            this.metadata = null;
        }
        this.textLength = textLength;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Map<String, Object> getMetadataObj() {
        return metadataObj;
    }

    public void setMetadataObj(Map<String, Object> metadataObj) {
        this.metadataObj = metadataObj;
    }

    public int getTextLength() {
        return textLength;
    }

    public void setTextLength(int textLength) {
        this.textLength = textLength;
    }

}
