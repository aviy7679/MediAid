//package com.example.mediaid.api;
//
//import com.example.mediaid.bl.RiskFactorService;
//import com.example.mediaid.dto.RiskFactorResponseDTO;
//import com.example.mediaid.dto.RiskFactorUpdateDTO;
//import com.example.mediaid.security.jwt.JwtUtil;
//import jakarta.servlet.http.HttpServletRequest;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/user")
//@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
//public class RiskFactorController {
//
//    @Autowired
//    private RiskFactorService riskFactorService;
//
//    @Autowired
//    private JwtUtil jwtUtil;
//
//    Logger logger = LoggerFactory.getLogger(RiskFactorController.class);
//
//    @PostMapping("/risk-factors")
//    public ResponseEntity<?> createRiskFactor(@RequestBody RiskFactorUpdateDTO dto, HttpServletRequest request) {
//        try{
//            UUID userId = extractUserIdFromRequest(request);
//            if(userId == null){
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body(createErrorResponse("Invalid or missing authorization token"));
//            }
//            logger.info("Updating risk factor for user {}.");
//
//            RiskFactorResponseDTO response = riskFactorService.updateUserRiskFactors(userId, dto);
//
//            return ResponseEntity.ok(response);
//
//        }catch (Exception e){
//            logger.warn("Error updating risk factor", e);
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error updating risk factor: " + e.getMessage());
//        }
//    }
//
//    //קבלת גורמי סיכון של המשתמש
//    @GetMapping("/risk-factors")
//    public ResponseEntity<?>getUserRiskFactors(HttpServletRequest request) {
//        try {
//            UUID userId = extractUserIdFromRequest(request);
//            if(userId == null){
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse("Invalid or missing authorization token"));
//            }
//            logger.info("Retrieving risk factors for user {}.", userId);
//
//            RiskFactorResponseDTO response = riskFactorService.getUserRiskFactors(userId);
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            logger.warn("Error retrieving risk factors", e);
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error retrieving risk factors: "+e.getMessage()));
//        }
//    }
//
//    //חילוץ מזהה המשתמש מהטוקן בבקשה
//    private UUID extractUserIdFromRequest(HttpServletRequest request) {
//        try {
//            String authHeader = request.getHeader("Authorization");
//            if (authHeader != null && authHeader.startsWith("Bearer ")) {
//                String token = authHeader.substring(7);
//
//                if (jwtUtil.isValid(token)) {
//                    UUID userId = jwtUtil.extractUserId(token);
//                    logger.info(("Extracted user id: " + userId));
//                    return userId;
//                }
//            }
//            logger.warn("No valid authorization header found");
//            return null;
//        } catch (Exception e) {
//            logger.error("Error extracting userId from token: " + e.getMessage());
//            return null;
//        }
//    }
//
//    //יצירת תגובת שגיאה
//    private Map<String, String> createErrorResponse(String error) {
//        Map<String, String> response = new HashMap<>();
//        response.put("error", "RISK_FACTOR_ERROR");
//        response.put("message", error);
//        return response;
//    }
//
//}
// RiskFactorController.java - גרסה מתוקנת
package com.example.mediaid.api;

import com.example.mediaid.bl.RiskFactorService;
import com.example.mediaid.dto.RiskFactorResponseDTO;
import com.example.mediaid.dto.RiskFactorUpdateDTO;
import com.example.mediaid.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/risk")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
public class RiskFactorController {

    @Autowired
    private RiskFactorService riskFactorService;

    @Autowired
    private JwtUtil jwtUtil;

    Logger logger = LoggerFactory.getLogger(RiskFactorController.class);

    // POST /api/user/risk-factors
    @PostMapping("/risk-factors")
    public ResponseEntity<?> createRiskFactor(@RequestBody RiskFactorUpdateDTO dto, HttpServletRequest request) {
        try{
            UUID userId = extractUserIdFromRequest(request);
            if(userId == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }
            logger.info("Updating risk factor for user {}.", userId);

            RiskFactorResponseDTO response = riskFactorService.updateUserRiskFactors(userId, dto);

            return ResponseEntity.ok(response);

        }catch (Exception e){
            logger.warn("Error updating risk factor", e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating risk factor: " + e.getMessage());
        }
    }

    // GET /api/user/risk-factors - קבלת גורמי סיכון של המשתמש
    @GetMapping("/risk-factors")
    public ResponseEntity<?>getUserRiskFactors(HttpServletRequest request) {
        try {
            UUID userId = extractUserIdFromRequest(request);
            if(userId == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse("Invalid or missing authorization token"));
            }
            logger.info("Retrieving risk factors for user {}.", userId);

            RiskFactorResponseDTO response = riskFactorService.getUserRiskFactors(userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.warn("Error retrieving risk factors", e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error retrieving risk factors: "+e.getMessage()));
        }
    }

    // חילוץ מזהה המשתמש מהטוקן בבקשה
    private UUID extractUserIdFromRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                if (jwtUtil.isValid(token)) {
                    UUID userId = jwtUtil.extractUserId(token);
                    logger.info(("Extracted user id: " + userId));
                    return userId;
                }
            }
            logger.warn("No valid authorization header found");
            return null;
        } catch (Exception e) {
            logger.error("Error extracting userId from token: " + e.getMessage());
            return null;
        }
    }

    // יצירת תגובת שגיאה
    private Map<String, String> createErrorResponse(String error) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "RISK_FACTOR_ERROR");
        response.put("message", error);
        return response;
    }

}