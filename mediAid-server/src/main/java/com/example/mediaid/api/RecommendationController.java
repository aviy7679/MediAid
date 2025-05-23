//package com.example.mediaid.api;
//
//import com.example.mediaid.bl.RecommendationService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("api/medical")
//public class RecommendationController {
//
//    private RecommendationService recommendationService;
//
//    @Autowired
//    public RecommendationController(RecommendationService recommendationService) {
//        this.recommendationService = recommendationService;
//    }
//
//    @PostMapping("/analyze-symptoms")
//    public ResponseEntity<Map<String, Object>> analyzeSymptoms(@RequestBody List<String> symptomCuis) {
//        //מציאת מחלות לפי הסימפטום
//        List<Map<String, Object>> possibleDiseases = recommendationService.findPossibleDisease(symptomCuis);
//
//        Map<String, Object> response = Map.of(
//                "possibleDiseases", possibleDiseases
//        );
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/recommendations/{diseaseCui}")
//    public ResponseEntity<Map<String, Object>> getMedicationRecommendations(@PathVariable String diseaseCui){
//        List<Map<String, Object>> recommendedMedication =
//                recommendationService.findRecommendedMedications(diseaseCui);
//        Map<String, Object> response = Map.of(
//                "recommendedMedication", recommendedMedication
//        );
//        return ResponseEntity.ok(response);
//    }
//}
