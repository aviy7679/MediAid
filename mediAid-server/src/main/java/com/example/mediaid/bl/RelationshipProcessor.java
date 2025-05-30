package com.example.mediaid.bl;


import com.example.mediaid.bl.neo4j.EntityTypes;
import com.example.mediaid.bl.neo4j.RelationshipTypes;
import com.example.mediaid.dal.UMLS_terms.*;
import com.example.mediaid.dal.UMLS_terms.relationships.UmlsRelationship;
import com.example.mediaid.dal.UMLS_terms.relationships.UmlsRelationshipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Service
public class RelationshipProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RelationshipProcessor.class);
    private static final int BATCH_SIZE = 1000;

    private Map<String, Integer> debugRelationCounts = new HashMap<>();
    private Map<String, Integer> debugSourceCounts = new HashMap<>();
    private Set<String> sampleRejectedRels = new HashSet<>();

    private final UmlsRelationshipRepository relationshipRepository;
    private final DiseaseRepository diseaseRepository;
    private final MedicationRepository medicationRepository;
    private final SymptomRepository symptomRepository;
    private final RiskFactorRepository riskFactorRepository;
    private final ProcedureRepository procedureRepository;
    private final AnatomicalStructureRepository anatomicalStructureRepository;
    private final LabTestRepository labTestRepository;
    private final BiologicalFunctionRepository biologicalFunctionRepository;

    private Set<String> existingCuis;
    private Set<String> diseaseCuis;
    private Set<String> medicationCuis;
    private Set<String> symptomCuis;
    private Set<String> riskFactorCuis;
    private Set<String> procedureCuis;
    private Set<String> anatomicalCuis;
    private Set<String> labTestCuis;
    private Set<String> biologicalFunctionCuis;


    @Autowired
    public RelationshipProcessor(
            UmlsRelationshipRepository relationshipRepository,
            DiseaseRepository diseaseRepository,
            MedicationRepository medicationRepository,
            SymptomRepository symptomRepository,
            RiskFactorRepository riskFactorRepository,
            ProcedureRepository procedureRepository,
            AnatomicalStructureRepository anatomicalStructureRepository,
            LabTestRepository labTestRepository,
            BiologicalFunctionRepository biologicalFunctionRepository) {

        this.relationshipRepository = relationshipRepository;
        this.diseaseRepository = diseaseRepository;
        this.medicationRepository = medicationRepository;
        this.symptomRepository = symptomRepository;
        this.riskFactorRepository = riskFactorRepository;
        this.procedureRepository = procedureRepository;
        this.anatomicalStructureRepository = anatomicalStructureRepository;
        this.labTestRepository = labTestRepository;
        this.biologicalFunctionRepository = biologicalFunctionRepository;
    }

    @Transactional
    public void processAndSaveRelationships(String mrrelPath) {
        if (relationshipRepository.countAllRelationships() > 0) {
            logger.info("Relationships already exist in database. Skipping import.");
            return;
        }

        logger.info("Loading existing CUIs from PostgreSQL...");
        loadExistingCuis();

        logger.info("Processing MRREL file: {}", mrrelPath);

        List<UmlsRelationship> batch = new ArrayList<>();
        int totalProcessed = 0;
        int accepted = 0;
        int skippedSelfLoops = 0;
        int skippedNonPreferredSource = 0;
        int skippedMissingNodes = 0;
        int skippedInvalidRelationType = 0;
        int skippedInvalidNodeTypes = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(mrrelPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalProcessed++;

                ProcessResult result = processLine(line);
                if (result.relationship != null) {
                    batch.add(result.relationship);
                    accepted++;

                    if (batch.size() >= BATCH_SIZE) {
                        relationshipRepository.saveAll(batch);
                        batch.clear();
                        logger.info("Saved batch. Total accepted: {}", accepted);
                    }
                } else {
                    switch (result.skipReason) {
                        case SELF_LOOP: skippedSelfLoops++; break;
                        case NON_PREFERRED_SOURCE: skippedNonPreferredSource++; break;
                        case MISSING_NODES: skippedMissingNodes++; break;
                        case INVALID_RELATION_TYPE: skippedInvalidRelationType++; break;
                        case INVALID_NODE_TYPES: skippedInvalidNodeTypes++; break;
                    }
                }

                if (totalProcessed % 1000000 == 0) {
                    logger.info("Processed {} lines, accepted {}", totalProcessed, accepted);
                }
            }

            if (!batch.isEmpty()) {
                relationshipRepository.saveAll(batch);
            }
            logger.info("=== DEBUGGING INFO ===");
            logger.info("TOP 20 SOURCES FOUND:");
            debugSourceCounts.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(20)
                    .forEach(entry -> logger.info("  {}: {} occurrences", entry.getKey(), entry.getValue()));

            logger.info("TOP 30 RELATIONSHIP TYPES FOUND:");
            debugRelationCounts.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(30)
                    .forEach(entry -> logger.info("  {}: {} occurrences", entry.getKey(), entry.getValue()));

            logger.info("SAMPLE REJECTED RELATIONSHIPS:");
            sampleRejectedRels.forEach(sample -> logger.info("  {}", sample));

            logger.info("PREFERRED SOURCES IN CONFIG:");
            EntityTypes.PREFERRED_SOURCES.forEach(source -> logger.info("  {}", source));

            logger.info("MAPPED RELATIONSHIPS IN CONFIG:");
            RelationshipTypes.UMLS_TO_NEO4J_RELATIONSHIPS.entrySet().stream()
                    .limit(20)
                    .forEach(entry -> logger.info("  '{}' -> '{}'", entry.getKey(), entry.getValue()));

            logger.info("COMPLETED RELATIONSHIP PROCESSING:");
            logger.info("  Total lines processed: {}", totalProcessed);
            logger.info("  Total relationships saved: {}", accepted);
            logger.info("  Skipped - Self loops: {}", skippedSelfLoops);
            logger.info("  Skipped - Non-preferred source: {}", skippedNonPreferredSource);
            logger.info("  Skipped - Missing nodes: {}", skippedMissingNodes);
            logger.info("  Skipped - Invalid relation type: {}", skippedInvalidRelationType);
            logger.info("  Skipped - Invalid node types: {}", skippedInvalidNodeTypes);

        } catch (IOException e) {
            logger.error("Error processing MRREL file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process MRREL file", e);
        }
    }


    private ProcessResult processLine(String line) {
        logger.info("💯💯💯💯💯Processing line: {} in demo mode", line);
        String[] fields = line.split("\\|");
        if (fields.length < 15) {
            return new ProcessResult(null, SkipReason.INVALID_FORMAT);
        }

        String cui1 = fields[0];
        String cui2 = fields[4];
        String rel = fields[3];
        String rela = fields[7];
        String sab = fields[10];

        if(DemoMode.MODE){
            if(!DemoMode.DEMO_MODES.contains(cui1) || !DemoMode.DEMO_MODES.contains(cui2)){
                return new ProcessResult(null, SkipReason.NON_DEMO_RELEVANT);
            }
        }

        // סינון 1: מניעת לולאות עצמיות
        if (cui1.equals(cui2)) {
            return new ProcessResult(null, SkipReason.SELF_LOOP);
        }

        // סינון 2: מקורות מועדפים
        if (!DemoMode.MODE && (!EntityTypes.PREFERRED_SOURCES.contains(sab))) {
            return new ProcessResult(null, SkipReason.NON_PREFERRED_SOURCE);
        }

        // סינון 3: קיום צמתים
        if (!DemoMode.MODE && (!existingCuis.contains(cui1) || !existingCuis.contains(cui2))) {
            return new ProcessResult(null, SkipReason.MISSING_NODES);
        }

        // סינון 4: סוג קשר תקין
        String relationshipType = determineRelationshipType(rel, rela);
        if (relationshipType == null) {
            return new ProcessResult(null, SkipReason.INVALID_RELATION_TYPE);
        }

        String neoRelType = RelationshipTypes.UMLS_TO_NEO4J_RELATIONSHIPS.get(relationshipType.toLowerCase());
        if (neoRelType == null) {
            if(DemoMode.MODE){
                neoRelType = normalizeRelationshipType(relationshipType);
            }else{
                return new ProcessResult(null, SkipReason.INVALID_RELATION_TYPE);
            }
        }

        // יצירת הקשר
        UmlsRelationship relationship = new UmlsRelationship();
        relationship.setCui1(cui1);
        relationship.setCui2(cui2);
        relationship.setRelationshipType(neoRelType);
        relationship.setWeight(RelationshipTypes.calculateRelationshipWeight(relationshipType, sab));
        relationship.setSource(sab);
        relationship.setOriginalRel(rel);
        relationship.setOriginalRela(rela);

        return new ProcessResult(relationship, null);
    }
    private void loadExistingCuis() {
            existingCuis = new HashSet<>();
            diseaseCuis = new HashSet<>();
            medicationCuis = new HashSet<>();
            symptomCuis = new HashSet<>();
            riskFactorCuis = new HashSet<>();
            procedureCuis = new HashSet<>();
            anatomicalCuis = new HashSet<>();
            labTestCuis = new HashSet<>();
            biologicalFunctionCuis = new HashSet<>();

            diseaseRepository.findAll().forEach(d -> {
                existingCuis.add(d.getCui());
                diseaseCuis.add(d.getCui());
            });

            medicationRepository.findAll().forEach(m -> {
                existingCuis.add(m.getCui());
                medicationCuis.add(m.getCui());
            });

            symptomRepository.findAll().forEach(s -> {
                existingCuis.add(s.getCui());
                symptomCuis.add(s.getCui());
            });

            riskFactorRepository.findAll().forEach(rf -> {
                existingCuis.add(rf.getCui());
                riskFactorCuis.add(rf.getCui());
            });

            procedureRepository.findAll().forEach(p -> {
                existingCuis.add(p.getCui());
                procedureCuis.add(p.getCui());
            });

            anatomicalStructureRepository.findAll().forEach(as -> {
                existingCuis.add(as.getCui());
                anatomicalCuis.add(as.getCui());
            });

            labTestRepository.findAll().forEach(lt -> {
                existingCuis.add(lt.getCui());
                labTestCuis.add(lt.getCui());
            });

            biologicalFunctionRepository.findAll().forEach(bf -> {
                existingCuis.add(bf.getCui());
                biologicalFunctionCuis.add(bf.getCui());
            });

            logger.info("Loaded {} existing CUIs", existingCuis.size());
            logger.info("  Diseases: {}", diseaseCuis.size());
            logger.info("  Medications: {}", medicationCuis.size());
            logger.info("  Symptoms: {}", symptomCuis.size());
            logger.info("  Risk Factors: {}", riskFactorCuis.size());
            logger.info("  Procedures: {}", procedureCuis.size());
            logger.info("  Anatomical Structures: {}", anatomicalCuis.size());
            logger.info("  Lab Tests: {}", labTestCuis.size());
            logger.info("  Biological Functions: {}", biologicalFunctionCuis.size());
        }

    private String determineRelationshipType(String rel, String rela) {
        // בדוק תחילה RELA (ספציפי יותר)
        if (rela != null && !rela.trim().isEmpty()) {
            String normalized = rela.trim().toLowerCase();
            if (RelationshipTypes.UMLS_TO_NEO4J_RELATIONSHIPS.containsKey(normalized)) {
                return normalized;
            }
            return normalized; // גם אם לא במיפוי, תחזיר כמו שהוא
        }

        // אם אין RELA, השתמש ב-REL
        if (rel != null && !rel.trim().isEmpty()) {
            String normalized = rel.trim().toLowerCase();
            if (RelationshipTypes.UMLS_TO_NEO4J_RELATIONSHIPS.containsKey(normalized)) {
                return normalized;
            }
            return normalized; // גם אם לא במיפוי, תחזיר כמו שהוא
        }

        return null; // רק אם באמת אין כלום
    }
    private boolean isValidNodeTypeCombo(String cui1, String cui2, String relationshipType) {
        return RelationshipTypes.isValidRelationshipForNodeTypes(
                diseaseCuis, medicationCuis, symptomCuis,
                riskFactorCuis, procedureCuis, anatomicalCuis,
                labTestCuis, biologicalFunctionCuis,
                cui1, cui2, relationshipType);
    }

    private static class ProcessResult {
        final UmlsRelationship relationship;
        final SkipReason skipReason;

        ProcessResult(UmlsRelationship relationship, SkipReason skipReason) {
            this.relationship = relationship;
            this.skipReason = skipReason;
        }
    }

    private enum SkipReason {
        SELF_LOOP,
        NON_PREFERRED_SOURCE,
        MISSING_NODES,
        INVALID_RELATION_TYPE,
        INVALID_NODE_TYPES,
        INVALID_FORMAT,
        NON_DEMO_RELEVANT
    }

    private String normalizeRelationshipType(String type) {
        return type.toUpperCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replaceAll("[^A-Z0-9_]", "");
    }
}
