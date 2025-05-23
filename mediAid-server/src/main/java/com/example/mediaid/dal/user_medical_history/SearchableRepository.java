package com.example.mediaid.dal.user_medical_history;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SearchableRepository<T> {

    @Query("SELECT e FROM #{#entityName} e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<T> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT e FROM #{#entityName} e WHERE e.cui = :cui")
    List<T> findByCui(@Param("cui") String cui);
}
