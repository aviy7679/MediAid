package com.example.mediaid.dal.UMLS_terms;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface DiseaseRepository extends JpaRepository<Disease, Long> {
    Disease findByCui(String cui);
    List<Disease> findByNameContainingIgnoreCaseOrderByNameAsc(String query, Pageable pageable);

}

