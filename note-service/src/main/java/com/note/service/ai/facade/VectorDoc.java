package com.note.service.ai.facade;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VectorDoc {
    private String id;
    private List<Float> vector;
    private Map<String, Object> payload;
    private Double score;
}
