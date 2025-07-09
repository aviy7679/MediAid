package com.example.mediaid.dal.UMLS_terms;

import com.example.mediaid.constants.ApiConstants;
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

    @Column(name = "cui", length = ApiConstants.CUI_LENGTH, nullable = false, unique = true)
    private String cui;

    @Column(name = "name", length = ApiConstants.MAX_ENTITY_NAME_LENGTH, nullable = false)
    private String name;
}