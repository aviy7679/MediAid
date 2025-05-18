package com.example.mediaid.dal.UMLS_terms;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiseaseRepository extends JpaRepository<Disease, Long> {
    // ניתן להוסיף כאן פונקציות חיפוש נוספות
    Disease findByCui(String cui);

    // חיפוש לפי שם (חלקי) המחלה
    List<Disease> findByNameContainingIgnoreCase(String name);
}

