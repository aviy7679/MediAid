package com.example.mediaid.dal.UMLS_terms;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {
    // ניתן להוסיף כאן פונקציות חיפוש נוספות
    Medication findByCui(String cui);

    // חיפוש לפי שם (חלקי) התרופה
    List<Medication> findByNameContainingIgnoreCase(String name);
}