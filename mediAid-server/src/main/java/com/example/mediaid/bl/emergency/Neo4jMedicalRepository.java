package com.example.mediaid.bl.emergency;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * Repository לשאילתות Neo4j
 */
@Repository
public class Neo4jMedicalRepository {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jMedicalRepository.class);

    @Autowired
    private Driver neo4jDriver;

    /**
     * מציאת מסלולים רפואיים עד 5 צעדים
     */
    public List<Record> findMedicalPathwaysQuery(String sourceCui, String targetCui, int maxDepth) {
        try (Session session = neo4jDriver.session()) {
            String advancedPathQuery = """
                MATCH (source {cui: $sourceCui})
                MATCH (target {cui: $targetCui})
                CALL apoc.path.expandConfig(source, {
                    relationshipFilter: "TREATS|CAUSES_SIDE_EFFECT|INDICATES|CAUSES_SYMPTOM|RISK_FACTOR_FOR|INFLUENCES>",
                    labelFilter: "+Disease|+Medication|+Symptom|+RiskFactor",
                    minLevel: 1,
                    maxLevel: $maxDepth,
                    limit: 15,
                    endNodes: [target]
                }) YIELD path
                WHERE last(nodes(path)).cui = $targetCui
                WITH path,
                     reduce(pathWeight = 0.0, rel in relationships(path) | 
                            pathWeight + rel.weight) / size(relationships(path)) *
                     CASE size(relationships(path))
                         WHEN 1 THEN 0.85
                         WHEN 2 THEN 0.72  // 0.85^2 מחושב מראש
                         WHEN 3 THEN 0.61  // 0.85^3 מחושב מראש
                         WHEN 4 THEN 0.52  // 0.85^4 מחושב מראש
                         WHEN 5 THEN 0.44  // 0.85^5 מחושב מראש
                         ELSE 0.35
                     END as riskScore,
                     [node in nodes(path) | {
                         cui: node.cui, 
                         name: node.name, 
                         type: labels(node)[0]
                     }] as pathNodes,
                     [rel in relationships(path) | {
                         type: type(rel), 
                         weight: rel.weight,
                         source: startNode(rel).name,
                         target: endNode(rel).name
                     }] as pathRelationships
                RETURN pathNodes, pathRelationships, 
                       riskScore,
                       size(relationships(path)) as pathLength
                ORDER BY riskScore DESC, pathLength ASC
                LIMIT 10
                """;

            return session.readTransaction(tx ->
                    tx.run(advancedPathQuery, Map.of(
                            "sourceCui", sourceCui,
                            "targetCui", targetCui,
                            "maxDepth", maxDepth
                    )).list());
        } catch (Exception e) {
            logger.error("Error in pathway query: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * איתור קהילות עם GDS
     */
    public List<Record> detectCommunitiesWithGDSQuery(List<String> userCuis) {
        try (Session session = neo4jDriver.session()) {
            if (userCuis == null || userCuis.isEmpty()) {
                logger.warn("No user CUIs provided for community detection");
                return new ArrayList<>();
            }

            // בניית השאילתה עם פרמטרים בטוחים במקום החדרת טקסט
            String query = """
            CALL gds.graph.project.cypher(
                'user-medical-network',
                'MATCH (n) WHERE n.cui IN $userCuis RETURN id(n) AS id, labels(n)[0] AS type',
                'MATCH (n)-[r]-(m) WHERE n.cui IN $userCuis AND m.cui IN $userCuis 
                 RETURN id(n) AS source, id(m) AS target, coalesce(r.weight, 0.5) AS weight'
            )
            YIELD graphName

            CALL gds.louvain.stream(graphName, {
                relationshipWeightProperty: 'weight',
                maxLevels: 3,
                tolerance: 0.001
            })
            YIELD nodeId, communityId

            WITH gds.util.asNode(nodeId) AS node, communityId
            RETURN communityId, 
                   collect({cui: node.cui, name: node.name, type: labels(node)[0]}) AS members,
                   count(*) AS size
            ORDER BY size DESC
            LIMIT 10
            """;

            logger.debug("Running GDS community detection with {} CUIs", userCuis.size());

            // העברת הפרמטרים בבטחה
            Map<String, Object> params = new HashMap<>();
            params.put("userCuis", userCuis);

            List<Record> records = session.readTransaction(tx -> tx.run(query, params).list());

            // ניקוי הגרף לאחר השימוש
            session.writeTransaction(tx -> {
                tx.run("CALL gds.graph.drop('user-medical-network', false)").consume();
                return null;
            });

            return records;
        } catch (Exception e) {
            logger.warn("GDS community detection failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * איתור קהילות בסיסי
     */
    public List<Record> detectCommunitiesBasicQuery(List<String> userCuis) {
        try (Session session = neo4jDriver.session()) {
            String basicCommunityQuery = """
            MATCH (n)-[r]-(m) 
            WHERE n.cui IN $userCuis AND m.cui IN $userCuis
            WITH n, collect(DISTINCT m) as connections, count(r) as connectionCount
            WHERE connectionCount >= 2
            RETURN n.cui as centerCui, n.name as centerName, 
                   [conn in connections | {cui: conn.cui, name: conn.name, type: labels(conn)[0]}] as members,
                   connectionCount as size
            ORDER BY connectionCount DESC
            LIMIT 5
            """;

            return session.readTransaction(tx ->
                    tx.run(basicCommunityQuery, Map.of("userCuis", userCuis)).list()
            );
        } catch (Exception e) {
            logger.error("Error in basic community detection: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * חישוב סיכונים
     */
    public List<Record> findRiskPropagationQuery(String sourceCui, String targetCui, double initialRisk, double decay) {
        try (Session session = neo4jDriver.session()) {
            String riskPropagationQuery = """
                MATCH (source {cui: $sourceCui})
                MATCH (target {cui: $targetCui})
                CALL apoc.path.expandConfig(source, {
                    relationshipFilter: "RISK_FACTOR_FOR|CAUSES_SYMPTOM|CAUSES_SIDE_EFFECT|LEADS_TO>",
                    maxLevel: 4,
                    limit: 20,
                    endNodes: [target]
                }) YIELD path
                WHERE any(node in nodes(path) WHERE node.cui = $targetCui)
                
                WITH path,
                     reduce(propagatedRisk = $initialRisk, rel in relationships(path) | 
                            propagatedRisk * rel.weight * 
                            CASE length(path)
                                WHEN 1 THEN $decay
                                WHEN 2 THEN $decay * $decay  
                                WHEN 3 THEN $decay * $decay * $decay
                                WHEN 4 THEN $decay * $decay * $decay * $decay
                                ELSE 0.1
                            END) as finalRisk
                WHERE finalRisk > 0.05
                
                RETURN nodes(path) as pathNodes,
                       relationships(path) as pathRels,
                       finalRisk,
                       length(path) as pathLength
                ORDER BY finalRisk DESC
                LIMIT 5
                """;

            return session.readTransaction(tx ->
                    tx.run(riskPropagationQuery, Map.of(
                            "sourceCui", sourceCui,
                            "targetCui", targetCui,
                            "initialRisk", initialRisk,
                            "decay", decay
                    )).list());
        } catch (Exception e) {
            logger.error("Error in risk propagation query: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * ניתוח centrality למציאת "hub" רפואיים חשובים
     */
    public List<Record> findMedicalHubsQuery(List<String> userCuis) {
        try (Session session = neo4jDriver.session()) {
            // בדיקה שהרשימה לא ריקה
            if (userCuis == null || userCuis.isEmpty()) {
                logger.warn("No user CUIs provided for centrality analysis");
                return new ArrayList<>();
            }

            String centralityQuery = """
            CALL gds.graph.project.cypher(
                'centrality-network',
                'MATCH (n) WHERE n.cui IN $userCuis RETURN id(n) AS id, n.cui AS cui, n.name AS name',
                'MATCH (n)-[r]-(m) WHERE n.cui IN $userCuis AND m.cui IN $userCuis RETURN id(n) AS source, id(m) AS target, coalesce(r.weight, 1.0) AS weight'
            )
            YIELD graphName

            CALL gds.betweenness.stream(graphName, {relationshipWeightProperty: 'weight'})
            YIELD nodeId, score

            WITH gds.util.asNode(nodeId) AS node, score
            RETURN node.cui AS cui, node.name AS name, score
            ORDER BY score DESC
            LIMIT 10
            """;

            logger.debug("Running GDS centrality analysis with {} CUIs", userCuis.size());
            logger.debug("User CUIs: {}", userCuis.subList(0, Math.min(5, userCuis.size())));

            Map<String, Object> params = Map.of("userCuis", userCuis);

            List<Record> records = session.readTransaction(tx -> tx.run(centralityQuery, params).list());

            // ניקוי הגרף
            try {
                session.writeTransaction(tx -> {
                    tx.run("CALL gds.graph.drop('centrality-network', false)").consume();
                    return null;
                });
            } catch (Exception e) {
                logger.debug("Graph cleanup issue (might not exist): {}", e.getMessage());
            }

            return records;
        } catch (Exception e) {
            logger.error("Error in GDS centrality query: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * מציאת Hub בסיסי
     */
    public List<Record> findHubsBasicQuery(List<String> userCuis) {
        try (Session session = neo4jDriver.session()) {
            String basicHubQuery = """
                MATCH (n)-[r]-(m) 
                WHERE n.cui IN $userCuis AND m.cui IN $userCuis
                WITH n, count(r) as connectionCount
                WHERE connectionCount > 1
                RETURN n.cui as cui, n.name as name, labels(n)[0] as type, connectionCount as score
                ORDER BY connectionCount DESC
                LIMIT 5
                """;

            return session.readTransaction(tx ->
                    tx.run(basicHubQuery, Map.of("userCuis", userCuis)).list()
            );
        } catch (Exception e) {
            logger.error("Error in basic hub detection: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * קשר בין תרופות לבין סימפטומים של המשתמש
     */
    public List<Record> findMedicationSideEffectsQuery(String medCui, String sympCui) {
        try (Session session = neo4jDriver.session()) {
            String query = """
                MATCH (med:Medication {cui: $medCui})-[r:CAUSES_SIDE_EFFECT]->(symp:Symptom {cui: $sympCui})
                RETURN med.name as medName, symp.name as sympName, r.weight as confidence, r.source as source
                UNION
                MATCH (med:Medication {cui: $medCui})-[r:SIDE_EFFECT_OF]-(symp:Symptom {cui: $sympCui})
                RETURN med.name as medName, symp.name as sympName, r.weight as confidence, r.source as source
            """;

            return session.readTransaction(tx ->
                    tx.run(query, Map.of("medCui", medCui, "sympCui", sympCui)).list()
            );
        } catch (Exception e) {
            logger.error("Error in side effects query: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * קשר בין מחלה וסימפטומים
     */
    public List<Record> findDiseaseSymptomsQuery(String disCui, String sympCui) {
        try (Session session = neo4jDriver.session()) {
            String query = """
                MATCH (dis:Disease {cui: $disCui})-[r:CAUSES_SYMPTOM]->(symp:Symptom {cui: $sympCui})
                RETURN dis.name as disName, symp.name as sympName, r.weight as confidence, r.source as source
                UNION
                MATCH (dis:Disease {cui: $disCui})<-[r:INDICATES]-(symp:Symptom {cui: $sympCui})
                RETURN dis.name as disName, symp.name as sympName, r.weight as confidence, r.source as source
                """;

            return session.readTransaction(tx ->
                    tx.run(query, Map.of("disCui", disCui, "sympCui", sympCui)).list());
        } catch (Exception e) {
            logger.error("Error in disease symptoms query: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * חיפוש טיפול אפשרי לסימפטום
     */
    public List<Record> findPossibleTreatmentsQuery(String sympCui) {
        try (Session session = neo4jDriver.session()) {
            // חיפוש תרופות שמטפלות בסימפטום
            String treatmentQuery = """
            MATCH (symp:Symptom {cui: $sympCui})<-[r:CAUSES_SYMPTOM]-(dis:Disease)<-[t:TREATS]-(med:Medication)
            RETURN DISTINCT med.name as medName, med.cui as medCui, dis.name as disName, 
                   (r.weight + t.weight) / 2 as confidence
            ORDER BY confidence DESC
            LIMIT 5
            """;

            return session.readTransaction(tx ->
                    tx.run(treatmentQuery, Map.of("sympCui", sympCui)).list());
        } catch (Exception e) {
            logger.error("Error in treatments query: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}