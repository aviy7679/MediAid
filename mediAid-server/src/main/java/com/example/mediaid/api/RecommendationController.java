//package com.example.mediaid.api;
//
//import com.example.mediaid.bl.emergency.SymptomAnalysisService;
//import com.example.mediaid.bl.emergency.TreatmentRecommendationEngine;
//import com.example.mediaid.dto.emergency.ExtractedSymptom;
//import com.example.mediaid.security.jwt.JwtUtil;
//import jakarta.servlet.http.HttpServletRequest;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//
//import java.util.*;
//
//@RestController
//@RequestMapping("/api")
//@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST})
//public class RecommendationController {
//
//    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);
//
//    @Autowired
//    private SymptomAnalysisService symptomAnalysisService;
//
//    @Autowired
//    private TreatmentRecommendationEngine treatmentEngine;
//
//    @Autowired
//    private JwtUtil jwtUtil;
//
//    //מתודת עזר לשליפת מזהה משתמש מתוך הבקשה
//    private UUID extractUserIdFromRequest(HttpServletRequest request) {
//        try{
//            String authHeader = request.getHeader("Authorization");
//            if(authHeader != null && authHeader.startsWith("Bearer ")) {
//                String token = authHeader.substring(7);
//                if(jwtUtil.isValid(token)){
//                    return jwtUtil.extractUserId(token);
//                }
//            }
//            return null;
//        }catch(Exception e) {
//            logger.error("Error extracting user ID from request", e);
//            return null;
//        }
//    }
//
//    @PostMapping("/upload-data")
//    public ResponseEntity<?> uploadData(
//        @RequestParam(value="text", required=false) String text,
//        @RequestParam(value="image", required=false) MultipartFile imageFile,
//        @RequestParam(value="audio", required=false) MultipartFile audioFile,
//        HttpServletRequest request) {
//        try{
//            UUID userId = extractUserIdFromRequest(request);
//            if(userId == null) {
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
//            }
//            logger.info("Received data upload request from user: {}", userId);
//
//            Set<String> extractedSymptoms = new LinkedHashSet<>();
//
//            //עיבוד טקסט
//            if(text != null && !text.trim().isEmpty()) {
//                logger.info("Processing text data for user: {}", userId);
//                extractedSymptoms.addAll(symptomAnalysisService.extractSymptomsFromText(text));
//            }
//
//            //עיבוד תמונה`
//            if(imageFile != null && !imageFile.isEmpty()) {
//                logger.info("Processing image data for user: {}", userId);
//                byte[] imageBytes = imageFile.getBytes();
//                List<ExtractedSymptom> imageSymptoms = symptomAnalysisService.extractedSymptomsFromImage(imageBytes);
//                imageSymptoms.forEach(symptom -> extractedSymptoms.add(symptom.getName()));            }
//
//            //עיבוד אודיו
//            if(audioFile != null && !audioFile.isEmpty()) {
//                logger.info("Processing audio data for user: {}", userId);
//                logger.info("Audio analysis is not implemented yet");
//            }
//
//            //ניתוח המצב הרפואי
////            TretmentPlan tretmentPlan = treatmentEngine.analyzeSituation(userId, new ArrayList<>(extractedSymptoms));
//
//            //החזרת התוכנית טיפול
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
////            response.put("guidelines", tretmentPlan.getGuidelines());
//            response.put("extractedSymptoms", new ArrayList<>(extractedSymptoms));
//            response.put("timestamp", System.currentTimeMillis());
//
//            logger.info("Analysis completed successfully for user: {}. Found {} symptoms.", userId, extractedSymptoms.size());
//
//            return ResponseEntity.ok(response);
//        }catch(Exception e) {
//            logger.error("Error processing data upload request", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request: " + e.getMessage());
//            }
//    }
////        מידע רפואי של המשתמש - לבדיקה*************
//    @GetMapping("/user-medical-context")
//    public ResponseEntity<?> getUserMedicalContext(HttpServletRequest request) {
//        try{
//            UUID userId = extractUserIdFromRequest(request);
//            if(userId == null) {
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
//            }
//
//            var medicalContext = treatmentEngine.getUserMedicalContext(userId);
//
//            return ResponseEntity.ok(Map.of(
//                "success",true,
//                "userId", userId,
//                "medicalContext", medicalContext
//            ));
//        }catch(Exception e){
//            logger.error("Error getting usermedical context", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error getting medical context: " + e.getMessage());
//        }
//    }
//
//    //בדיקת חיבור לשרת
//    @GetMapping("/python-health")
//    public ResponseEntity<?> checkPythonHealth() {
//        try {
//            //בדיקת חיבור לשרת פייתון
//            boolean isHealthy = symptomAnalysisService.checkPythonServerHealth();
//
//            return ResponseEntity.ok(Map.of(
//                "success", true,
//                "pythonServerHealthy", isHealthy,
//                "message", isHealthy ? "Python server is responsive" : "Python service is not responding"
//            ));
//
//        } catch (Exception e) {
//            logger.error("Error checking Python service health", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Error checking Python service health: " + e.getMessage()));
//        }
//    }
//
//
//}
//

package com.example.mediaid.api;

