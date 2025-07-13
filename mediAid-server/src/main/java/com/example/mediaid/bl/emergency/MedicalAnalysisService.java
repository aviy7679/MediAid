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
//@Service
//public class MedicalGraphAnalyticsService {
//
//    private static final Logger logger = LoggerFactory.getLogger(MedicalGraphAnalyticsService.class);
//
//    @Autowired
//    private Driver neo4jDriver;
//
//    //מציאת מסלולים רפואיים עד 5 צעדים - עם תיקון פונקציות מתמטיות
//    public List<MedicalPathway> findMedicalPathways(String sourceCui, Set<ExtractedSymptom> targetSymptoms, int maxDepth) {
//        logger.info("Finding advanced pathways from {} to {} symptoms (max depth: {})", sourceCui, targetSymptoms.size(), maxDepth);
//        List<MedicalPathway> pathways = new ArrayList<>();
//        try (Session session = neo4jDriver.session()) {
//            for(ExtractedSymptom symptom : targetSymptoms) {
//                String advancedPathQuery = """
//                MATCH (source {cui: $sourceCui})
//                MATCH (target {cui: $targetCui})
//                CALL apoc.path.expandConfig(source, {
//                    relationshipFilter: "TREATS|CAUSES_SIDE_EFFECT|INDICATES|RISK_FACTOR_FOR|INFLUENCES>",
//                    labelFilter: "+Disease|+Medication|+Symptom|+RiskFactor",
//                    minLevel: 1,
//                    maxLevel: $maxDepth,
//                    limit: 15,
//                    endNodes: [target]
//                }) YIELD path
//                WHERE last(nodes(path)).cui = $targetCui
//                WITH path,
//                     // חישוב מתקן של risk score ללא exp/ln
//                     reduce(pathWeight = 0.0, rel in relationships(path) |
//                            pathWeight + rel.weight) / size(relationships(path)) *
//                     CASE size(relationships(path))
//                         WHEN 1 THEN 0.85
//                         WHEN 2 THEN 0.72  // 0.85^2 מחושב מראש
//                         WHEN 3 THEN 0.61  // 0.85^3 מחושב מראש
//                         WHEN 4 THEN 0.52  // 0.85^4 מחושב מראש
//                         WHEN 5 THEN 0.44  // 0.85^5 מחושב מראש
//                         ELSE 0.35
//                     END as riskScore,
//                     [node in nodes(path) | {
//                         cui: node.cui,
//                         name: node.name,
//                         type: labels(node)[0]
//                     }] as pathNodes,
//                     [rel in relationships(path) | {
//                         type: type(rel),
//                         weight: rel.weight,
//                         source: startNode(rel).name,
//                         target: endNode(rel).name
//                     }] as pathRelationships
//                RETURN pathNodes, pathRelationships,
//                       riskScore,
//                       size(relationships(path)) as pathLength
//                ORDER BY riskScore DESC, pathLength ASC
//                LIMIT 10
//                """;
//
//                List<Record> records = session.readTransaction(tx ->
//                        tx.run(advancedPathQuery, Map.of(
//                                "sourceCui", sourceCui,
//                                "targetCui", symptom.getCui(),
//                                "maxDepth", maxDepth
//                        )).list());
//
//                for (Record record : records) {
//                    try{
//                        MedicalPathway pathway = parsePathwayFromRecord(record, sourceCui, symptom);
//                        if(pathway != null && pathway.getRiskScore() > 0.1) {
//                            pathways.add(pathway);
//                        }
//                    }catch(Exception e){
//                        logger.error("Error parsing pathway: {}",e.getMessage());
//                    }
//                }
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
//    private List<MedicalCommunity> createFallbackCommunity(List<UserMedicalEntity> userContext) {
//        List<MedicalCommunity> communities = new ArrayList<>();
//
//        if (userContext.size() >= 2) {
//            MedicalCommunity community = new MedicalCommunity();
//            community.setCommunityId(1L);
//            community.setSize(userContext.size());
//
//            List<CommunityMember> members = userContext.stream()
//                    .map(entity -> {
//                        CommunityMember member = new CommunityMember();
//                        member.setCui(entity.getCui());
//                        member.setName(entity.getName());
//                        member.setType(entity.getType());
//                        return member;
//                    }).collect(Collectors.toList());
//
//            community.setMembers(members);
//            community.setCohesionScore(0.5);
//            community.setDominantType(findDominantType(members));
//            community.setDescription("User medical profile community (fallback)");
//
//            communities.add(community);
//        }
//
//        return communities;
//    }
//
//private List<MedicalCommunity> detectCommunitiesWithGDS(Session session, List<String> userCuis) {
//    List<MedicalCommunity> communities = new ArrayList<>();
//
//    try {
//        if (userCuis == null || userCuis.isEmpty()) {
//            logger.warn("No user CUIs provided for community detection");
//            return communities;
//        }
//
//        // בניית השאילתה עם פרמטרים בטוחים במקום החדרת טקסט
//        String query = """
//        CALL gds.graph.project.cypher(
//            'user-medical-network',
//            'MATCH (n) WHERE n.cui IN $userCuis RETURN id(n) AS id, labels(n)[0] AS type',
//            'MATCH (n)-[r]-(m) WHERE n.cui IN $userCuis AND m.cui IN $userCuis
//             RETURN id(n) AS source, id(m) AS target, coalesce(r.weight, 0.5) AS weight'
//        )
//        YIELD graphName
//
//        CALL gds.louvain.stream(graphName, {
//            relationshipWeightProperty: 'weight',
//            maxLevels: 3,
//            tolerance: 0.001
//        })
//        YIELD nodeId, communityId
//
//        WITH gds.util.asNode(nodeId) AS node, communityId
//        RETURN communityId,
//               collect({cui: node.cui, name: node.name, type: labels(node)[0]}) AS members,
//               count(*) AS size
//        ORDER BY size DESC
//        LIMIT 10
//        """;
//
//        logger.debug("Running GDS community detection with {} CUIs", userCuis.size());
//
//        // העברת הפרמטרים בבטחה
//        Map<String, Object> params = new HashMap<>();
//        params.put("userCuis", userCuis);
//
//        List<Record> records = session.readTransaction(tx -> tx.run(query, params).list());
//
//        for (Record record : records) {
//            try {
//                MedicalCommunity community = parseCommunityFromRecord(record);
//                if (community != null && community.getSize() >= 2) {
//                    communities.add(community);
//                }
//            } catch (Exception e) {
//                logger.error("Error parsing community: {}", e.getMessage());
//            }
//        }
//
//        // ניקוי הגרף לאחר השימוש
//        session.writeTransaction(tx -> {
//            tx.run("CALL gds.graph.drop('user-medical-network', false)").consume();
//            return null;
//        });
//
//    } catch (Exception e) {
//        logger.warn("GDS community detection failed: {}", e.getMessage());
//    }
//
//    return communities;
//}
//    // תיקון נוסף: בדיקה ש-detectMedicalCommunities מקבלת רשימה תקינה
//    public List<MedicalCommunity> detectMedicalCommunities(List<UserMedicalEntity> userContext) {
//        logger.info("Detecting medical communities using Louvain algorithm");
//        List<MedicalCommunity> communities = new ArrayList<>();
//
//        try(Session session = neo4jDriver.session()) {
//            // תיקון: וידוא שיש ישויות
//            if (userContext == null || userContext.isEmpty()) {
//                logger.warn("No user context provided for community detection");
//                return communities;
//            }
//
//            List<String> userCuis = userContext.stream()
//                    .map(UserMedicalEntity::getCui)
//                    .filter(cui -> cui != null && !cui.trim().isEmpty()) // תיקון: סינון CUIs null
//                    .collect(Collectors.toList());
//
//            if (userCuis.isEmpty()) {
//                logger.warn("No valid CUIs found in user context");
//                return communities;
//            }
//
//            logger.info("Attempting community detection with {} valid CUIs", userCuis.size());
//
//            // נסה GDS ראשית
//            try {
//                communities = detectCommunitiesWithGDS(session, userCuis);
//                if (!communities.isEmpty()) {
//                    logger.info("GDS community detection successful: {} communities found", communities.size());
//                    return communities;
//                }
//            } catch (Exception e) {
//                logger.warn("GDS community detection failed: {}", e.getMessage());
//            }
//
//            // fallback לגישה בסיסית
//            logger.info("Using basic community detection approach");
//            return detectCommunitiesBasic(session, userCuis);
//
//        } catch (Exception e) {
//            logger.error("Error in community detection: {}", e.getMessage());
//            return createFallbackCommunity(userContext);
//        }
//    }
//
//
//    private List<MedicalCommunity> detectCommunitiesBasic(Session session, List<String> userCuis) {
//        List<MedicalCommunity> communities = new ArrayList<>();
//
//        try {
//            String basicCommunityQuery = """
//            MATCH (n)-[r]-(m)
//            WHERE n.cui IN $userCuis AND m.cui IN $userCuis
//            WITH n, collect(DISTINCT m) as connections, count(r) as connectionCount
//            WHERE connectionCount >= 2
//            RETURN n.cui as centerCui, n.name as centerName,
//                   [conn in connections | {cui: conn.cui, name: conn.name, type: labels(conn)[0]}] as members,
//                   connectionCount as size
//            ORDER BY connectionCount DESC
//            LIMIT 5
//            """;
//
//            List<Record> records = session.readTransaction(tx ->
//                    tx.run(basicCommunityQuery, Map.of("userCuis", userCuis)).list()
//            );
//
//            long communityId = 1;
//            for (Record record : records) {
//                try {
//                    MedicalCommunity community = new MedicalCommunity();
//                    community.setCommunityId(communityId++);
//                    community.setSize(record.get("size").asInt());
//
//                    List<Object> members = record.get("members").asList();
//                    List<CommunityMember> communityMembers = members.stream()
//                            .map(member -> {
//                                @SuppressWarnings("unchecked")
//                                Map<String, Object> memberMap = (Map<String, Object>) member;
//                                CommunityMember cm = new CommunityMember();
//                                cm.setCui((String) memberMap.get("cui"));
//                                cm.setName((String) memberMap.get("name"));
//                                cm.setType((String) memberMap.get("type"));
//                                return cm;
//                            }).collect(Collectors.toList());
//
//                    community.setMembers(communityMembers);
//                    community.setCohesionScore(calculateCohesionScore(community));
//                    community.setDominantType(findDominantType(communityMembers));
//                    community.setDescription(generateCommunityDescription(community));
//
//                    communities.add(community);
//                } catch (Exception e) {
//                    logger.error("Error parsing basic community: {}", e.getMessage());
//                }
//            }
//
//        } catch (Exception e) {
//            logger.error("Error in basic community detection: {}", e.getMessage());
//        }
//
//        return communities;
//    }
//
//    /**
//     * חישוב סיכונים עם הגרף - עם תיקון פונקציות מתמטיות
//     */
//    public RiskPropagationResult calculateRiskPropagation(List<UserMedicalEntity> riskSources, Set<ExtractedSymptom> targetSymptoms, double decayFactor) {
//        logger.info("Calculating risk propagation from {} sources to {} targets",
//                riskSources.size(), targetSymptoms.size());
//        Map<String, Double> symptomRiskScores = new HashMap<>();
//        List<RiskPropagationPath> propagationPaths = new ArrayList<>();
//
//        try(Session session = neo4jDriver.session()) {
//            for(UserMedicalEntity riskSource : riskSources) {
//                for(ExtractedSymptom symptom : targetSymptoms) {
//                    // תיקון: שימוש בחישוב decay ללא pow
//                    String riskPropagationQuery = """
//                        MATCH (source {cui: $sourceCui})
//                        MATCH (target {cui: $targetCui})
//                        CALL apoc.path.expandConfig(source, {
//                            relationshipFilter: "RISK_FACTOR_FOR|CAUSES|INFLUENCES|LEADS_TO>",
//                            maxLevel: 4,
//                            limit: 20,
//                            endNodes: [target]
//                        }) YIELD path
//                        WHERE any(node in nodes(path) WHERE node.cui = $targetCui)
//
//                        WITH path,
//                             reduce(propagatedRisk = $initialRisk, rel in relationships(path) |
//                                    propagatedRisk * rel.weight *
//                                    CASE length(path)
//                                        WHEN 1 THEN $decay
//                                        WHEN 2 THEN $decay * $decay
//                                        WHEN 3 THEN $decay * $decay * $decay
//                                        WHEN 4 THEN $decay * $decay * $decay * $decay
//                                        ELSE 0.1
//                                    END) as finalRisk
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
//                    double initialRisk = calculateInitialRisk(riskSource);
//
//                    List<Record> records = session.readTransaction(tx->
//                            tx.run(riskPropagationQuery, Map.of(
//                                    "sourceCui", riskSource.getCui(),
//                                    "targetCui", symptom.getCui(),
//                                    "initialRisk", initialRisk,
//                                    "decay", decayFactor
//                            )).list());
//
//                    for (Record record : records) {
//                        try{
//                            double finalRisk = record.get("finalRisk").asDouble();
//                            symptomRiskScores.merge(symptom.getCui(), finalRisk, Double::sum);
//
//                            RiskPropagationPath path = parseRiskPropagationPath(record, riskSource, symptom);
//                            if (path != null) {
//                                propagationPaths.add(path);
//                            }
//                        }catch(Exception e){
//                            logger.error("Error parsing risk propagation path: {}",e.getMessage());
//                        }
//                    }
//                }
//            }
//        }catch(Exception e){
//            logger.error("Error in risk propagation calculation: {}", e.getMessage(), e);
//        }
//
//        RiskPropagationResult result = new RiskPropagationResult();
//        result.setSymptomRiskScores(symptomRiskScores);
//        result.setPropagationPaths(propagationPaths);
//        result.setTotalRiskScore(symptomRiskScores.values().stream().mapToDouble(Double::doubleValue).sum());
//
//        logger.info("Risk propagation complete. Total risk: {}", String.format("%.3f", result.getTotalRiskScore()));
//        return result;
//    }
//
//
//    /**
//     * ניתוח centrality למציאת "hub" רפואיים חשובים - עם תיקון parameters
//     */
//    public List<MedicalHub> findMedicalHubs(List<UserMedicalEntity> userContext) {
//        List<MedicalHub> hubs = new ArrayList<>();
//
//        try (Session session = neo4jDriver.session()) {
//            // הפכי את רשימת ה־CUIs לרשימת מחרוזות אמיתית (לא מחרוזת אחת עם גרשיים!)
//            List<String> userCuis = userContext.stream()
//                    .map(UserMedicalEntity::getCui)
//                    .collect(Collectors.toList());
//
//            // בדיקה שהרשימה לא ריקה
//            if (userCuis == null || userCuis.isEmpty()) {
//                logger.warn("No user CUIs provided for centrality analysis");
//                return hubs;
//            }
//
//            String centralityQuery = """
//            CALL gds.graph.project.cypher(
//                'centrality-network',
//                'MATCH (n) WHERE n.cui IN $userCuis RETURN id(n) AS id, n.cui AS cui, n.name AS name',
//                'MATCH (n)-[r]-(m) WHERE n.cui IN $userCuis AND m.cui IN $userCuis RETURN id(n) AS source, id(m) AS target, coalesce(r.weight, 1.0) AS weight'
//            )
//            YIELD graphName
//
//            CALL gds.betweenness.stream(graphName, {relationshipWeightProperty: 'weight'})
//            YIELD nodeId, score
//
//            WITH gds.util.asNode(nodeId) AS node, score
//            RETURN node.cui AS cui, node.name AS name, score
//            ORDER BY score DESC
//            LIMIT 10
//            """;
//
//            logger.debug("Running GDS centrality analysis with {} CUIs", userCuis.size());
//            logger.debug("User CUIs: {}", userCuis.subList(0, Math.min(5, userCuis.size())));
//
//            Map<String, Object> params = Map.of("userCuis", userCuis);
//
//            List<Record> records = session.readTransaction(tx ->
//                    tx.run(centralityQuery, params).list());
//
//            for (Record record : records) {
//                try {
//                    MedicalHub hub = new MedicalHub();
//                    hub.setCui(record.get("cui").asString());
//                    hub.setName(record.get("name").asString());
//                    hub.setCentralityScore(record.get("score").asDouble());
//                    hubs.add(hub);
//                } catch (Exception e) {
//                    logger.error("Error parsing hub: {}", e.getMessage());
//                }
//            }
//
//            // ניקוי הגרף
//            try {
//                session.writeTransaction(tx -> {
//                    tx.run("CALL gds.graph.drop('centrality-network', false)").consume();
//                    return null;
//                });
//            } catch (Exception e) {
//                logger.debug("Graph cleanup issue (might not exist): {}", e.getMessage());
//            }
//
//        } catch (Exception e) {
//            logger.error("Error in GDS centrality analysis: {}", e.getMessage());
//        }
//
//        return hubs;
//    }
//    private List<MedicalHub> findHubsBasic(Session session, List<String> userCuis) {
//        List<MedicalHub> hubs = new ArrayList<>();
//
//        try {
//            String basicHubQuery = """
//                MATCH (n)-[r]-(m)
//                WHERE n.cui IN $userCuis AND m.cui IN $userCuis
//                WITH n, count(r) as connectionCount
//                WHERE connectionCount > 1
//                RETURN n.cui as cui, n.name as name, labels(n)[0] as type, connectionCount as score
//                ORDER BY connectionCount DESC
//                LIMIT 5
//                """;
//
//            List<Record> records = session.readTransaction(tx ->
//                    tx.run(basicHubQuery, Map.of("userCuis", userCuis)).list()
//            );
//
//            for (Record record : records) {
//                MedicalHub hub = new MedicalHub();
//                hub.setCui(record.get("cui").asString());
//                hub.setName(record.get("name").asString());
//                hub.setType(record.get("type").asString());
//                hub.setCentralityScore(record.get("score").asDouble());
//                hub.setInfluenceLevel(categorizeInfluence(hub.getCentralityScore()));
//                hubs.add(hub);
//            }
//
//        } catch (Exception e) {
//            logger.error("Error in basic hub detection: {}", e.getMessage());
//        }
//
//        return hubs;
//    }
//
//    // שאר הפונקציות נשארות זהות...
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
//                        Map<String, Object> nodeMap = (Map<String, Object>) node;
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
//                        Map<String, Object> relMap = (Map<String, Object>) rel;
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
//                        Map<String, Object> memberMap = (Map<String, Object>) member;
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
//        try {
//            int pathLength = record.get("pathLength").asInt();
//
//            RiskPropagationPath path = new RiskPropagationPath();
//            path.setSourceCui(source.getCui());
//            path.setSourceName(source.getName());
//            path.setTargetCui(target.getCui());
//            path.setTargetName(target.getName());
//            path.setFinalRisk(record.get("finalRisk").asDouble());
//            path.setPathLength(pathLength);
//            path.setDecayFactor(Math.pow(0.85, pathLength));
//
//            return path;
//        }catch (Exception e) {
//            logger.error("Error parsing path: {}", e.getMessage());
//            return null;
//        }
//    }
//
//    private double calculateInitialRisk(UserMedicalEntity entity){
//        return switch (entity.getType()) {
//            case "disease" ->entity.getSeverity() !=null?
//                    switch (entity.getSeverity().toLowerCase()){
//                        case "severe"->0.9;
//                        case "moderate"->0.6;
//                        case "mild"->0.3;
//                        default -> 0.5;
//                    }:0.5;
//            case "riskfactor"->entity.getSeverity() !=null && entity.getAdditionalData().containsKey("weight")?
//                    ((Number)entity.getAdditionalData().get("weight")).doubleValue():0.4;
//            default -> 0.3;
//        };
//    }
//
//    // ביטחון יורד עם אורך המסלול אבל עולה עם רמת הסיכון
//    private double calculatePathwayConfidence(double riskScore, int pathLength) {
//        return riskScore * Math.pow(0.9, pathLength-1);
//    }
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
//    // Data classes נשארות זהות...
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
//    }
//
//    @Data
//    public static class CommunityMember {
//        private String cui;
//        private String name;
//        private String type;
//    }
//
//    @Data
//    public static class MedicalHub {
//        private String cui;
//        private String name;
//        private String type;
//        private double centralityScore;
//        private String influenceLevel;
//    }
//
//    @Data
//    public static class RiskPropagationResult {
//        private Map<String, Double> symptomRiskScores;
//        private List<RiskPropagationPath> propagationPaths;
//        private double totalRiskScore;
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
//    }
//}

