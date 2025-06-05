package com.example.mediaid.api;

import com.example.mediaid.bl.DemoMode;
import com.example.mediaid.dal.UMLS_terms.Disease;
import com.example.mediaid.dal.UMLS_terms.DiseaseRepository;
import com.example.mediaid.dal.UMLS_terms.Medication;
import com.example.mediaid.dal.UMLS_terms.MedicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class TermsController {

    private static final Logger logger = LoggerFactory.getLogger(TermsController.class);

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private DiseaseRepository diseaseRepository;

    // GET /api/medications/search - חיפוש תרופות עם עדיפות למונחי Demo
    @GetMapping("/medications/search")
    public List<Medication> searchMedications(
            @RequestParam String query,
            @RequestParam(defaultValue = "30") int limit) {

        logger.info("🔍 Searching medications with query: '{}', limit: {}", query, limit);

        try {
            List<Medication> combinedResults = new ArrayList<>();

            if (DemoMode.MODE) {
                // שלב 1: חיפוש במונחי Demo ראשונים
                List<String> demoCuis = DemoMode.DEMO_CUIS.stream().collect(Collectors.toList());
                List<Medication> demoResults = medicationRepository.findDemoRelevantByNameContaining(
                        demoCuis, query, PageRequest.of(0, limit));

                combinedResults.addAll(demoResults);
                logger.debug("Found {} DEMO medications for query '{}'", demoResults.size(), query);

                // שלב 2: אם צריך עוד תוצאות, חפש במונחים אחרים
                int remainingLimit = limit - demoResults.size();
                if (remainingLimit > 0) {
                    List<Medication> nonDemoResults = medicationRepository.findNonDemoByNameContaining(
                            demoCuis, query, PageRequest.of(0, remainingLimit));

                    combinedResults.addAll(nonDemoResults);
                    logger.debug("Found {} additional non-demo medications", nonDemoResults.size());
                }
            } else {
                // אם Demo Mode כבוי, חיפוש רגיל
                combinedResults = medicationRepository.findByNameContainingIgnoreCaseOrderByNameAsc(
                        query, PageRequest.of(0, limit));
            }

            logger.info("Total found {} medications for query '{}' (Demo mode: {})",
                    combinedResults.size(), query, DemoMode.MODE ? "ON" : "OFF");

            // הדפס כמה דוגמאות עם סימון Demo
            combinedResults.stream().limit(5).forEach(med -> {
                boolean isDemo = DemoMode.isRelevantForDemo(med.getCui());
                logger.debug("   {} {} (CUI: {})",
                        isDemo ? "🔸" : "  ", med.getName(), med.getCui());
            });

            return combinedResults;

        } catch (Exception e) {
            logger.error("Error searching medications: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // GET /api/diseases/search - חיפוש מחלות עם עדיפות למונחי Demo
    @GetMapping("/diseases/search")
    public List<Disease> searchDiseases(
            @RequestParam String query,
            @RequestParam(defaultValue = "30") int limit) {

        logger.debug(" Searching diseases with query: '{}', limit: {}", query, limit);

        try {
            List<Disease> combinedResults = new ArrayList<>();

            if (DemoMode.MODE) {
                // שלב 1: חיפוש במונחי Demo ראשונים
                List<String> demoCuis = DemoMode.DEMO_CUIS.stream().collect(Collectors.toList());
                List<Disease> demoResults = diseaseRepository.findDemoRelevantByNameContaining(
                        demoCuis, query, PageRequest.of(0, limit));

                combinedResults.addAll(demoResults);
                logger.debug("Found {} DEMO diseases for query '{}'", demoResults.size(), query);

                // שלב 2: אם צריך עוד תוצאות, חפש במונחים אחרים
                int remainingLimit = limit - demoResults.size();
                if (remainingLimit > 0) {
                    List<Disease> nonDemoResults = diseaseRepository.findNonDemoByNameContaining(
                            demoCuis, query, PageRequest.of(0, remainingLimit));

                    combinedResults.addAll(nonDemoResults);
                    logger.debug("Found {} additional non-demo diseases", nonDemoResults.size());
                }
            } else {
                // אם Demo Mode כבוי, חיפוש רגיל
                combinedResults = diseaseRepository.findByNameContainingIgnoreCaseOrderByNameAsc(
                        query, PageRequest.of(0, limit));
            }

            logger.debug("Total found {} diseases for query '{}' (Demo mode: {})",
                    combinedResults.size(), query, DemoMode.MODE ? "ON" : "OFF");

            // הדפס כמה דוגמאות עם סימון Demo
            combinedResults.stream().limit(5).forEach(disease -> {
                boolean isDemo = DemoMode.isRelevantForDemo(disease.getCui());
                logger.debug("   {} {} (CUI: {})",
                        isDemo ? "🔸" : "  ", disease.getName(), disease.getCui());
            });

            return combinedResults;

        } catch (Exception e) {
            logger.error("Error searching diseases: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // שמור על ה-endpoints הישנים לתאימות לאחור
    @GetMapping("/searchTerms/medication")
    @Deprecated
    public List<Medication> searchMedicationsOld(
            @RequestParam String query,
            @RequestParam(defaultValue = "30") int limit) {
        return searchMedications(query, limit);
    }

    @GetMapping("/searchTerms/disease")
    @Deprecated
    public List<Disease> searchDiseasesOld(
            @RequestParam String query,
            @RequestParam(defaultValue = "30") int limit) {
        return searchDiseases(query, limit);
    }

    // GET /api/demo-info - מידע על מצב Demo
    @GetMapping("/demo-info")
    public Map<String, Object> getDemoInfo() {
        Map<String, Object> demoInfo = new HashMap<>();

        demoInfo.put("demoMode", DemoMode.MODE);
        demoInfo.put("demoCuisCount", DemoMode.DEMO_CUIS.size());
        demoInfo.put("description", DemoMode.getDemoStats());

        if (DemoMode.MODE) {
            // הוסף מידע מפורט על מונחי Demo
            try {
                // ספור כמה מונחי demo קיימים בפועל במסד
                List<String> demoCuisList = DemoMode.DEMO_CUIS.stream().collect(Collectors.toList());

                long demoMedicationsCount = medicationRepository.findDemoRelevantByNameContaining(
                        demoCuisList, "", PageRequest.of(0, 1000)).size();

                long demoDiseasesCount = diseaseRepository.findDemoRelevantByNameContaining(
                        demoCuisList, "", PageRequest.of(0, 1000)).size();

                demoInfo.put("availableDemoMedications", demoMedicationsCount);
                demoInfo.put("availableDemoDiseases", demoDiseasesCount);

                logger.debug("Demo info: {} medications, {} diseases available",
                        demoMedicationsCount, demoDiseasesCount);

            } catch (Exception e) {
                logger.warn("Could not get demo statistics: {}", e.getMessage());
                demoInfo.put("error", "Could not calculate demo statistics");
            }
        }

        return demoInfo;
    }

    // GET /api/demo-terms - קבלת רשימת מונחי Demo (לפיתוח/דיבוג)
    @GetMapping("/demo-terms")
    public Map<String, Object> getDemoTerms() {
        Map<String, Object> result = new HashMap<>();

        if (!DemoMode.MODE) {
            result.put("message", "Demo mode is disabled");
            return result;
        }

        try {
            List<String> demoCuisList = DemoMode.DEMO_CUIS.stream()
                    .sorted()
                    .collect(Collectors.toList());

            // מצא מונחי demo שקיימים במסד
            List<Medication> demoMedications = medicationRepository.findDemoRelevantByNameContaining(
                    demoCuisList, "", PageRequest.of(0, 1000));

            List<Disease> demoDiseases = diseaseRepository.findDemoRelevantByNameContaining(
                    demoCuisList, "", PageRequest.of(0, 1000));

            Map<String, Object> medications = new HashMap<>();
            demoMedications.forEach(med ->
                    medications.put(med.getCui(), med.getName()));

            Map<String, Object> diseases = new HashMap<>();
            demoDiseases.forEach(disease ->
                    diseases.put(disease.getCui(), disease.getName()));

            result.put("totalDemoCuis", DemoMode.DEMO_CUIS.size());
            result.put("foundMedications", medications);
            result.put("foundDiseases", diseases);
            result.put("foundMedicationsCount", demoMedications.size());
            result.put("foundDiseasesCount", demoDiseases.size());

            logger.info("Demo terms: {} medications, {} diseases found from {} total CUIs",
                    demoMedications.size(), demoDiseases.size(), DemoMode.DEMO_CUIS.size());

        } catch (Exception e) {
            logger.error("Error getting demo terms: {}", e.getMessage());
            result.put("error", e.getMessage());
        }

        return result;
    }
}