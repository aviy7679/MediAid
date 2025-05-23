package com.example.mediaid.dal.UMLS_terms;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.util.List;

@NoRepositoryBean
public interface BaseUmlsRepository<T extends BaseUmlsEntity> extends JpaRepository<T, Long> {
    T findByCui(String cui);
    List<T> findByNameContainingIgnoreCaseOrderByNameAsc(String query, Pageable pageable);
}

