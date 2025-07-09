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

    @Value("${python.server.timeout:" + PYTHON_SERVER_TIMEOUT + "}")
    private int timeoutMs;

    //תקשורת REST עם שרתים אחרים
    private final RestTemplate restTemplate;

    //המרות JAVA ופורמטים כמו JSON
    private final ObjectMapper objectMapper;

    public SymptomAnalysisService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    //חילוץ מטקסט
    public List<ExtractedSymptom> extractSymptomsFromText(String text){
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
                return parseSymptoms(response.getBody(),true);
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
            logger.error("Unexpected error occurred while extracting symptoms from text analysis: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    //חילוץ מתמונה
    public List<ExtractedSymptom> extractedSymptomsFromImage(byte[] image){
        logger.info("Extracting symptoms from image of size {}", image.length);
        try{
            //המרה לBase64
            String base64Image = Base64.getEncoder().encodeToString(image);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image", base64Image);
            requestBody.put("min_confidence", MIN_SYMPTOM_CONFIDENCE); //סף דיפולטיבי

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String url = pythonServerUrl + "/image/analyze";
            ResponseEntity<String> response  = restTemplate.postForEntity(url, request, String.class);

            if(response.getStatusCode() == HttpStatus.OK){
                return parseSymptoms(response.getBody(),false);
            }else {
                logger.error("Python server returned error status: {}", response.getStatusCode());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("Error in image extraction: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    //בדיקת תקינות השרת - פייתון
    public boolean checkPythonServerHealth() {
        try {
            String url = pythonServerUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode healthResponse = objectMapper.readTree(response.getBody());
                boolean ready = healthResponse.path("ready").asBoolean(false);
                logger.info("Python server health check: ready={}", ready);
                return ready;
            }

            return false;

        } catch (Exception e) {
            logger.error("Python server health check failed: {}", e.getMessage());
            return false;
        }
    }

    //תרגום התשובה לאובייקט
    private List<ExtractedSymptom> parseSymptoms (String jsonResponse, boolean isText){
        List<ExtractedSymptom> symptoms = new ArrayList<>();

        try{
            JsonNode root = objectMapper.readTree(jsonResponse);

            if(!root.path("success").asBoolean()){
                String sourceType = isText ? "text" : "image";
                logger.error("Python server returned error for {}: {}", sourceType, root.path("error").asText());
                return symptoms;
            }

            JsonNode symptomsArray = root.path("symptoms");

            for (JsonNode symptomNode : symptomsArray) {
                ExtractedSymptom symptom = new ExtractedSymptom();

                symptom.setCui(symptomNode.path("cui").asText());
                symptom.setName(symptomNode.path("name").asText());
                symptom.setDetectedName(symptomNode.path("detected_name").asText());

                //הגדרות ספציפיות לטקסט
                if(isText){
                    symptom.setConfidence(symptomNode.path("accuracy").asDouble());
                    symptom.setProbability(symptomNode.path("context_similarity").asDouble());
                    symptom.setStartPosition(symptomNode.path("start_position").asInt());
                    symptom.setEndPosition(symptomNode.path("end_position").asInt());
                    symptom.setStatus(symptomNode.path("status").asText("Unknown"));
                    symptom.setSource("text");
                    symptom.setAnalyzerType("MedCAT");
                }else{
                    symptom.setConfidence(symptomNode.path("confidence").asDouble());
                    symptom.setProbability(symptomNode.path("probability").asDouble());
                    symptom.setStartPosition(null);
                    symptom.setEndPosition(null);
                    symptom.setStatus("Present");
                    symptom.setSource("image");
                    symptom.setAnalyzerType("BiomedCLIP");
                }

                // סינון סימפטומים עם רמת ביטחון נמוכה מדי
                if (symptom.getConfidence() >= MIN_SYMPTOM_CONFIDENCE) {
                    symptoms.add(symptom);
                }
            }

            String sourceType = isText ? "text" : "image";
            logger.info("Parsed {} symptoms from {} (after filtering)", symptoms.size(), sourceType);

        } catch (Exception e) {
            String sourceType = isText ? "text" : "image";
            logger.error("Failed to parse {} symptoms: {}",sourceType,e.getMessage(), e);
        }

        return symptoms;
    }
}