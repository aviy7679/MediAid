package com.example.mediaid.dal.user_medical_history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// UserRiskFactorRepository.java
@Repository
public interface UserRiskFactorRepository extends JpaRepository<UserRiskFactor, Long> {

    List<UserRiskFactor> findByUser_UserId(UUID userId);

    List<UserRiskFactor> findByRiskFactor_Id(Long riskFactorId);

    Optional<UserRiskFactor> findByUser_UserIdAndRiskFactor_Id(UUID userId, Long riskFactorId);

    List<UserRiskFactor> findByUser_UserIdAndIsActive(UUID userId, Boolean isActive);

    List<UserRiskFactor> findByUser_UserIdAndRiskLevel(UUID userId, String riskLevel);

    List<UserRiskFactor> findByUser_UserIdAndIdentifiedDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT urf FROM UserRiskFactor urf WHERE urf.user.userId = :userId AND urf.isActive = true")
    List<UserRiskFactor> findActiveRiskFactorsForUser(@Param("userId") UUID userId);

    @Query("SELECT urf FROM UserRiskFactor urf WHERE urf.user.userId = :userId AND urf.riskLevel = 'high' AND urf.isActive = true")
    List<UserRiskFactor> findHighRiskFactorsForUser(@Param("userId") UUID userId);

    @Query("SELECT urf FROM UserRiskFactor urf WHERE urf.user.userId = :userId AND urf.riskLevel IN ('high', 'critical') AND urf.isActive = true")
    List<UserRiskFactor> findCriticalRiskFactorsForUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(urf) FROM UserRiskFactor urf WHERE urf.user.userId = :userId AND urf.isActive = true")
    Long countActiveRiskFactorsForUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(urf) FROM UserRiskFactor urf WHERE urf.user.userId = :userId AND urf.riskLevel = 'high' AND urf.isActive = true")
    Long countHighRiskFactorsForUser(@Param("userId") UUID userId);
}
