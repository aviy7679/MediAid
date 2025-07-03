//package com.example.mediaid.bl.emergency;
//
//import com.example.mediaid.dto.emergency.ExtractedSymptom;
//import com.example.mediaid.dto.emergency.UserMedicalEntity;
//import lombok.Data;
//import org.neo4j.driver.Driver;
//import org.neo4j.driver.Session;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.neo4j.driver.Record;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * שירות Analytics מתקדם לגרף רפואי - Graph Thinking אמיתי
// */
//
//@Service
//public class MedicalGraphAnalyticsService {
//
//    private static final Logger logger = LoggerFactory.getLogger(MedicalGraphAnalyticsService.class);
//
//    @Autowired
//    private Driver neo4jDriver;
//
//    //מציאת מסלולים רפואיים עד 5 צעדים
//    public List<MedicalPathway> findMedicalPathways(String sourceCui, Set<ExtractedSymptom> targetSymptoms, int maxDepth) {
//        logger.info("Finding advanced pathways from {} to {} symptoms (max depth: {})", sourceCui, targetSymptoms.size(), maxDepth);
//        List<MedicalPathway> pathways = new ArrayList<>();
//        try (Session session = neo4jDriver.session()) {
//            for(ExtractedSymptom symptom : targetSymptoms) {
//                String advancedPathQuery= """
//                        MATCH (source {cui: $sourceCui})
//                        CALL apoc.path.expandConfig(source, {
//                            relationshipFilter: "TREATS|CAUSES_SIDE_EFFECT|INDICATES|RISK_FACTOR_FOR|INFLUENCES>",
//                            labelFilter: "+Disease|+Medication|+Symptom|+RiskFactor",
//                            minLevel: 1,
//                            maxLevel: $maxDepth,
//                            limit: 15,
//                            endNodes: [{cui: $targetCui}]
//                        }) YIELD path
//                        WHERE last(nodes(path)).cui = $targetCui
//                        WITH path,
//                             reduce(pathWeight = 1.0, rel in relationships(path) |\s
//                                    pathWeight * rel.weight * exp(ln(0.85) * length(path))) as riskScore,
//                             [node in nodes(path) | {cui: node.cui, name: node.name, type: labels(node)[0]}] as pathNodes,
//                             [rel in relationships(path) | {type: type(rel), weight: rel.weight}] as pathRelationships
//                        RETURN pathNodes, pathRelationships, riskScore, length(path) as pathLength
//                        ORDER BY riskScore DESC, pathLength ASC
//                        LIMIT 10
//                        """;
//
//                var result = session.readTransaction(tx->
//                        tx.run(advancedPathQuery, Map.of(
//                                "sourceCui", sourceCui,
//                                "targetCui", symptom.getCui(),
//                                "maxDepth", maxDepth
//                        )));
//                result.forEachRemaining(record -> {
//                    try{
//                        MedicalPathway pathway = parsePathwayFromRecord(record, sourceCui, symptom);
//                        if(pathway != null && pathway.getRiskScore() > 0.1) {
//                            pathways.add(pathway);
//                        }
//                    }catch(Exception e){
//                        logger.error("Error parsing pathway: {}",e.getMessage());
//                    }
//                });
//            }
//        }catch(Exception e){
//            logger.error("Error in pathway finding: {}",e.getMessage());
//        }
//        logger.info("Found {} pathways", pathways.size());
//        return pathways.stream()
//                .sorted((p1, p2)->Double.compare(p2.getRiskScore(), p1.getRiskScore()))
//                .collect(Collectors.toList());
//    }
//
//
//    /**
//     * זיהוי קהילות רפואיות באמצעות אלגוריתם Louvain
//     * מזהה clusters של מחלות/סימפטומים/תרופות הקשורים זה לזה
//     */
//    public List<MedicalCommunity> detectMedicalCommunities(List<UserMedicalEntity> userContext) {
//        logger.info("Detecting medical communities using Louvain algorithm");
//        List<MedicalCommunity> communities = new ArrayList<>();
//        try(Session session = neo4jDriver.session()) {
//            //גרף זמני למשתמש הנוכחי
//            List<String> userCuis = userContext.stream()
//                    .map(UserMedicalEntity::getCui)
//                    .collect(Collectors.toList());
//
//            String communityDetectionQuery = """
//                CALL gds.graph.project.cypher(
//                    'user-medical-network',
//                    'MATCH (n) WHERE n.cui IN $userCuis RETURN id(n) AS id, labels(n)[0] AS type',
//                    'MATCH (n)-[r]-(m) WHERE n.cui IN $userCuis AND m.cui IN $userCuis
//                     RETURN id(n) AS source, id(m) AS target,
//                            coalesce(r.weight, 0.5) AS weight, type(r) AS relationshipType'
//                )
//                YIELD graphName
//
//                CALL gds.louvain.stream(graphName, {
//                    relationshipWeightProperty: 'weight',
//                    includeIntermediateCommunities: true,
//                    maxLevels: 3,
//                    tolerance: 0.001
//                })
//                YIELD nodeId, communityId, intermediateCommunityIds
//
//                WITH gds.util.asNode(nodeId) AS node, communityId, intermediateCommunityIds
//                RETURN communityId,
//                       collect({cui: node.cui, name: node.name, type: labels(node)[0]}) AS members,
//                       count(*) AS size
//                ORDER BY size DESC
//                LIMIT 10
//                """;
//
//            var result = session.readTransaction(tx->tx.run(communityDetectionQuery,
//                    Map.of("userCuis", userCuis)));
//
//            result.forEachRemaining(record -> {
//                try{
//                    MedicalCommunity community = parseCommunityFromRecord(record);
//                    if (community != null && community.getSize() >=2) {
//                        communities.add(community);
//                    }
//                }catch(Exception e){
//                    logger.error("Error parsing community: {}",e.getMessage());
//                }
//            });
//
//            //ניקוי הגרף הזמני
//            session.writeTransaction(tx->{
//                tx.run("CALL gds.graph.drop('user-medical-network', false)");
//                return null;
//            });
//
//        }catch(Exception e){
//            logger.error("Error detecting medical communities: {}",e.getMessage());
//        }
//        logger.info("Found {} communities", communities.size());
//        return communities;
//    }
//
//
//    /**
//     * חישוב סיכונים עם הגרף
//     */
// public RiskPropagationResult calculateRiskPropagation(List<UserMedicalEntity> riskSources,Set<ExtractedSymptom> targetSymptoms, double decayFactor) {
//     logger.info("Calculating risk propagation from {} sources to {} targets",
//             riskSources.size(), targetSymptoms.size());
//     Map<String, Double> symptomRiskScores = new HashMap<>();
//     List<RiskPropagationPath> propagationPaths = new ArrayList<>();
//
//     try(Session session = neo4jDriver.session()) {
//         for(UserMedicalEntity riskSource : riskSources) {
//             for(ExtractedSymptom symptom : targetSymptoms) {
//                 String riskPropagationQuery = """
//                        MATCH (source {cui: $sourceCui})
//                        CALL apoc.path.expandConfig(source, {
//                            relationshipFilter: "RISK_FACTOR_FOR|CAUSES|INFLUENCES|LEADS_TO>",
//                            maxLevel: 4,
//                            limit: 20
//                        }) YIELD path
//                        WHERE any(node in nodes(path) WHERE node.cui = $targetCui)
//
//                        WITH path,
//                             reduce(propagatedRisk = $initialRisk, rel in relationships(path) |
//                                    propagatedRisk * rel.weight * exp(ln(0.85) * length(path))) as finalRisk
//                        WHERE finalRisk > 0.05
//
//                        RETURN nodes(path) as pathNodes,
//                               relationships(path) as pathRels,
//                               finalRisk,
//                               length(path) as pathLength
//                        ORDER BY finalRisk DESC
//                        LIMIT 5
//                        """;
//
//                 double initialRisk = calculateInitialRisk(riskSource);
//
//                 var result = session.readTransaction(tx->tx.run(riskPropagationQuery,
//                         Map.of("sourceCui", riskSource.getCui(),
//                                 "targetCui", symptom.getCui(),
//                                 "initialRisk", initialRisk,
//                                 "decay", decayFactor
//                         )));
//                 result.forEachRemaining(record -> {
//                     try{
//                         double finalRisk = record.get("finalRisk").asDouble();
//                         symptomRiskScores.merge(symptom.getCui(), finalRisk, Double::sum);
//
//                         RiskPropagationPath path = parseRiskPropagationPath(record, riskSource, symptom);
//                         if (path != null) {
//                             propagationPaths.add(path);
//                         }
//                     }catch(Exception e){
//                         logger.error("Error parsing risk propagation path: {}",e.getMessage());
//
//                     }
//                 });
//             }
//         }
//     }catch(Exception e){
//         logger.error("Error in risk propagation calculation: {}", e.getMessage(), e);
//     }
//
//     RiskPropagationResult result = new RiskPropagationResult();
//     result.setSymptomRiskScores(symptomRiskScores);
//     result.setPropagationPaths(propagationPaths);
//     result.setTotalRiskScore(symptomRiskScores.values().stream().mapToDouble(Double::doubleValue).sum());
//
//     logger.info("Risk propagation complete. Total risk: {:.3f}", result.getTotalRiskScore());
//     return result;
// }
//
//
//
///**
// * ניתוח centrality למציאת "hub" רפואיים חשובים
// * מזהה איזה מחלות/תרופות הכי משפיעות ברשת
// */
//public List<MedicalHub> findMedicalHubs(List<UserMedicalEntity> userContext) {
//    List<MedicalHub> hubs = new ArrayList<>();
//
//    try(Session session = neo4jDriver.session()) {
//        String userCuis = userContext.stream()
//                .map(UserMedicalEntity::getCui)
//                .collect(Collectors.joining("', '", "'", "'"));
//
//        String centralityQuery = """
//                CALL gds.graph.project.cypher(
//                    'centrality-network',
//                    'MATCH (n) WHERE n.cui IN $userCuis RETURN id(n) AS id, n.cui AS cui, n.name AS name',
//                    'MATCH (n)-[r]-(m) WHERE n.cui IN $userCuis AND m.cui IN $userCuis
//                     RETURN id(n) AS source, id(m) AS target, coalesce(r.weight, 1.0) AS weight'
//                )
//                YIELD graphName
//
//                CALL gds.betweenness.stream(graphName, {relationshipWeightProperty: 'weight'})
//                YIELD nodeId, score
//
//                WITH gds.util.asNode(nodeId) AS node, score
//                WHERE score > 0
//                RETURN node.cui AS cui, node.name AS name, score,
//                       labels(node)[0] AS type
//                ORDER BY score DESC
//                LIMIT 10
//                """.formatted(userCuis, userCuis, userCuis);
//
//        var result = session.readTransaction(tx ->
//                tx.run(centralityQuery, Map.of("userCuis", userCuis)).list()
//        );
//        result.forEachRemaining(record -> {
//            try {
//                MedicalHub hub = new MedicalHub();
//                hub.setCui(record.get("cui").asString());
//                hub.setName(record.get("name").asString());
//                hub.setType(record.get("type").asString());
//                hub.setCentralityScore(record.get("score").asDouble());
//                hub.setInfluenceLevel(categorizeInfluence(hub.getCentralityScore()));
//                hubs.add(hub);
//            }catch(Exception e){
//                logger.error("Error parsing hub: {}",e.getMessage());
//            }
//        });
//        session.writeTransaction(tx->{
//            tx.run("CALL gds.graph.drop('centrality-network', false)");
//            return null;
//        });
//    }catch(Exception e){
//        logger.error("Error in centrality analysis: {}", e.getMessage(), e);
//    }
//    logger.info("Found {} medical hubs", hubs.size());
//    return hubs;
//}
//
//
//
//
//    private MedicalPathway parsePathwayFromRecord(Record record, String sourceCui, ExtractedSymptom targetSymptom) {
//        try{
//            List<Object> pathNodes = record.get("pathNodes").asList();
//            List<Object> pathRelationships = record.get("pathRelationships").asList();
//            double riskScore = record.get("riskScore").asDouble();
//            int pathLength = record.get("pathLength").asInt();
//
//            MedicalPathway pathway = new MedicalPathway();
//            pathway.setSourceCui(sourceCui);
//            pathway.setTargetCui(targetSymptom.getCui());
//            pathway.setTargetName(targetSymptom.getName());
//            pathway.setRiskScore(riskScore);
//            pathway.setPathLength(pathLength);
//            pathway.setConfidence(calculatePathwayConfidence(riskScore, pathLength));
//
//            List<PathNode> nodes = pathNodes.stream()
//                    .map(node->{
//                        @SuppressWarnings("unchecked")
//                                Map<String, Object> nodeMap = (Map<String, Object>) node;
//                        PathNode pathNode = new PathNode();
//                        pathNode.setCui((String)nodeMap.get("cui"));
//                        pathNode.setName((String)nodeMap.get("name"));
//                        pathNode.setType((String)nodeMap.get("type"));
//                        return pathNode;
//                    }).collect(Collectors.toList());
//            pathway.setNodes(nodes);
//
//            List<PathRelationship> relationships = pathRelationships.stream()
//                    .map(rel->{
//                        @SuppressWarnings("unchecked")
//                                Map<String, Object> relMap = (Map<String, Object>) rel;
//                        PathRelationship pathRel = new PathRelationship();
//                        pathRel.setType((String)relMap.get("type"));
//                        pathRel.setWeight(((Number) relMap.get("weight")).doubleValue());
//                        return pathRel;
//                    }).collect(Collectors.toList());
//            pathway.setRelationships(relationships);
//            pathway.setExplanation(generatePathwayExplanation(pathway));
//            return pathway;
//        } catch (Exception e) {
//            logger.error("Error parsing pathway: {}", e.getMessage());
//            return null;
//        }
//    }
//
//    private MedicalCommunity parseCommunityFromRecord(Record record) {
//        try{
//            MedicalCommunity community = new MedicalCommunity();
//
//            community.setCommunityId(record.get("communityId").asLong());
//            community.setSize(record.get("size").asInt());
//            List<Object> members = record.get("members").asList();
//
//            List<CommunityMember> communityMembers = members.stream()
//                    .map(member->{
//                        @SuppressWarnings("unchecked")
//                                Map<String, Object> memberMap = (Map<String, Object>) member;
//                        CommunityMember cm = new CommunityMember();
//                        cm.setCui((String)memberMap.get("cui"));
//                        cm.setName((String)memberMap.get("name"));
//                        cm.setType((String)memberMap.get("type"));
//                        return cm;
//                    }).collect(Collectors.toList());
//            community.setMembers(communityMembers);
//
//            community.setCohesionScore(calculateCohesionScore(community));
//            community.setDominantType(findDominantType(communityMembers));
//            community.setDescription(generateCommunityDescription(community));
//
//            return community;
//        } catch (Exception e) {
//            logger.error("Error parsing community: {}", e.getMessage());
//            return null;
//        }
//    }
//
//    private RiskPropagationPath parseRiskPropagationPath(Record record, UserMedicalEntity source, ExtractedSymptom target) {
//     try {
//         int pathLength = record.get("pathLength").asInt();
//
//         RiskPropagationPath path = new RiskPropagationPath();
//         path.setSourceCui(source.getCui());
//         path.setSourceName(source.getName());
//         path.setTargetCui(target.getCui());
//         path.setTargetName(target.getName());
//         path.setFinalRisk(record.get("finalRisk").asDouble());
//         path.setPathLength(pathLength);
//         path.setDecayFactor(Math.pow(0.85, pathLength));
//
//         return path;
//     }catch (Exception e) {
//         logger.error("Error parsing path: {}", e.getMessage());
//         return null;
//     }
//    }
//
//    private double calculateInitialRisk(UserMedicalEntity entity){
//     return switch (entity.getType()) {
//         case "disease" ->entity.getSeverity() !=null?
//                 switch (entity.getSeverity().toLowerCase()){
//             case "severe"->0.9;
//             case "moderate"->0.6;
//             case "mild"->0.3;
//                     default -> 0.5;
//                 }:0.5;
//         case "riskfactor"->entity.getSeverity() !=null && entity.getAdditionalData().containsKey("weight")?
//                 ((Number)entity.getAdditionalData().get("weight")).doubleValue():0.4;
//         default -> 0.3;
//     };
//    }
//
//    // ביטחון יורד עם אורך המסלול אבל עולה עם רמת הסיכון
//    private double calculatePathwayConfidence(double riskScore, int pathLength) {
//        return riskScore * Math.pow(0.9, pathLength-1);
//    }
//
//
//    private double calculateCohesionScore(MedicalCommunity community) {
//        double baseScore = Math.min(1.0, community.getSize()/10.0);
//
//        Set<String> uniqueTypes = community.getMembers().stream()
//                .map(CommunityMember::getType)
//                .collect(Collectors.toSet());
//        double diversityBonus = uniqueTypes.size() > 1 ? 0.2 : 0.0;
//
//        return Math.min(1.0, baseScore + diversityBonus);
//    }
//
//    private String findDominantType(List<CommunityMember> members) {
//        return members.stream()
//                .collect(Collectors.groupingBy(CommunityMember::getType, Collectors.counting()))
//                .entrySet().stream()
//                .max(Map.Entry.comparingByValue())
//                .map(Map.Entry::getKey)
//                .orElse("Mixed");
//    }
//
//    private String generatePathwayExplanation(MedicalPathway pathway) {
//        StringBuilder explanation = new StringBuilder();
//
//        if(pathway.getNodes().size()>=2){
//            PathNode first = pathway.getNodes().get(0);
//            PathNode last = pathway.getNodes().get(pathway.getNodes().size() - 1);
//
//            explanation.append(String.format("Medical pathway from %s to %s ",
//                    first.getName(), last.getName()));
//            explanation.append(String.format("through %d intermediate steps. ", pathway.getPathLength() - 1));
//            explanation.append(String.format("Risk score: %.2f, Confidence: %.2f",
//                    pathway.getRiskScore(), pathway.getConfidence()));
//        }
//        return explanation.toString();
//    }
//
//    private String generateCommunityDescription(MedicalCommunity community) {
//        return String.format("Medical community of %d entities (primarily %s) with cohesion score %.2f",
//                community.getSize(), community.getDominantType(), community.getCohesionScore());
//    }
//
//    private String categorizeInfluence(double centralityScore) {
//        if (centralityScore > 50) return "Very High";
//        if (centralityScore > 20) return "High";
//        if (centralityScore > 5) return "Medium";
//        return "Low";
//    }
//
//
//
//
//
//
//
//
//    @Data
//    public static class MedicalPathway {
//        private String sourceCui;
//        private String targetCui;
//        private String targetName;
//        private double riskScore;
//        private int pathLength;
//        private double confidence;
//        private List<PathNode> nodes;
//        private List<PathRelationship> relationships;
//        private String explanation;
//    }
//
//    @Data
//    public static class PathNode {
//        private String cui;
//        private String name;
//        private String type;
//
//        public String getCui() { return cui; }
//        public void setCui(String cui) { this.cui = cui; }
//        public String getName() { return name; }
//        public void setName(String name) { this.name = name; }
//        public String getType() { return type; }
//        public void setType(String type) { this.type = type; }
//    }
//
//    @Data
//    public static class PathRelationship {
//        private String type;
//        private double weight;
//
//    }
//
//    @Data
//    public static class MedicalCommunity {
//        private long communityId;
//        private int size;
//        private List<CommunityMember> members;
//        private double cohesionScore;
//        private String dominantType;
//        private String description;
//
//    }
//
//    @Data
//    public static class CommunityMember {
//        private String cui;
//        private String name;
//        private String type;
//
//    }
//
//    @Data
//    public static class MedicalHub {
//        private String cui;
//        private String name;
//        private String type;
//        private double centralityScore;
//        private String influenceLevel;
//
//    }
//
//    @Data
//    public static class RiskPropagationResult {
//        private Map<String, Double> symptomRiskScores;
//        private List<RiskPropagationPath> propagationPaths;
//        private double totalRiskScore;
//
//    }
//
//    @Data
//    public static class RiskPropagationPath {
//        private String sourceCui;
//        private String sourceName;
//        private String targetCui;
//        private String targetName;
//        private double finalRisk;
//        private int pathLength;
//        private double decayFactor;
//
//    }
//}
package com.example.mediaid.bl.emergency;

