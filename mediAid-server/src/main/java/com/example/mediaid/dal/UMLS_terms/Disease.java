package com.example.mediaid.dal.UMLS_terms;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "diseases")
@Data
public class Disease {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disease_id")
    private Long id;

    @Column(name = "cui", length = 8, nullable = false, unique = true)
    private String cui;

    @Column(name = "name", nullable = false)
    private String name;


}