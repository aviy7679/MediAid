package com.example.mediaid.bl.emegency;

import com.example.mediaid.dto.emergency.ExtractedSymptom;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SymptomAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(SymptomAnalysisService.class);

    @Value("${python.server.url:http://localhost:5000}")
    private String pythonServerUrl;

    @Value("${python.server.timeout:30000}")
    private int timeoutMs;

    //תקשורת REST עם שרתים אחרים
    private final RestTemplate restTemplate;

    //המרות JAVA ופורמטים כמו JSON
    private final ObjectMapper objectMapper;


    // Enum להגדרת סוגי מקורות הסימפטומים
    public enum SymptomSourceType {
        TEXT("text", "MedCAT", "Unknown", "accuracy", "context_similarity", "detected_name", true),
        IMAGE("image", "BiomedCLIP", "Present", "confidence", "probability", "name", false);

        private final String source;
        private final String analyzerType;
        private final String defaultStatus;
        private final String confidenceField;
        private final String probabilityField;
        private final String detectedNameField;
        private final boolean hasPositions;

        SymptomSourceType(String source, String analyzerType, String defaultStatus,
                          String confidenceField, String probabilityField,
                          String detectedNameField, boolean hasPositions) {
            this.source = source;
            this.analyzerType = analyzerType;
            this.defaultStatus = defaultStatus;
            this.confidenceField = confidenceField;
            this.probabilityField = probabilityField;
            this.detectedNameField = detectedNameField;
            this.hasPositions = hasPositions;
        }

        // Getters
        public String getSource() { return source; }
        public String getAnalyzerType() { return analyzerType; }
        public String getDefaultStatus() { return defaultStatus; }
        public String getConfidenceField() { return confidenceField; }
        public String getProbabilityField() { return probabilityField; }
        public String getDetectedNameField() { return detectedNameField; }
        public boolean isHasPositions() { return hasPositions; }
    }

    public SymptomAnalysisService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }


    //חילוץ מטקסט
    public List<ExtractedSymptom> extractSymptomFromText(String text){
        try{
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            //אובייקט לבקשת HTTP
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            //שליחת הבקשה
            String url = pythonServerUrl + "/text/analyze";
            ResponseEntity<String> response  = restTemplate.postForEntity(url, request, String.class);

            if(response.getStatusCode() == HttpStatus.OK){
                return
            }
        }
    }

    private List<ExtractedSymptom> parseSymptoms (String jsonResponse, boolean isText){
        List<ExtractedSymptom> symptoms = new ArrayList<>();

        try{
            JsonNode root = objectMapper.readTree(jsonResponse);

            if(!root.path("success").asBoolean()){
                String sourceType = isText ? "text" : "image";
                logger.error("Python server returned error for {}: {}", sourceType, root.path("error").asText());
                return symptoms;
            }

            JsonNode symptomsArray = root.path("data").path("symptoms");

        }
    }
}