import com.example.mediaid.bl.emergency.SymptomAnalysisService;
import com.example.mediaid.bl.emergency.TreatmentRecommendationEngine;
import com.example.mediaid.dto.emergency.ExtractedSymptom;
import com.example.mediaid.dto.emergency.TreatmentPlan;
import com.example.mediaid.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class RecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    @Autowired
    private SymptomAnalysisService symptomAnalysisService;

    @Autowired
    private TreatmentRecommendationEngine treatmentEngine;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * העלאת נתונים לניתוח - משולב (טקסט + תמונה + אודיו)
     */
    @PostMapping("/upload-data")
    public ResponseEntity<?> uploadData(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            @RequestParam(value = "audio", required = false) MultipartFile audioFile,
            HttpServletRequest httpRequest) {

        try {
            UUID userId = extractUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            logger.info("Data upload and analysis for user: {}", userId);

            Set<ExtractedSymptom> allExtractedSymptoms = new LinkedHashSet<>();
            List<String> processedInputs = new ArrayList<>();

            // שלב 1: עיבוד טקסט
            if (text != null && !text.trim().isEmpty()) {
                logger.info("Processing text data for user: {}", userId);
                logger.info("Analyzing text: {}", text.substring(0, Math.min(50, text.length())));

                var textSymptoms = symptomAnalysisService.extractSymptomsFromText(text);
                allExtractedSymptoms.addAll(textSymptoms);
                processedInputs.add("text");

                logger.info("Extracted {} symptoms from text", textSymptoms.size());
            }

            // שלב 2: עיבוד תמונה
            if (imageFile != null && !imageFile.isEmpty()) {
                logger.info("Processing image data for user: {}", userId);
                logger.info("Analyzing image: {} (size: {} bytes)",
                        imageFile.getOriginalFilename(), imageFile.getSize());

                var imageSymptoms = symptomAnalysisService.extractedSymptomsFromImage(imageFile.getBytes());
                allExtractedSymptoms.addAll(imageSymptoms);
                processedInputs.add("image");

                logger.info("Extracted {} symptoms from image", imageSymptoms.size());
            }

            // שלב 3: עיבוד אודיו (לעתיד)
            if (audioFile != null && !audioFile.isEmpty()) {
                logger.info("Processing audio data for user: {}", userId);
                logger.info("Audio file: {} (size: {} bytes)",
                        audioFile.getOriginalFilename(), audioFile.getSize());

                // כרגע לא מיושם - להוסיף כשיהיה מוכן
                logger.warn("Audio analysis is not implemented yet");
                processedInputs.add("audio (not processed)");
            }

            // בדיקה אם נמצאו סימפטומים
            if (allExtractedSymptoms.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "No symptoms detected in the provided data",
                        "processedInputs", processedInputs,
                        "extractedSymptoms", allExtractedSymptoms,
                        "treatmentPlan", null,
                        "timestamp", System.currentTimeMillis()
                ));
            }

            // שלב 4: ניתוח המצב הרפואי וגיבוש הנחיות טיפול
            TreatmentPlan treatmentPlan = treatmentEngine.analyzeSituation(userId, allExtractedSymptoms);

            // שלב 5: החזרת התוצאות המפורטות
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("analysisType", determineAnalysisType(processedInputs));
            response.put("processedInputs", processedInputs);
            response.put("originalText", text);
            if (imageFile != null && !imageFile.isEmpty()) {
                response.put("imageFileName", imageFile.getOriginalFilename());
            }
            if (audioFile != null && !audioFile.isEmpty()) {
                response.put("audioFileName", audioFile.getOriginalFilename());
            }
            response.put("extractedSymptoms", new ArrayList<>(allExtractedSymptoms));
            response.put("treatmentPlan", treatmentPlan);
            response.put("timestamp", System.currentTimeMillis());

            logger.info("Analysis completed for user {}. Found {} symptoms, urgency level: {}",
                    userId, allExtractedSymptoms.size(), treatmentPlan.getUrgencyLevel());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error in data upload and analysis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error processing data: " + e.getMessage()));
        }
    }

    /**
     * קבלת מידע רפואי נוכחי של המשתמש (לצורך בדיקה)
     */
    @GetMapping("/user-medical-context")
    public ResponseEntity<?> getUserMedicalContext(HttpServletRequest httpRequest) {
        try {
            UUID userId = extractUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            var medicalContext = treatmentEngine.getUserMedicalContext(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId", userId,
                    "medicalContext", medicalContext
            ));

        } catch (Exception e) {
            logger.error("Error getting user medical context", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error getting medical context: " + e.getMessage()));
        }
    }

    /**
     * בדיקת חיבור לשרת Python
     */
    @GetMapping("/python-health")
    public ResponseEntity<?> checkPythonHealth() {
        try {
            boolean isHealthy = symptomAnalysisService.checkPythonServerHealth();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "pythonServerHealthy", isHealthy,
                    "message", isHealthy ? "Python server is responsive" : "Python server is not responding"
            ));

        } catch (Exception e) {
            logger.error("Error checking Python server health", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error checking Python server: " + e.getMessage()));
        }
    }

    // Helper methods
    private UUID extractUserIdFromRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtil.isValid(token)) {
                    return jwtUtil.extractUserId(token);
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("Error extracting userId from token", e);
            return null;
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "DATA_ANALYSIS_ERROR");
        error.put("message", message);
        return error;
    }

    private String determineAnalysisType(List<String> processedInputs) {
        if (processedInputs.size() == 1) {
            return processedInputs.get(0);
        } else if (processedInputs.size() > 1) {
            return "combined";
        }
        return "none";
    }
}