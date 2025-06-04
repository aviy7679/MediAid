
package com.example.mediaid.api;

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
import java.util.List;

@RestController
@RequestMapping("/api") // שונה מ /searchTerms ל /api
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class TermsController {

    private static final Logger logger = LoggerFactory.getLogger(TermsController.class);

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private DiseaseRepository diseaseRepository;

    // GET /api/medications/search - מתאים לקריאה מ-MedicationSearch.jsx
    @GetMapping("/medications/search")
    public List<Medication> searchMedications(
            @RequestParam String query,
            @RequestParam(defaultValue = "30") int limit) {

        logger.info("🔍 Searching medications with query: '{}', limit: {}", query, limit);

        try {
            List<Medication> results = medicationRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query,
                    PageRequest.of(0, limit));

            logger.debug("✅ Found {} medications for query '{}'", results.size(), query);

            // הדפס כמה דוגמאות
            results.stream().limit(3).forEach(med ->
                    logger.debug("   - {} (CUI: {})", med.getName(), med.getCui()));

            return results;
        } catch (Exception e) {
            logger.error("❌ Error searching medications: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // GET /api/diseases/search - מתאים לקריאה מ-UserDataWizard.jsx

    @GetMapping("/diseases/search")
    public List<Disease> searchDiseases(
            @RequestParam String query,
            @RequestParam(defaultValue = "30") int limit) {

        logger.debug("🔍 Searching diseases with query: '{}', limit: {}", query, limit);

        try {
            List<Disease> results = diseaseRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query,
                    PageRequest.of(0, limit));

            logger.debug("✅ Found {} diseases for query '{}'", results.size(), query);

            // הדפס כמה דוגמאות
            results.stream().limit(3).forEach(disease ->
                    logger.debug("   - {} (CUI: {})", disease.getName(), disease.getCui()));

            return results;
        } catch (Exception e) {
            logger.error("❌ Error searching diseases: {}", e.getMessage(), e);
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
}