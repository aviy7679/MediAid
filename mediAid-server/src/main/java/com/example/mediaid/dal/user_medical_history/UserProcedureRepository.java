package com.example.mediaid.dal.user_medical_history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// UserProcedureRepository.java
@Repository
public interface UserProcedureRepository extends JpaRepository<UserProcedure, Long> {

    List<UserProcedure> findByUser_UserId(UUID userId);

    List<UserProcedure> findByProcedure_Id(Long procedureId);

    Optional<UserProcedure> findByUser_UserIdAndProcedure_IdAndProcedureDate(UUID userId, Long procedureId, LocalDate procedureDate);

    List<UserProcedure> findByUser_UserIdAndPerformingDoctor(UUID userId, String doctor);

    List<UserProcedure> findByUser_UserIdAndHospitalClinic(UUID userId, String hospitalClinic);

    List<UserProcedure> findByUser_UserIdAndProcedureDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    List<UserProcedure> findByUser_UserIdAndOutcome(UUID userId, String outcome);

    @Query("SELECT up FROM UserProcedure up WHERE up.user.userId = :userId ORDER BY up.procedureDate DESC")
    List<UserProcedure> findProceduresForUserOrderedByDate(@Param("userId") UUID userId);

    @Query("SELECT up FROM UserProcedure up WHERE up.user.userId = :userId AND up.procedureDate >= :date")
    List<UserProcedure> findRecentProceduresForUser(@Param("userId") UUID userId, @Param("date") LocalDate date);

    @Query("SELECT DISTINCT up.performingDoctor FROM UserProcedure up WHERE up.user.userId = :userId")
    List<String> findDoctorsForUser(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT up.hospitalClinic FROM UserProcedure up WHERE up.user.userId = :userId")
    List<String> findHospitalsForUser(@Param("userId") UUID userId);
}
