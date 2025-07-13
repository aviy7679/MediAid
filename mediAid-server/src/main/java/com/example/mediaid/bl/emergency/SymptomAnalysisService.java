package com.example.mediaid.bl.emergency;

import com.example.mediaid.dto.emergency.ExtractedSymptom;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static com.example.mediaid.constants.ApiConstants.*;
import static com.example.mediaid.constants.MedicalAnalysisConstants.*;

@Service
public class SymptomAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(SymptomAnalysisService.class);

    @Value("${python.server.url:http://localhost:5000}")
    private String pythonServerUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SymptomAnalysisService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<ExtractedSymptom> extractSymptomsFromText(String text){
        logger.info("Extracting symptoms from text: {}", text.substring(0, Math.min(50, text.length())) + "...");
        try{
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String url = pythonServerUrl + "/text/analyze";
            logger.info("Sending request to: {}", url);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            logger.info("Received response with status: {}", response.getStatusCode());
            logger.debug("Response body: {}", response.getBody());

            if(response.getStatusCode() == HttpStatus.OK){
                return parseSymptoms(response.getBody(), true);
            }else {
                logger.error("Python server returned error status: {}", response.getStatusCode());
                return new ArrayList<>();
            }
        }catch(HttpClientErrorException e){
            logger.error("HTTP error calling Python server for text analysis: {}", e.getResponseBodyAsString());
            return new ArrayList<>();
        }catch(ResourceAccessException e){
            logger.error("Python server not accessible for text analysis: {}", e.getMessage());
            return new ArrayList<>();
        }catch(Exception e){
            logger.error("Unexpected error occurred while extracting symptoms from text analysis: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<ExtractedSymptom> extractedSymptomsFromImage(byte[] image){
        logger.info("Extracting symptoms from image of size {}", image.length);
        try{
            String base64Image = Base64.getEncoder().encodeToString(image);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image", base64Image);
            requestBody.put("min_confidence", MIN_SYMPTOM_CONFIDENCE);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String url = pythonServerUrl + "/image/analyze";
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if(response.getStatusCode() == HttpStatus.OK){
                return parseSymptoms(response.getBody(), false);
            }else {
                logger.error("Python server returned error status: {}", response.getStatusCode());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("Error in image extraction: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    private List<ExtractedSymptom> parseSymptoms(String jsonResponse, boolean isText){
        List<ExtractedSymptom> symptoms = new ArrayList<>();

        try{
            logger.debug("Parsing symptoms from JSON: {}", jsonResponse);
            JsonNode root = objectMapper.readTree(jsonResponse);

            if(!root.path("success").asBoolean()){
                String sourceType = isText ? "text" : "image";
                logger.error("Python server returned error for {}: {}", sourceType, root.path("error").asText());
                return symptoms;
            }

            JsonNode symptomsArray = root.path("symptoms");
            logger.info("Found {} symptoms in response", symptomsArray.size());

            for (JsonNode symptomNode : symptomsArray) {
                ExtractedSymptom symptom = new ExtractedSymptom();

                // Basic fields that are the same for both text and image
                symptom.setCui(symptomNode.path("cui").asText());
                symptom.setName(symptomNode.path("name").asText());
                symptom.setDetectedName(symptomNode.path("detected_name").asText());

                if(isText){
                    // ✅ FIXED: Updated field names to match Python server response
                    symptom.setConfidence(symptomNode.path("confidence").asDouble()); // Was: "accuracy"

                    // Set probability to confidence if context_similarity doesn't exist
                    double probability = symptomNode.path("context_similarity").asDouble(
                            symptomNode.path("confidence").asDouble()
                    );
                    symptom.setProbability(probability);

                    // ✅ FIXED: Updated field names
                    symptom.setStartPosition(symptomNode.path("start").asInt()); // Was: "start_position"
                    symptom.setEndPosition(symptomNode.path("end").asInt());     // Was: "end_position"

                    // Use match_type as status if available, otherwise default
                    String status = symptomNode.path("match_type").asText(
                            symptomNode.path("status").asText("Present")
                    );
                    symptom.setStatus(status);

                    symptom.setSource("text");

                    // Use the actual analyzer type from response if available
                    String analyzerType = symptomNode.path("source").asText("keyword_analyzer");
                    symptom.setAnalyzerType(analyzerType);

                }else{
                    // Image analysis fields (these seem correct already)
                    symptom.setConfidence(symptomNode.path("confidence").asDouble());
                    symptom.setProbability(symptomNode.path("probability").asDouble());
                    symptom.setStartPosition(null);
                    symptom.setEndPosition(null);
                    symptom.setStatus("Present");
                    symptom.setSource("image");
                    symptom.setAnalyzerType("BiomedCLIP");
                }

                // ✅ IMPROVED: Better logging for debugging
                logger.debug("Parsed symptom: {} (CUI: {}, Confidence: {:.2f})",
                        symptom.getName(), symptom.getCui(), symptom.getConfidence());

                // Filter symptoms with low confidence
                if (symptom.getConfidence() >= MIN_SYMPTOM_CONFIDENCE) {
                    symptoms.add(symptom);
                    logger.debug("Added symptom: {} (confidence {:.2f} >= threshold {:.2f})",
                            symptom.getName(), symptom.getConfidence(), MIN_SYMPTOM_CONFIDENCE);
                } else {
                    logger.debug("Filtered out symptom: {} (confidence {:.2f} < threshold {:.2f})",
                            symptom.getName(), symptom.getConfidence(), MIN_SYMPTOM_CONFIDENCE);
                }
            }

            String sourceType = isText ? "text" : "image";
            logger.info("Successfully parsed {} symptoms from {} (after filtering with threshold {:.2f})",
                    symptoms.size(), sourceType, MIN_SYMPTOM_CONFIDENCE);

        } catch (Exception e) {
            String sourceType = isText ? "text" : "image";
            logger.error("Failed to parse {} symptoms: {}", sourceType, e.getMessage(), e);
            logger.error("JSON response was: {}", jsonResponse);
        }

        return symptoms;
    }
}