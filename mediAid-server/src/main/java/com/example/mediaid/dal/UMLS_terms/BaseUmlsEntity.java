package com.example.mediaid.dal.UMLS_terms;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@MappedSuperclass //מחלקת אב ללא מימוש במסד
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseUmlsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cui", length = 8, nullable = false, unique = true)
    private String cui;

    @Column(name = "name", nullable = false)
    private String name;

    // לוגיקה משותפת לכל ישויות UMLS אם נדרש
}