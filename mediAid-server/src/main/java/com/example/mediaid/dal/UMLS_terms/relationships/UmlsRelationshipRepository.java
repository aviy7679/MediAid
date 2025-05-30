package com.example.mediaid.dal.UMLS_terms.relationships;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<UmlsRelationship> findAll(Pageable pageable);

    @Query("SELECT DISTINCT ur.relationshipType FROM UmlsRelationship ur")
    List<String> findDistinctRelationshipTypes();

    @Query("SELECT DISTINCT ur.source FROM UmlsRelationship ur")
    List<String> findDistinctSources();
}