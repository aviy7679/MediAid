package com.example.mediaid.dal.user_medical_history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// UserAnatomicalStructureRepository.java
@Repository
public interface UserAnatomicalStructureRepository extends JpaRepository<UserAnatomicalStructure, Long> {

    List<UserAnatomicalStructure> findByUser_UserId(UUID userId);

    List<UserAnatomicalStructure> findByAnatomicalStructure_Id(Long anatomicalStructureId);

    Optional<UserAnatomicalStructure> findByUser_UserIdAndAnatomicalStructure_IdAndConditionDate(UUID userId, Long anatomicalStructureId, LocalDate conditionDate);

    List<UserAnatomicalStructure> findByUser_UserIdAndConditionType(UUID userId, String conditionType);

    List<UserAnatomicalStructure> findByUser_UserIdAndStatus(UUID userId, String status);

    List<UserAnatomicalStructure> findByUser_UserIdAndSeverity(UUID userId, String severity);

    List<UserAnatomicalStructure> findByUser_UserIdAndConditionDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT uas FROM UserAnatomicalStructure uas WHERE uas.user.userId = :userId AND uas.status = 'active' ORDER BY uas.conditionDate DESC")
    List<UserAnatomicalStructure> findActiveAnatomicalConditionsForUser(@Param("userId") UUID userId);

    @Query("SELECT uas FROM UserAnatomicalStructure uas WHERE uas.user.userId = :userId AND uas.conditionType = :conditionType ORDER BY uas.conditionDate DESC")
    List<UserAnatomicalStructure> findAnatomicalConditionsByType(@Param("userId") UUID userId, @Param("conditionType") String conditionType);

    @Query("SELECT uas FROM UserAnatomicalStructure uas WHERE uas.user.userId = :userId AND uas.severity IN ('severe', 'critical')")
    List<UserAnatomicalStructure> findSevereAnatomicalConditions(@Param("userId") UUID userId);

    @Query("SELECT COUNT(uas) FROM UserAnatomicalStructure uas WHERE uas.user.userId = :userId AND uas.status = 'active'")
    Long countActiveAnatomicalConditionsForUser(@Param("userId") UUID userId);
}
