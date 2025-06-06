package com.example.mediaid.api;

import com.example.mediaid.bl.neo4j.DemoMode;
import com.example.mediaid.bl.neo4j.DemoRelationshipEnricher;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/demo")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class DemoEnrichmentController {

    private static final Logger logger = LoggerFactory.getLogger(DemoEnrichmentController.class);

    @Autowired
    private DemoRelationshipEnricher demoEnricher;

    @Autowired
    private Driver neo4jDriver;

    /**
     * הפעלת העשרת קשרים למושגי דמו
     */
    @PostMapping("/enrich-relationships")
    public ResponseEntity<Map<String, Object>> enrichDemoRelationships() {
        logger.info("🚀 API call to enrich demo relationships");

        try {
            Map<String, Object> results = demoEnricher.enrichDemoRelationships();

            if ((Boolean) results.getOrDefault("success", false)) {
                return ResponseEntity.ok(results);
            } else {
                return ResponseEntity.internalServerError().body(results);
            }

        } catch (Exception e) {
            logger.error("Error in demo enrichment", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * ניתוח קשרים של מושגי דמו
     */
    @GetMapping("/analyze-demo-relationships")
    public ResponseEntity<Map<String, Object>> analyzeDemoRelationships() {
        logger.info("📊 API call to analyze demo relationships");

        try {
            Map<String, Object> analysis = new HashMap<>();

            try (Session session = neo4jDriver.session()) {
                session.readTransaction(tx -> {
                    // ספירת קשרים עבור כל מושג דמו
                    Map<String, Long> demoCuiRelationships = new HashMap<>();

                    for (String demoCui : DemoMode.DEMO_CUIS) {
                        // קשרים כcui1
                        var result1 = tx.run(
                                "MATCH (n1 {cui: $cui})-[r]->(n2) RETURN COUNT(r) as count",
                                Map.of("cui", demoCui)
                        );
                        long outgoing = result1.hasNext() ? result1.next().get("count").asLong() : 0;

                        // קשרים כcui2
                        var result2 = tx.run(
                                "MATCH (n1)-[r]->(n2 {cui: $cui}) RETURN COUNT(r) as count",
                                Map.of("cui", demoCui)
                        );
                        long incoming = result2.hasNext() ? result2.next().get("count").asLong() : 0;

                        long total = outgoing + incoming;
                        if (total > 0) {
                            demoCuiRelationships.put(demoCui, total);
                        }
                    }

                    analysis.put("demoCuiRelationships", demoCuiRelationships);

                    // טופ 10 מושגי דמו עם הכי הרבה קשרים
                    List<Map<String, Object>> topDemoCuis = demoCuiRelationships.entrySet().stream()
                            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                            .limit(10)
                            .map(entry -> {
                                Map<String, Object> cuiInfo = new HashMap<>();
                                cuiInfo.put("cui", entry.getKey());
                                cuiInfo.put("relationshipCount", entry.getValue());
                                cuiInfo.put("name", getDemoConceptName(entry.getKey()));
                                return cuiInfo;
                            })
                            .toList();

                    analysis.put("topDemoCuis", topDemoCuis);

                    // סטטיסטיקות כלליות
                    long totalDemoRelationships = demoCuiRelationships.values().stream()
                            .mapToLong(Long::longValue)
                            .sum();
                    analysis.put("totalDemoRelationships", totalDemoRelationships);
                    analysis.put("demoCuisWithRelationships", demoCuiRelationships.size());
                    analysis.put("totalDemoCuis", DemoMode.DEMO_CUIS.size());

                    // ממוצע קשרים למושג דמו
                    double avgRelationships = demoCuiRelationships.isEmpty() ? 0.0 :
                            (double) totalDemoRelationships / demoCuiRelationships.size();
                    analysis.put("averageRelationshipsPerDemoCui", Math.round(avgRelationships * 100.0) / 100.0);

                    return null;
                });

                // ניתוח סוגי קשרים
                Map<String, Long> relationshipTypes = analyzeDemoRelationshipTypes();
                analysis.put("relationshipTypes", relationshipTypes);

                analysis.put("success", true);
                analysis.put("timestamp", System.currentTimeMillis());
            }

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            logger.error("Error analyzing demo relationships", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * ניתוח סוגי קשרים של מושגי דמו
     */
    private Map<String, Long> analyzeDemoRelationshipTypes() {
        Map<String, Long> relationshipTypes = new HashMap<>();

        try (Session session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                // קשרים שיוצאים ממושגי דמו
                StringBuilder cuiList = new StringBuilder();
                for (String cui : DemoMode.DEMO_CUIS) {
                    if (cuiList.length() > 0) cuiList.append("', '");
                    cuiList.append(cui);
                }

                String query = "MATCH (n1)-[r]->(n2) " +
                        "WHERE n1.cui IN ['" + cuiList + "'] OR n2.cui IN ['" + cuiList + "'] " +
                        "RETURN type(r) as relType, COUNT(r) as count " +
                        "ORDER BY count DESC " +
                        "LIMIT 20";

                var result = tx.run(query);
                result.forEachRemaining(record -> {
                    String relType = record.get("relType").asString();
                    long count = record.get("count").asLong();
                    relationshipTypes.put(relType, count);
                });

                return null;
            });
        } catch (Exception e) {
            logger.warn("Error analyzing relationship types: {}", e.getMessage());
        }

        return relationshipTypes;
    }

    /**
     * קבלת פרטים על מושג דמו ספציפי
     */
    @GetMapping("/demo-concept/{cui}")
    public ResponseEntity<Map<String, Object>> getDemoConceptDetails(@PathVariable String cui) {
        logger.info("🔍 Getting details for demo concept: {}", cui);

        if (!DemoMode.DEMO_CUIS.contains(cui)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "CUI is not a demo concept");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            Map<String, Object> details = new HashMap<>();

            try (Session session = neo4jDriver.session()) {
                session.readTransaction(tx -> {
                    // קשרים יוצאים
                    var outgoingResult = tx.run(
                            "MATCH (n1 {cui: $cui})-[r]->(n2) " +
                                    "RETURN n2.cui as targetCui, n2.name as targetName, " +
                                    "type(r) as relType, r.weight as weight, r.source as source " +
                                    "ORDER BY r.weight DESC " +
                                    "LIMIT 50",
                            Map.of("cui", cui)
                    );

                    List<Map<String, Object>> outgoingRelationships = new ArrayList<>();
                    outgoingResult.forEachRemaining(record -> {
                        Map<String, Object> rel = new HashMap<>();
                        rel.put("direction", "outgoing");
                        rel.put("targetCui", record.get("targetCui").asString(""));
                        rel.put("targetName", record.get("targetName").asString(""));
                        rel.put("relationshipType", record.get("relType").asString(""));
                        rel.put("weight", record.get("weight").asDouble(0.0));
                        rel.put("source", record.get("source").asString(""));
                        outgoingRelationships.add(rel);
                    });

                    // קשרים נכנסים
                    var incomingResult = tx.run(
                            "MATCH (n1)-[r]->(n2 {cui: $cui}) " +
                                    "RETURN n1.cui as sourceCui, n1.name as sourceName, " +
                                    "type(r) as relType, r.weight as weight, r.source as source " +
                                    "ORDER BY r.weight DESC " +
                                    "LIMIT 50",
                            Map.of("cui", cui)
                    );

                    List<Map<String, Object>> incomingRelationships = new ArrayList<>();
                    incomingResult.forEachRemaining(record -> {
                        Map<String, Object> rel = new HashMap<>();
                        rel.put("direction", "incoming");
                        rel.put("sourceCui", record.get("sourceCui").asString(""));
                        rel.put("sourceName", record.get("sourceName").asString(""));
                        rel.put("relationshipType", record.get("relType").asString(""));
                        rel.put("weight", record.get("weight").asDouble(0.0));
                        rel.put("source", record.get("source").asString(""));
                        incomingRelationships.add(rel);
                    });

                    details.put("cui", cui);
                    details.put("name", getDemoConceptName(cui));
                    details.put("outgoingRelationships", outgoingRelationships);
                    details.put("incomingRelationships", incomingRelationships);
                    details.put("totalOutgoing", outgoingRelationships.size());
                    details.put("totalIncoming", incomingRelationships.size());
                    details.put("totalRelationships", outgoingRelationships.size() + incomingRelationships.size());

                    return null;
                });
            }

            details.put("success", true);
            details.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(details);

        } catch (Exception e) {
            logger.error("Error getting demo concept details", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * סטטוס מצב דמו
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDemoStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("demoModeEnabled", DemoMode.MODE);
        status.put("totalDemoCuis", DemoMode.DEMO_CUIS.size());
        status.put("demoCuis", DemoMode.DEMO_CUIS);
        status.put("demoStats", DemoMode.getDemoStats());

        // ספירת מושגי דמו שקיימים בNeo4j
        try (Session session = neo4jDriver.session()) {
            long demoCuisInNeo4j = session.readTransaction(tx -> {
                StringBuilder cuiList = new StringBuilder();
                for (String cui : DemoMode.DEMO_CUIS) {
                    if (cuiList.length() > 0) cuiList.append("', '");
                    cuiList.append(cui);
                }

                var result = tx.run("MATCH (n) WHERE n.cui IN ['" + cuiList + "'] RETURN COUNT(n) as count");
                return result.hasNext() ? result.next().get("count").asLong() : 0;
            });

            status.put("demoCuisInNeo4j", demoCuisInNeo4j);
        } catch (Exception e) {
            status.put("demoCuisInNeo4j", "Error: " + e.getMessage());
        }

        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }

    /**
     * עזרה ותיעוד
     */
    @GetMapping("/help")
    public ResponseEntity<Map<String, Object>> getDemoHelp() {
        Map<String, Object> help = new HashMap<>();

        help.put("description", "Demo Relationship Enrichment API");

        help.put("endpoints", Map.of(
                "POST /api/admin/demo/enrich-relationships",
                "Enrich relationships for demo concepts by scanning MRREL file",

                "GET /api/admin/demo/analyze-demo-relationships",
                "Analyze existing relationships for demo concepts",

                "GET /api/admin/demo/demo-concept/{cui}",
                "Get detailed relationships for specific demo concept",

                "GET /api/admin/demo/status",
                "Get demo mode status and statistics"
        ));

        help.put("process", List.of(
                "1. Ensure demo mode is enabled in DemoMode.java",
                "2. Configure MRREL file path in application.properties",
                "3. Call POST /enrich-relationships to start enrichment",
                "4. Monitor progress in logs",
                "5. Use analyze endpoints to verify results"
        ));

        help.put("expectedResults", Map.of(
                "Aspirin (C0004057)", "Hundreds of relationships (side effects, interactions, treatments)",
                "Diabetes (C4013416)", "Hundreds of relationships (symptoms, complications, treatments)",
                "Chest Pain (C3807341)", "Hundreds of relationships (causes, related conditions, tests)"
        ));

        help.put("estimatedTime", "2-4 hours depending on MRREL file size");

        return ResponseEntity.ok(help);
    }

    /**
     * קבלת שם מושג דמו
     */
    private String getDemoConceptName(String cui) {
        Map<String, String> conceptNames = Map.of(
                "C0004057", "Aspirin",
                "C0025598", "Metformin",
                "C4013416", "Diabetes mellitus",
                "C4014808", "Hypertensive disease",
                "C0004096", "Asthma",
                "C3807341", "Chest pain",
                "C0018681", "Headache",
                "C0015967", "Fever",
                "C3554470", "Nausea",
                "C0030193", "Pain"
        );

        return conceptNames.getOrDefault(cui, "Demo Concept");
    }
}