import com.example.mediaid.dto.emergency.ExtractedSymptom;
import com.example.mediaid.dto.emergency.UserMedicalEntity;
import lombok.Data;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neo4j.driver.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * שירות Analytics מתקדם לגרף רפואי - Graph Thinking אמיתי
 */
@Service
public class MedicalGraphAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(MedicalGraphAnalyticsService.class);

    @Autowired
    private Driver neo4jDriver;

    //מציאת מסלולים רפואיים עד 5 צעדים - עם תיקון פונקציות מתמטיות
    public List<MedicalPathway> findMedicalPathways(String sourceCui, Set<ExtractedSymptom> targetSymptoms, int maxDepth) {
        logger.info("Finding advanced pathways from {} to {} symptoms (max depth: {})", sourceCui, targetSymptoms.size(), maxDepth);
        List<MedicalPathway> pathways = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            for(ExtractedSymptom symptom : targetSymptoms) {
                // תיקון: שימוש בחישוב decay ללא pow/power
                String advancedPathQuery = """
                        MATCH (source {cui: $sourceCui})
                        CALL apoc.path.expandConfig(source, {
                            relationshipFilter: "TREATS|CAUSES_SIDE_EFFECT|INDICATES|RISK_FACTOR_FOR|INFLUENCES>",
                            labelFilter: "+Disease|+Medication|+Symptom|+RiskFactor",
                            minLevel: 1,
                            maxLevel: $maxDepth,
                            limit: 15,
                            endNodes: [{cui: $targetCui}]
                        }) YIELD path
                        WHERE last(nodes(path)).cui = $targetCui
                        WITH path,
                             reduce(pathWeight = 1.0, rel in relationships(path) | 
                                    pathWeight * rel.weight * 
                                    CASE length(path)
                                        WHEN 1 THEN 0.85
                                        WHEN 2 THEN 0.7225  
                                        WHEN 3 THEN 0.614
                                        WHEN 4 THEN 0.522
                                        WHEN 5 THEN 0.444
                                        ELSE 0.4
                                    END) as riskScore,
                             [node in nodes(path) | {cui: node.cui, name: node.name, type: labels(node)[0]}] as pathNodes,
                             [rel in relationships(path) | {type: type(rel), weight: rel.weight}] as pathRelationships
                        RETURN pathNodes, pathRelationships, riskScore, length(path) as pathLength
                        ORDER BY riskScore DESC, pathLength ASC
                        LIMIT 10
                        """;

                // תיקון: צריכה נכונה של התוצאות
                List<Record> records = session.readTransaction(tx->
                        tx.run(advancedPathQuery, Map.of(
                                "sourceCui", sourceCui,
                                "targetCui", symptom.getCui(),
                                "maxDepth", maxDepth
                        )).list());

                for (Record record : records) {
                    try{
                        MedicalPathway pathway = parsePathwayFromRecord(record, sourceCui, symptom);
                        if(pathway != null && pathway.getRiskScore() > 0.1) {
                            pathways.add(pathway);
                        }
                    }catch(Exception e){
                        logger.error("Error parsing pathway: {}",e.getMessage());
                    }
                }
            }
        }catch(Exception e){
            logger.error("Error in pathway finding: {}",e.getMessage());
        }
        logger.info("Found {} pathways", pathways.size());
        return pathways.stream()
                .sorted((p1, p2)->Double.compare(p2.getRiskScore(), p1.getRiskScore()))
                .collect(Collectors.toList());
    }

    /**
     * זיהוי קהילות רפואיות - עם fallback אם GDS לא זמין
     */
    public List<MedicalCommunity> detectMedicalCommunities(List<UserMedicalEntity> userContext) {
        logger.info("Detecting medical communities using Louvain algorithm");
        List<MedicalCommunity> communities = new ArrayList<>();

        try(Session session = neo4jDriver.session()) {
            // בדיקה אם GDS זמין
            boolean gdsAvailable = checkGDSAvailability(session);

            if (gdsAvailable) {
                return detectCommunitiesWithGDS(session, userContext);
            } else {
                return detectCommunitiesWithBasicCypher(session, userContext);
            }

        } catch (Exception e) {
            logger.error("Error detecting medical communities: {}", e.getMessage());
            // fallback לגישה בסיסית
            return detectCommunitiesBasic(userContext);
        }
    }

    private boolean checkGDSAvailability(Session session) {
        try {
            session.readTransaction(tx -> {
                tx.run("CALL gds.version()").list();
                return null;
            });
            return true;
        } catch (Exception e) {
            logger.warn("GDS not available, using basic community detection");
            return false;
        }
    }

    private List<MedicalCommunity> detectCommunitiesWithGDS(Session session, List<UserMedicalEntity> userContext) {
        List<MedicalCommunity> communities = new ArrayList<>();

        try {
            List<String> userCuis = userContext.stream()
                    .map(UserMedicalEntity::getCui)
                    .collect(Collectors.toList());

            String communityDetectionQuery = """
                CALL gds.graph.project.cypher(
                    'user-medical-network',
                    'MATCH (n) WHERE n.cui IN $userCuis RETURN id(n) AS id, labels(n)[0] AS type',
                    'MATCH (n)-[r]-(m) WHERE n.cui IN $userCuis AND m.cui IN $userCuis 
                     RETURN id(n) AS source, id(m) AS target, 
                            coalesce(r.weight, 0.5) AS weight, type(r) AS relationshipType'
                )
                YIELD graphName
                
                CALL gds.louvain.stream(graphName, {
                    relationshipWeightProperty: 'weight',
                    includeIntermediateCommunities: true,
                    maxLevels: 3,
                    tolerance: 0.001
                })
                YIELD nodeId, communityId, intermediateCommunityIds
                
                WITH gds.util.asNode(nodeId) AS node, communityId, intermediateCommunityIds
                RETURN communityId, 
                       collect({cui: node.cui, name: node.name, type: labels(node)[0]}) AS members,
                       count(*) AS size
                ORDER BY size DESC
                LIMIT 10
                """;

            List<Record> records = session.readTransaction(tx->
                    tx.run(communityDetectionQuery, Map.of("userCuis", userCuis)).list());

            for (Record record : records) {
                try{
                    MedicalCommunity community = parseCommunityFromRecord(record);
                    if (community != null && community.getSize() >=2) {
                        communities.add(community);
                    }
                }catch(Exception e){
                    logger.error("Error parsing community: {}",e.getMessage());
                }
            }

            //ניקוי הגרף הזמני
            session.writeTransaction(tx->{
                tx.run("CALL gds.graph.drop('user-medical-network', false)").list();
                return null;
            });

        } catch (Exception e) {
            logger.error("Error in GDS community detection: {}", e.getMessage());
        }

        return communities;
    }

    private List<MedicalCommunity> detectCommunitiesWithBasicCypher(Session session, List<UserMedicalEntity> userContext) {
        List<MedicalCommunity> communities = new ArrayList<>();

        try {
            List<String> userCuis = userContext.stream()
                    .map(UserMedicalEntity::getCui)
                    .collect(Collectors.toList());

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

            List<Record> records = session.readTransaction(tx ->
                    tx.run(basicCommunityQuery, Map.of("userCuis", userCuis)).list()
            );

            long communityId = 1;
            for (Record record : records) {
                try {
                    MedicalCommunity community = new MedicalCommunity();
                    community.setCommunityId(communityId++);
                    community.setSize(record.get("size").asInt());

                    List<Object> members = record.get("members").asList();
                    List<CommunityMember> communityMembers = members.stream()
                            .map(member -> {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> memberMap = (Map<String, Object>) member;
                                CommunityMember cm = new CommunityMember();
                                cm.setCui((String) memberMap.get("cui"));
                                cm.setName((String) memberMap.get("name"));
                                cm.setType((String) memberMap.get("type"));
                                return cm;
                            }).collect(Collectors.toList());

                    community.setMembers(communityMembers);
                    community.setCohesionScore(calculateCohesionScore(community));
                    community.setDominantType(findDominantType(communityMembers));
                    community.setDescription(generateCommunityDescription(community));

                    communities.add(community);
                } catch (Exception e) {
                    logger.error("Error parsing basic community: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Error in basic community detection: {}", e.getMessage());
        }

        return communities;
    }

    private List<MedicalCommunity> detectCommunitiesBasic(List<UserMedicalEntity> userContext) {
        // fallback פשוט ללא גרף
        List<MedicalCommunity> communities = new ArrayList<>();

        if (userContext.size() >= 3) {
            MedicalCommunity community = new MedicalCommunity();
            community.setCommunityId(1L);
            community.setSize(userContext.size());

            List<CommunityMember> members = userContext.stream()
                    .map(entity -> {
                        CommunityMember member = new CommunityMember();
                        member.setCui(entity.getCui());
                        member.setName(entity.getName());
                        member.setType(entity.getType());
                        return member;
                    }).collect(Collectors.toList());

            community.setMembers(members);
            community.setCohesionScore(0.5);
            community.setDominantType(findDominantType(members));
            community.setDescription("Basic user medical profile community");

            communities.add(community);
        }

        return communities;
    }

    /**
     * חישוב סיכונים עם הגרף - עם תיקון פונקציות מתמטיות
     */
    public RiskPropagationResult calculateRiskPropagation(List<UserMedicalEntity> riskSources, Set<ExtractedSymptom> targetSymptoms, double decayFactor) {
        logger.info("Calculating risk propagation from {} sources to {} targets",
                riskSources.size(), targetSymptoms.size());
        Map<String, Double> symptomRiskScores = new HashMap<>();
        List<RiskPropagationPath> propagationPaths = new ArrayList<>();

        try(Session session = neo4jDriver.session()) {
            for(UserMedicalEntity riskSource : riskSources) {
                for(ExtractedSymptom symptom : targetSymptoms) {
                    // תיקון: שימוש בחישוב decay ללא pow
                    String riskPropagationQuery = """
                            MATCH (source {cui: $sourceCui})
                            CALL apoc.path.expandConfig(source, {
                                relationshipFilter: "RISK_FACTOR_FOR|CAUSES|INFLUENCES|LEADS_TO>",
                                maxLevel: 4,
                                limit: 20
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

                    double initialRisk = calculateInitialRisk(riskSource);

                    List<Record> records = session.readTransaction(tx->
                            tx.run(riskPropagationQuery, Map.of(
                                    "sourceCui", riskSource.getCui(),
                                    "targetCui", symptom.getCui(),
                                    "initialRisk", initialRisk,
                                    "decay", decayFactor
                            )).list());

                    for (Record record : records) {
                        try{
                            double finalRisk = record.get("finalRisk").asDouble();
                            symptomRiskScores.merge(symptom.getCui(), finalRisk, Double::sum);

                            RiskPropagationPath path = parseRiskPropagationPath(record, riskSource, symptom);
                            if (path != null) {
                                propagationPaths.add(path);
                            }
                        }catch(Exception e){
                            logger.error("Error parsing risk propagation path: {}",e.getMessage());
                        }
                    }
                }
            }
        }catch(Exception e){
            logger.error("Error in risk propagation calculation: {}", e.getMessage(), e);
        }

        RiskPropagationResult result = new RiskPropagationResult();
        result.setSymptomRiskScores(symptomRiskScores);
        result.setPropagationPaths(propagationPaths);
        result.setTotalRiskScore(symptomRiskScores.values().stream().mapToDouble(Double::doubleValue).sum());

        logger.info("Risk propagation complete. Total risk: {:.3f}", result.getTotalRiskScore());
        return result;
    }

    /**
     * ניתוח centrality למציאת "hub" רפואיים חשובים - עם תיקון parameters
     */
    public List<MedicalHub> findMedicalHubs(List<UserMedicalEntity> userContext) {
        List<MedicalHub> hubs = new ArrayList<>();

        try(Session session = neo4jDriver.session()) {
            List<String> userCuis = userContext.stream()
                    .map(UserMedicalEntity::getCui)
                    .collect(Collectors.toList());

            // תיקון: שימוש ב-parameters במקום String.format
            String centralityQuery = """
                    CALL gds.graph.project.cypher(
                        'centrality-network',
                        'MATCH (n) WHERE n.cui IN $userCuis RETURN id(n) AS id, n.cui AS cui, n.name AS name',
                        'MATCH (n)-[r]-(m) WHERE n.cui IN $userCuis AND m.cui IN $userCuis 
                         RETURN id(n) AS source, id(m) AS target, coalesce(r.weight, 1.0) AS weight'
                    )
                    YIELD graphName
                    
                    CALL gds.betweenness.stream(graphName, {relationshipWeightProperty: 'weight'})
                    YIELD nodeId, score
                    
                    WITH gds.util.asNode(nodeId) AS node, score
                    WHERE score > 0
                    RETURN node.cui AS cui, node.name AS name, score,
                           labels(node)[0] AS type
                    ORDER BY score DESC
                    LIMIT 10
                    """;

            try {
                List<Record> records = session.readTransaction(tx->
                        tx.run(centralityQuery, Map.of("userCuis", userCuis)).list());

                for (Record record : records) {
                    try {
                        MedicalHub hub = new MedicalHub();
                        hub.setCui(record.get("cui").asString());
                        hub.setName(record.get("name").asString());
                        hub.setType(record.get("type").asString());
                        hub.setCentralityScore(record.get("score").asDouble());
                        hub.setInfluenceLevel(categorizeInfluence(hub.getCentralityScore()));
                        hubs.add(hub);
                    }catch(Exception e){
                        logger.error("Error parsing hub: {}",e.getMessage());
                    }
                }

                // ניקוי הגרף הזמני
                session.writeTransaction(tx->{
                    tx.run("CALL gds.graph.drop('centrality-network', false)").list();
                    return null;
                });

            } catch (Exception e) {
                logger.warn("GDS centrality analysis failed, using basic approach: {}", e.getMessage());
                // fallback לגישה בסיסית
                hubs = findHubsBasic(session, userCuis);
            }

        }catch(Exception e){
            logger.error("Error in centrality analysis: {}", e.getMessage(), e);
        }
        logger.info("Found {} medical hubs", hubs.size());
        return hubs;
    }

    private List<MedicalHub> findHubsBasic(Session session, List<String> userCuis) {
        List<MedicalHub> hubs = new ArrayList<>();

        try {
            String basicHubQuery = """
                MATCH (n)-[r]-(m) 
                WHERE n.cui IN $userCuis AND m.cui IN $userCuis
                WITH n, count(r) as connectionCount
                WHERE connectionCount > 1
                RETURN n.cui as cui, n.name as name, labels(n)[0] as type, connectionCount as score
                ORDER BY connectionCount DESC
                LIMIT 5
                """;

            List<Record> records = session.readTransaction(tx ->
                    tx.run(basicHubQuery, Map.of("userCuis", userCuis)).list()
            );

            for (Record record : records) {
                MedicalHub hub = new MedicalHub();
                hub.setCui(record.get("cui").asString());
                hub.setName(record.get("name").asString());
                hub.setType(record.get("type").asString());
                hub.setCentralityScore(record.get("score").asDouble());
                hub.setInfluenceLevel(categorizeInfluence(hub.getCentralityScore()));
                hubs.add(hub);
            }

        } catch (Exception e) {
            logger.error("Error in basic hub detection: {}", e.getMessage());
        }

        return hubs;
    }

    // שאר הפונקציות נשארות זהות...
    private MedicalPathway parsePathwayFromRecord(Record record, String sourceCui, ExtractedSymptom targetSymptom) {
        try{
            List<Object> pathNodes = record.get("pathNodes").asList();
            List<Object> pathRelationships = record.get("pathRelationships").asList();
            double riskScore = record.get("riskScore").asDouble();
            int pathLength = record.get("pathLength").asInt();

            MedicalPathway pathway = new MedicalPathway();
            pathway.setSourceCui(sourceCui);
            pathway.setTargetCui(targetSymptom.getCui());
            pathway.setTargetName(targetSymptom.getName());
            pathway.setRiskScore(riskScore);
            pathway.setPathLength(pathLength);
            pathway.setConfidence(calculatePathwayConfidence(riskScore, pathLength));

            List<PathNode> nodes = pathNodes.stream()
                    .map(node->{
                        @SuppressWarnings("unchecked")
                        Map<String, Object> nodeMap = (Map<String, Object>) node;
                        PathNode pathNode = new PathNode();
                        pathNode.setCui((String)nodeMap.get("cui"));
                        pathNode.setName((String)nodeMap.get("name"));
                        pathNode.setType((String)nodeMap.get("type"));
                        return pathNode;
                    }).collect(Collectors.toList());
            pathway.setNodes(nodes);

            List<PathRelationship> relationships = pathRelationships.stream()
                    .map(rel->{
                        @SuppressWarnings("unchecked")
                        Map<String, Object> relMap = (Map<String, Object>) rel;
                        PathRelationship pathRel = new PathRelationship();
                        pathRel.setType((String)relMap.get("type"));
                        pathRel.setWeight(((Number) relMap.get("weight")).doubleValue());
                        return pathRel;
                    }).collect(Collectors.toList());
            pathway.setRelationships(relationships);
            pathway.setExplanation(generatePathwayExplanation(pathway));
            return pathway;
        } catch (Exception e) {
            logger.error("Error parsing pathway: {}", e.getMessage());
            return null;
        }
    }

    private MedicalCommunity parseCommunityFromRecord(Record record) {
        try{
            MedicalCommunity community = new MedicalCommunity();

            community.setCommunityId(record.get("communityId").asLong());
            community.setSize(record.get("size").asInt());
            List<Object> members = record.get("members").asList();

            List<CommunityMember> communityMembers = members.stream()
                    .map(member->{
                        @SuppressWarnings("unchecked")
                        Map<String, Object> memberMap = (Map<String, Object>) member;
                        CommunityMember cm = new CommunityMember();
                        cm.setCui((String)memberMap.get("cui"));
                        cm.setName((String)memberMap.get("name"));
                        cm.setType((String)memberMap.get("type"));
                        return cm;
                    }).collect(Collectors.toList());
            community.setMembers(communityMembers);

            community.setCohesionScore(calculateCohesionScore(community));
            community.setDominantType(findDominantType(communityMembers));
            community.setDescription(generateCommunityDescription(community));

            return community;
        } catch (Exception e) {
            logger.error("Error parsing community: {}", e.getMessage());
            return null;
        }
    }

    private RiskPropagationPath parseRiskPropagationPath(Record record, UserMedicalEntity source, ExtractedSymptom target) {
        try {
            int pathLength = record.get("pathLength").asInt();

            RiskPropagationPath path = new RiskPropagationPath();
            path.setSourceCui(source.getCui());
            path.setSourceName(source.getName());
            path.setTargetCui(target.getCui());
            path.setTargetName(target.getName());
            path.setFinalRisk(record.get("finalRisk").asDouble());
            path.setPathLength(pathLength);
            path.setDecayFactor(Math.pow(0.85, pathLength));

            return path;
        }catch (Exception e) {
            logger.error("Error parsing path: {}", e.getMessage());
            return null;
        }
    }

    private double calculateInitialRisk(UserMedicalEntity entity){
        return switch (entity.getType()) {
            case "disease" ->entity.getSeverity() !=null?
                    switch (entity.getSeverity().toLowerCase()){
                        case "severe"->0.9;
                        case "moderate"->0.6;
                        case "mild"->0.3;
                        default -> 0.5;
                    }:0.5;
            case "riskfactor"->entity.getSeverity() !=null && entity.getAdditionalData().containsKey("weight")?
                    ((Number)entity.getAdditionalData().get("weight")).doubleValue():0.4;
            default -> 0.3;
        };
    }

    // ביטחון יורד עם אורך המסלול אבל עולה עם רמת הסיכון
    private double calculatePathwayConfidence(double riskScore, int pathLength) {
        return riskScore * Math.pow(0.9, pathLength-1);
    }

    private double calculateCohesionScore(MedicalCommunity community) {
        double baseScore = Math.min(1.0, community.getSize()/10.0);

        Set<String> uniqueTypes = community.getMembers().stream()
                .map(CommunityMember::getType)
                .collect(Collectors.toSet());
        double diversityBonus = uniqueTypes.size() > 1 ? 0.2 : 0.0;

        return Math.min(1.0, baseScore + diversityBonus);
    }

    private String findDominantType(List<CommunityMember> members) {
        return members.stream()
                .collect(Collectors.groupingBy(CommunityMember::getType, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Mixed");
    }

    private String generatePathwayExplanation(MedicalPathway pathway) {
        StringBuilder explanation = new StringBuilder();

        if(pathway.getNodes().size()>=2){
            PathNode first = pathway.getNodes().get(0);
            PathNode last = pathway.getNodes().get(pathway.getNodes().size() - 1);

            explanation.append(String.format("Medical pathway from %s to %s ",
                    first.getName(), last.getName()));
            explanation.append(String.format("through %d intermediate steps. ", pathway.getPathLength() - 1));
            explanation.append(String.format("Risk score: %.2f, Confidence: %.2f",
                    pathway.getRiskScore(), pathway.getConfidence()));
        }
        return explanation.toString();
    }

    private String generateCommunityDescription(MedicalCommunity community) {
        return String.format("Medical community of %d entities (primarily %s) with cohesion score %.2f",
                community.getSize(), community.getDominantType(), community.getCohesionScore());
    }

    private String categorizeInfluence(double centralityScore) {
        if (centralityScore > 50) return "Very High";
        if (centralityScore > 20) return "High";
        if (centralityScore > 5) return "Medium";
        return "Low";
    }

    // Data classes נשארות זהות...
    @Data
    public static class MedicalPathway {
        private String sourceCui;
        private String targetCui;
        private String targetName;
        private double riskScore;
        private int pathLength;
        private double confidence;
        private List<PathNode> nodes;
        private List<PathRelationship> relationships;
        private String explanation;
    }

    @Data
    public static class PathNode {
        private String cui;
        private String name;
        private String type;

        public String getCui() { return cui; }
        public void setCui(String cui) { this.cui = cui; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    @Data
    public static class PathRelationship {
        private String type;
        private double weight;
    }

    @Data
    public static class MedicalCommunity {
        private long communityId;
        private int size;
        private List<CommunityMember> members;
        private double cohesionScore;
        private String dominantType;
        private String description;
    }

    @Data
    public static class CommunityMember {
        private String cui;
        private String name;
        private String type;
    }

    @Data
    public static class MedicalHub {
        private String cui;
        private String name;
        private String type;
        private double centralityScore;
        private String influenceLevel;
    }

    @Data
    public static class RiskPropagationResult {
        private Map<String, Double> symptomRiskScores;
        private List<RiskPropagationPath> propagationPaths;
        private double totalRiskScore;
    }

    @Data
    public static class RiskPropagationPath {
        private String sourceCui;
        private String sourceName;
        private String targetCui;
        private String targetName;
        private double finalRisk;
        private int pathLength;
        private double decayFactor;
    }
}