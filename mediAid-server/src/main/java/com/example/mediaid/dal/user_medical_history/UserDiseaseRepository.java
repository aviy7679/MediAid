package com.example.mediaid.dal.user_medical_history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDiseaseRepository extends JpaRepository<UserDisease, Long> {

    List<UserDisease> findByUser_UserId(UUID userId);

    List<UserDisease> findByDisease_Id(Long diseaseId);

    Optional<UserDisease> findByUser_UserIdAndDisease_Id(UUID userId, Long diseaseId);

    List<UserDisease> findByUser_UserIdAndStatus(UUID userId, String status);

    List<UserDisease> findByUser_UserIdAndDiagnosisDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT ud FROM UserDisease ud WHERE ud.user.userId = :userId AND ud.endDate IS NULL")
    List<UserDisease> findActiveDiseasesForUser(@Param("userId") UUID userId);

    @Query("SELECT ud FROM UserDisease ud WHERE ud.user.userId = :userId AND ud.severity = :severity")
    List<UserDisease> findDiseasesBySeverity(@Param("userId") UUID userId, @Param("severity") String severity);

    @Query("SELECT COUNT(ud) FROM UserDisease ud WHERE ud.user.userId = :userId AND ud.endDate IS NULL")
    Long countActiveDiseasesForUser(@Param("userId") UUID userId);
}

// ================================================================
// Additional Utility Repositories
// ================================================================

// Generic Search Repository Interface
package com.example.mediaid.dal.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

