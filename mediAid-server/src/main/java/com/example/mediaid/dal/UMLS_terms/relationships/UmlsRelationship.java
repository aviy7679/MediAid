package com.example.mediaid.dal.UMLS_terms.relationships;

import jakarta.persistence.*;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "umls_relationships")
@Data
@NoArgsConstructor
public class UmlsRelationship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cui1", length = 8, nullable = false)
    private String cui1;

    @Column(name = "cui2", length = 8, nullable = false)
    private String cui2;

    @Column(name = "relationship_type", length = 50, nullable = false)
    private String relationshipType;

    @Column(name = "weight", nullable = false)
    private Double weight;

    @Column(name = "source", length = 20)
    private String source;

    @Column(name = "original_rel", length = 50)
    private String originalRel;

    @Column(name = "original_rela", length = 50)
    private String originalRela;
}