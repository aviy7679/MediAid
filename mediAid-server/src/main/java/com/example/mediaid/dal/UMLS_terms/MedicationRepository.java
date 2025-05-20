package com.example.mediaid.dal.UMLS_terms;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {
    Medication findByCui(String cui);
    List<Medication> findByNameContainingIgnoreCaseOrderByNameAsc(String query, Pageable pageable);

}