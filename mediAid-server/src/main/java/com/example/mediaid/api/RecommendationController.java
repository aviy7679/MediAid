package com.example.mediaid.api;

import com.example.mediaid.bl.emergency.SymptomAnalysisService;
import com.example.mediaid.bl.emergency.TreatmentRecommendationEngine;
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

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedMethods = {RequestMethod.GET, RequestMethod.POST})
public class RecommendationController {

    privat static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    @Awtoride
    private SymptomAnalysisService symptomAnalysisService;

    @Awtoride
    private TreatmentRecommendationEngin treatmentEngine;

    @Awtoride
    private JwtUtil jwtUtil;

    //מתודת עזר לשליפת מזהה משתמש מתוך הבקשה
    private UUID extractUserIdFromRequest(HttpServletRequest request) {
        try{
            String authHeader = request.getHeader("Authorization");
            if(authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if(jwtUtil.isValid(token)){
                    return jwtUtil.extractUserId(token);
                }
            }
            return null;
        }catch(Exception e) {
            logger.error("Error extracting user ID from request", e);
            return null;
        }
    }

    @PostMapping("/upload-data")
    public ResponseEntity<?> uploadData(
        @RequestParam(value="text", required=false) String text,
        @RequestParam(value="image", required=false) MultipartFile imageFile,
        @RequestParam(value="audio", required=false) MultipartFile audioFile,
        HttpServletRequest request) {
        try{
            UUID userId = extractUserIdFromRequest(request);
            if(userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
            }
            logger.info("Received data upload request from user: {}", userId);

            Set<String> extractedSymptoms = new LinkedHashSet<>();
            
            //עיבוד טקסט
            if(text != null && !text.trim().isEmpty()) {
                logger.info("Processing text data for user: {}", userId);
                extractedSymptoms.addAll(symptomAnalysisService.extractSymptomsFromText(text));
            }

            //עיבוד תמונה`
            if(imageFile != null && !imageFile.isEmpty()) {
                logger.info("Processing image data for user: {}", userId);
                extractedSymptoms.addAll(symptomAnalysisService.extractSymptomsFromImage(imageFile));
            }

            //עיבוד אודיו
            if(audioFile != null && !audioFile.isEmpty()) {
                logger.info("Processing audio data for user: {}", userId);
                logger.info("Audio analysis is not implemented yet");
            }

            //ניתוח המצב הרפואי
            TretmentPlan tretmentPlan = treatmentEngine.analyzeSituation(userId, new ArrayList<>(extractedSymptoms));

            //החזרת התוכנית טיפול
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("guidelines", tretmentPlan.getGuidelines());
            response.put("extractedSymptoms", new ArrayList<>(extractedSymptoms));
            response.put("timestamp", System.currentTimeMillis());

            logger.info("Analysis completed successfully for user: {}. Found {} symptoms, urgency level: {}", userId, extractedSymptoms.size(), tretmentPlan.getUrgencyLevel());

            return ResponseEntity.ok(response);
        }catch(Exception e) {
            logger.error("Error processing data upload request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request: " + e.getMessage());
            }
    }
        //מידע רפואי של המשתמש - לבדיקה*************
    @GetMapping("/user-medical-context")
    public ResponseEntity<?> getUserMedicalContext(HttpServletRequest request) {
        try{
            UUID userId = extractUserIdFromRequest(request);
            if(userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
            }

            var medicalContext = treatmentEngine.getUserMedicalContext(userId);

            return ResponseEntity.ok(Map.of(
                "success",true,
                "userId", userId,
                "medicalContext", medicalContext
            ));
        }catch(Exception e){
            logger.error("Error getting usermedical context", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error getting medical context: " + e.getMessage());
        }
    }

    //בדיקת חיבור לשרת
    @GetMapping("/python-health")
    public ResponseEntity<?> checkPythonHealth() {
        try {
            //בדיקת חיבור לשרת פייתון
            boolean isHealthy = symptomAnalysisService.checkPythonServerHealth();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "pythonServerHealthy", isHealthy,
                "message", isHealthy ? "Python server is responsive" : "Python service is not responding"
            ))

        } catch (Exception e) {
            logger.error("Error checking Python service health", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Error checking Python service health: " + e.getMessage()));
        }
    }


}

