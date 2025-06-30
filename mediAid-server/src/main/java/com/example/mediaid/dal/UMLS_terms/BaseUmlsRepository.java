package com.example.mediaid.dal.UMLS_terms;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.util.List;

@NoRepositoryBean
public interface BaseUmlsRepository<T extends BaseUmlsEntity> extends JpaRepository<T, Long> {
    T findByCui(String cui);
    List<T> findByNameContainingIgnoreCaseOrderByNameAsc(String query, Pageable pageable);

    // חיפוש במונחי Demo (לפי CUI + שם)
    @Query("SELECT e FROM #{#entityName} e WHERE e.cui IN :demoCuis AND LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY e.name ASC")
    List<T> findDemoRelevantByNameContaining(@Param("demoCuis") List<String> demoCuis, @Param("query") String query, Pageable pageable);

    // חיפוש במונחים שאינם Demo (לפי שם, אך לא בCUI של Demo)
    @Query("SELECT e FROM #{#entityName} e WHERE e.cui NOT IN :demoCuis AND LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY e.name ASC")
    List<T> findNonDemoByNameContaining(@Param("demoCuis") List<String> demoCuis, @Param("query") String query, Pageable pageable);
}

