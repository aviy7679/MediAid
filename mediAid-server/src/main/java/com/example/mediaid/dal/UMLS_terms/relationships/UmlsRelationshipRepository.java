package com.example.mediaid.dal.UMLS_terms.relationships;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UmlsRelationshipRepository extends JpaRepository<UmlsRelationship, Long> {
    List<UmlsRelationship> findByCui1AndCui2(String cui1, String cui2);
    List<UmlsRelationship> findByRelationshipType(String relationshipType);
    List<UmlsRelationship> findBySource(String source);

    @Query("SELECT COUNT(ur) FROM UmlsRelationship ur")
    long countAllRelationships();
}