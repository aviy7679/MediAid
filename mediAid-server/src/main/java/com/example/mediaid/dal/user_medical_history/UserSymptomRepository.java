package com.example.mediaid.dal.user_medical_history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// UserSymptomRepository.java
@Repository
public interface UserSymptomRepository extends JpaRepository<UserSymptom, Long> {

    List<UserSymptom> findByUser_UserId(UUID userId);

    List<UserSymptom> findBySymptom_Id(Long symptomId);

    Optional<UserSymptom> findByUser_UserIdAndSymptom_IdAndStartDate(UUID userId, Long symptomId, LocalDate startDate);

    List<UserSymptom> findByUser_UserIdAndIsActive(UUID userId, Boolean isActive);

    List<UserSymptom> findByUser_UserIdAndSeverity(UUID userId, String severity);

    List<UserSymptom> findByUser_UserIdAndStartDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT us FROM UserSymptom us WHERE us.user.userId = :userId AND us.isActive = true")
    List<UserSymptom> findActiveSymptomsForUser(@Param("userId") UUID userId);

    @Query("SELECT us FROM UserSymptom us WHERE us.user.userId = :userId AND us.endDate IS NULL")
    List<UserSymptom> findOngoingSymptomsForUser(@Param("userId") UUID userId);

    @Query("SELECT us FROM UserSymptom us WHERE us.user.userId = :userId AND us.severity IN ('severe', 'critical')")
    List<UserSymptom> findSevereSymptomsForUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(us) FROM UserSymptom us WHERE us.user.userId = :userId AND us.isActive = true")
    Long countActiveSymptomsForUser(@Param("userId") UUID userId);
}
