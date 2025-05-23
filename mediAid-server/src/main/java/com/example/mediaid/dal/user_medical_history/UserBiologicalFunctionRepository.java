package com.example.mediaid.dal.user_medical_history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// UserBiologicalFunctionRepository.java
@Repository
public interface UserBiologicalFunctionRepository extends JpaRepository<UserBiologicalFunction, Long> {

    List<UserBiologicalFunction> findByUser_UserId(UUID userId);

    List<UserBiologicalFunction> findByBiologicalFunction_Id(Long biologicalFunctionId);

    Optional<UserBiologicalFunction> findByUser_UserIdAndBiologicalFunction_IdAndAssessmentDate(UUID userId, Long biologicalFunctionId, LocalDate assessmentDate);

    List<UserBiologicalFunction> findByUser_UserIdAndFunctionStatus(UUID userId, String functionStatus);

    List<UserBiologicalFunction> findByUser_UserIdAndAssessmentDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT ubf FROM UserBiologicalFunction ubf WHERE ubf.user.userId = :userId ORDER BY ubf.assessmentDate DESC")
    List<UserBiologicalFunction> findBiologicalFunctionsForUserOrderedByDate(@Param("userId") UUID userId);

    @Query("SELECT ubf FROM UserBiologicalFunction ubf WHERE ubf.user.userId = :userId AND ubf.biologicalFunction.id = :functionId ORDER BY ubf.assessmentDate DESC")
    List<UserBiologicalFunction> findFunctionHistoryForUser(@Param("userId") UUID userId, @Param("functionId") Long functionId);

    @Query("SELECT ubf FROM UserBiologicalFunction ubf WHERE ubf.user.userId = :userId AND ubf.functionStatus = 'impaired' ORDER BY ubf.assessmentDate DESC")
    List<UserBiologicalFunction> findImpairedFunctionsForUser(@Param("userId") UUID userId);

    @Query("SELECT ubf FROM UserBiologicalFunction ubf WHERE ubf.user.userId = :userId AND ubf.functionStatus = 'normal' ORDER BY ubf.assessmentDate DESC")
    List<UserBiologicalFunction> findNormalFunctionsForUser(@Param("userId") UUID userId);

    @Query("SELECT ubf FROM UserBiologicalFunction ubf WHERE ubf.user.userId = :userId AND ubf.assessmentDate >= :date")
    List<UserBiologicalFunction> findRecentAssessmentsForUser(@Param("userId") UUID userId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(ubf) FROM UserBiologicalFunction ubf WHERE ubf.user.userId = :userId AND ubf.functionStatus = 'impaired'")
    Long countImpairedFunctionsForUser(@Param("userId") UUID userId);
}
