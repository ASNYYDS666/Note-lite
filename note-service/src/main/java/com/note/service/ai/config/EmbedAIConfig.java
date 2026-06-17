package com.note.service.ai.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbedAIConfig {

    private String provider;
    private String apiKey;
    private String model;
    private String baseUrl;
}