package com.example.mediaid.bl.emergency;

import com.example.mediaid.dto.emergency.ExtractedSymptom;
import com.example.mediaid.dto.emergency.MedicalConnection;
import com.example.mediaid.dto.emergency.UserMedicalEntity;
import lombok.Data;
import org.neo4j.driver.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * שירות Analytics מתקדם לגרף רפואי - Graph Thinking אמיתי - מהקוד המקורי שלך
 */
@Service
public class MedicalAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(MedicalAnalysisService.class);

    @Autowired
    private Neo4jMedicalRepository repository;

    /**
     * מציאת מסלולים רפואיים עד 5 צעדים - עם תיקון פונקציות מתמטיות - מהקוד המקורי
     */
    public List<MedicalPathway> findMedicalPathways(String sourceCui, Set<ExtractedSymptom> targetSymptoms, int maxDepth) {
        logger.info("Finding advanced pathways from {} to {} symptoms (max depth: {})", sourceCui, targetSymptoms.size(), maxDepth);
        List<MedicalPathway> pathways = new ArrayList<>();

        for (ExtractedSymptom symptom : targetSymptoms) {
            List<Record> records = repository.findMedicalPathwaysQuery(sourceCui, symptom.getCui(), maxDepth);

            for (Record record : records) {
                try {
                    MedicalPathway pathway = parsePathwayFromRecord(record, sourceCui, symptom);
                    if (pathway != null && pathway.getRiskScore() > 0.1) {
                        pathways.add(pathway);
                    }
                } catch (Exception e) {
                    logger.error("Error parsing pathway: {}", e.getMessage());
                }
            }
        }

        logger.info("Found {} pathways", pathways.size());
        return pathways.stream()
                .sorted((p1, p2) -> Double.compare(p2.getRiskScore(), p1.getRiskScore()))
                .collect(Collectors.toList());
    }

    /**
     * תיקון נוסף: בדיקה ש-detectMedicalCommunities מקבלת רשימה תקינה - מהקוד המקורי
     */
    public List<MedicalCommunity> detectMedicalCommunities(List<UserMedicalEntity> userContext) {
        logger.info("Detecting medical communities using Louvain algorithm");
        List<MedicalCommunity> communities = new ArrayList<>();

        // תיקון: וידוא שיש ישויות
        if (userContext == null || userContext.isEmpty()) {
            logger.warn("No user context provided for community detection");
            return communities;
        }

        List<String> userCuis = userContext.stream()
                .map(UserMedicalEntity::getCui)
                .filter(cui -> cui != null && !cui.trim().isEmpty()) // תיקון: סינון CUIs null
                .collect(Collectors.toList());

        if (userCuis.isEmpty()) {
            logger.warn("No valid CUIs found in user context");
            return communities;
        }

        logger.info("Attempting community detection with {} valid CUIs", userCuis.size());

        // נסה GDS ראשית
        try {
            communities = detectCommunitiesWithGDS(userCuis);
            if (!communities.isEmpty()) {
                logger.info("GDS community detection successful: {} communities found", communities.size());
                return communities;
            }
        } catch (Exception e) {
            logger.warn("GDS community detection failed: {}", e.getMessage());
        }

        // fallback לגישה בסיסית
        logger.info("Using basic community detection approach");
        communities = detectCommunitiesBasic(userCuis);

        if (communities.isEmpty()) {
            return createFallbackCommunity(userContext);
        }

        return communities;
    }

    /**
     * חישוב סיכונים עם הגרף - עם תיקון פונקציות מתמטיות - מהקוד המקורי
     */
    public RiskPropagationResult calculateRiskPropagation(List<UserMedicalEntity> riskSources, Set<ExtractedSymptom> targetSymptoms, double decayFactor) {
        logger.info("Calculating risk propagation from {} sources to {} targets", riskSources.size(), targetSymptoms.size());
        Map<String, Double> symptomRiskScores = new HashMap<>();
        List<RiskPropagationPath> propagationPaths = new ArrayList<>();

        for (UserMedicalEntity riskSource : riskSources) {
            for (ExtractedSymptom symptom : targetSymptoms) {
                double initialRisk = calculateInitialRisk(riskSource);

                List<Record> records = repository.findRiskPropagationQuery(
                        riskSource.getCui(), symptom.getCui(), initialRisk, decayFactor);

                for (Record record : records) {
                    try {
                        double finalRisk = record.get("finalRisk").asDouble();
                        symptomRiskScores.merge(symptom.getCui(), finalRisk, Double::sum);

                        RiskPropagationPath path = parseRiskPropagationPath(record, riskSource, symptom);
                        if (path != null) {
                            propagationPaths.add(path);
                        }
                    } catch (Exception e) {
                        logger.error("Error parsing risk propagation path: {}", e.getMessage());
                    }
                }
            }
        }

        RiskPropagationResult result = new RiskPropagationResult();
        result.setSymptomRiskScores(symptomRiskScores);
        result.setPropagationPaths(propagationPaths);
        result.setTotalRiskScore(symptomRiskScores.values().stream().mapToDouble(Double::doubleValue).sum());

        logger.info("Risk propagation complete. Total risk: {}", String.format("%.3f", result.getTotalRiskScore()));
        return result;
    }

    /**
     * ניתוח centrality למציאת "hub" רפואיים חשובים - עם תיקון parameters - מהקוד המקורי
     */
    public List<MedicalHub> findMedicalHubs(List<UserMedicalEntity> userContext) {
        List<MedicalHub> hubs = new ArrayList<>();

        // הפכי את רשימת ה־CUIs לרשימת מחרוזות אמיתית (לא מחרוזת אחת עם גרשיים!)
        List<String> userCuis = userContext.stream()
                .map(UserMedicalEntity::getCui)
                .collect(Collectors.toList());

        // בדיקה שהרשימה לא ריקה
        if (userCuis == null || userCuis.isEmpty()) {
            logger.warn("No user CUIs provided for centrality analysis");
            return hubs;
        }

        try {
            List<Record> records = repository.findMedicalHubsQuery(userCuis);

            for (Record record : records) {
                try {
                    MedicalHub hub = new MedicalHub();
                    hub.setCui(record.get("cui").asString());
                    hub.setName(record.get("name").asString());
                    hub.setCentralityScore(record.get("score").asDouble());
                    hubs.add(hub);
                } catch (Exception e) {
                    logger.error("Error parsing hub: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error in GDS centrality analysis: {}", e.getMessage());
            // fallback לגישה בסיסית
            hubs = findHubsBasic(userCuis);
        }

        return hubs;
    }

    /**
     * קשר בין תרופות לבין סימפטומים של המשתמש - מהקוד המקורי
     */
    public List<MedicalConnection> findMedicationSideEffects(List<UserMedicalEntity> medications, List<ExtractedSymptom> symptoms) {
        List<MedicalConnection> connections = new ArrayList<>();

        for (UserMedicalEntity medication : medications) {
            for (ExtractedSymptom symptom : symptoms) {
                List<Record> records = repository.findMedicationSideEffectsQuery(medication.getCui(), symptom.getCui());

                for (Record record : records) {
                    try {
                        MedicalConnection connection = new MedicalConnection();
                        connection.setType(MedicalConnection.ConnectionType.SIDE_EFFECT);
                        connection.setFromEntity(record.get("medName").asString());
                        connection.setToEntity(record.get("sympName").asString());
                        connection.setFromCui(medication.getCui());
                        connection.setToCui(symptom.getCui());
                        connection.setConfidence(record.get("confidence").asDouble());
                        connection.setExplanation(String.format("The drug %s may cause a side effect: %s",
                                medication.getName(), symptom.getName()));
                        connections.add(connection);

                        logger.info("Found side effect connection: {} -> {}", medication.getName(), symptom.getName());
                    } catch (Exception e) {
                        logger.warn("Error processing side effect record: {}", e.getMessage());
                    }
                }
            }
        }
        return connections;
    }

    /**
     * קשר בין מחלה וסימפטומים - מהקוד המקורי
     */
    public List<MedicalConnection> findDiseaseSymptoms(List<UserMedicalEntity> diseases, List<ExtractedSymptom> symptoms) {
        List<MedicalConnection> connections = new ArrayList<>();

        for (UserMedicalEntity disease : diseases) {
            for (ExtractedSymptom symptom : symptoms) {
                List<Record> records = repository.findDiseaseSymptomsQuery(disease.getCui(), symptom.getCui());

                for (Record record : records) {
                    try {
                        MedicalConnection connection = new MedicalConnection();
                        connection.setType(MedicalConnection.ConnectionType.DISEASE_SYMPTOM);
                        connection.setFromEntity(record.get("disName").asString());
                        connection.setToEntity(record.get("sympName").asString());
                        connection.setFromCui(disease.getCui());
                        connection.setToCui(symptom.getCui());
                        connection.setConfidence(record.get("confidence").asDouble());
                        connection.setExplanation(
                                String.format("המחלה %s יכולה להיות הגורם לסימפטום: %s",
                                        disease.getName(), symptom.getName())
                        );
                        connections.add(connection);

                        logger.info("Found disease-symptom connection: {} -> {}", disease.getName(), symptom.getName());
                    } catch (Exception e) {
                        logger.warn("Error processing disease-symptom record: {}", e.getMessage());
                    }
                }
            }
        }
        return connections;
    }

    /**
     * חיפוש טיפול אפשרי לסימפטום - מהקוד המקורי
     */
    public List<MedicalConnection> findPossibleTreatments(List<ExtractedSymptom> symptoms) {
        List<MedicalConnection> connections = new ArrayList<>();

        for (ExtractedSymptom symptom : symptoms) {
            List<Record> records = repository.findPossibleTreatmentsQuery(symptom.getCui());

            // עיבוד התוצאות
            for (Record record : records) {
                try {
                    MedicalConnection connection = new MedicalConnection();
                    connection.setType(MedicalConnection.ConnectionType.DISEASE_SYMPTOM);
                    connection.setFromEntity(symptom.getName());
                    connection.setToEntity(record.get("medName").asString());
                    connection.setFromCui(symptom.getCui());
                    connection.setToCui(record.get("medCui").asString());
                    connection.setConfidence(record.get("confidence").asDouble());
                    connection.setExplanation(
                            String.format("התרופה %s עשויה לעזור בטיפול בסימפטום %s (דרך %s)",
                                    record.get("medName").asString(), symptom.getName(), record.get("disName").asString())
                    );
                    connections.add(connection);
                } catch (Exception e) {
                    logger.warn("Error processing treatment record: {}", e.getMessage());
                }
            }
        }
        return connections;
    }

    // ========== פונקציות פרטיות מהקוד המקורי ==========

    private List<MedicalCommunity> detectCommunitiesWithGDS(List<String> userCuis) {
        List<MedicalCommunity> communities = new ArrayList<>();

        List<Record> records = repository.detectCommunitiesWithGDSQuery(userCuis);

        for (Record record : records) {
            try {
                MedicalCommunity community = parseCommunityFromRecord(record);
                if (community != null && community.getSize() >= 2) {
                    communities.add(community);
                }
            } catch (Exception e) {
                logger.error("Error parsing community: {}", e.getMessage());
            }
        }

        return communities;
    }

    private List<MedicalCommunity> detectCommunitiesBasic(List<String> userCuis) {
        List<MedicalCommunity> communities = new ArrayList<>();

        List<Record> records = repository.detectCommunitiesBasicQuery(userCuis);

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

        return communities;
    }

    private List<MedicalCommunity> createFallbackCommunity(List<UserMedicalEntity> userContext) {
        List<MedicalCommunity> communities = new ArrayList<>();

        if (userContext.size() >= 2) {
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
            community.setDescription("User medical profile community (fallback)");

            communities.add(community);
        }

        return communities;
    }

    private List<MedicalHub> findHubsBasic(List<String> userCuis) {
        List<MedicalHub> hubs = new ArrayList<>();

        List<Record> records = repository.findHubsBasicQuery(userCuis);

        for (Record record : records) {
            MedicalHub hub = new MedicalHub();
            hub.setCui(record.get("cui").asString());
            hub.setName(record.get("name").asString());
            hub.setType(record.get("type").asString());
            hub.setCentralityScore(record.get("score").asDouble());
            hub.setInfluenceLevel(categorizeInfluence(hub.getCentralityScore()));
            hubs.add(hub);
        }

        return hubs;
    }

    // שאר הפונקציות נשארות זהות מהקוד המקורי...
    private MedicalPathway parsePathwayFromRecord(Record record, String sourceCui, ExtractedSymptom targetSymptom) {
        try {
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
                    .map(node -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> nodeMap = (Map<String, Object>) node;
                        PathNode pathNode = new PathNode();
                        pathNode.setCui((String) nodeMap.get("cui"));
                        pathNode.setName((String) nodeMap.get("name"));
                        pathNode.setType((String) nodeMap.get("type"));
                        return pathNode;
                    }).collect(Collectors.toList());
            pathway.setNodes(nodes);

            List<PathRelationship> relationships = pathRelationships.stream()
                    .map(rel -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> relMap = (Map<String, Object>) rel;
                        PathRelationship pathRel = new PathRelationship();
                        pathRel.setType((String) relMap.get("type"));
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
        try {
            MedicalCommunity community = new MedicalCommunity();

            community.setCommunityId(record.get("communityId").asLong());
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
        } catch (Exception e) {
            logger.error("Error parsing path: {}", e.getMessage());
            return null;
        }
    }

    private double calculateInitialRisk(UserMedicalEntity entity) {
        return switch (entity.getType()) {
            case "disease" -> entity.getSeverity() != null ?
                    switch (entity.getSeverity().toLowerCase()) {
                        case "severe" -> 0.9;
                        case "moderate" -> 0.6;
                        case "mild" -> 0.3;
                        default -> 0.5;
                    } : 0.5;
            case "riskfactor" -> entity.getSeverity() != null && entity.getAdditionalData().containsKey("weight") ?
                    ((Number) entity.getAdditionalData().get("weight")).doubleValue() : 0.4;
            default -> 0.3;
        };
    }

    // ביטחון יורד עם אורך המסלול אבל עולה עם רמת הסיכון
    private double calculatePathwayConfidence(double riskScore, int pathLength) {
        return riskScore * Math.pow(0.9, pathLength - 1);
    }

    private double calculateCohesionScore(MedicalCommunity community) {
        double baseScore = Math.min(1.0, community.getSize() / 10.0);

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

        if (pathway.getNodes().size() >= 2) {
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

    // Data classes נשארות זהות מהקוד המקורי...
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