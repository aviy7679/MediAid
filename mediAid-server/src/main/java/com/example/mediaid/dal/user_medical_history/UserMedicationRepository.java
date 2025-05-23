package com.example.mediaid.dal.user_medical_history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// UserMedicationRepository.java
@Repository
public interface UserMedicationRepository extends JpaRepository<UserMedication, Long> {

    List<UserMedication> findByUser_UserId(UUID userId);

    List<UserMedication> findByMedication_Id(Long medicationId);

    Optional<UserMedication> findByUser_UserIdAndMedication_IdAndStartDate(UUID userId, Long medicationId, LocalDate startDate);

    List<UserMedication> findByUser_UserIdAndIsActive(UUID userId, Boolean isActive);

    List<UserMedication> findByUser_UserIdAndPrescribingDoctor(UUID userId, String doctor);

    List<UserMedication> findByUser_UserIdAndStartDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT um FROM UserMedication um WHERE um.user.userId = :userId AND um.isActive = true")
    List<UserMedication> findActiveMedicationsForUser(@Param("userId") UUID userId);

    @Query("SELECT um FROM UserMedication um WHERE um.user.userId = :userId AND um.endDate IS NULL")
    List<UserMedication> findCurrentMedicationsForUser(@Param("userId") UUID userId);

    @Query("SELECT um FROM UserMedication um WHERE um.user.userId = :userId AND um.endDate < CURRENT_DATE")
    List<UserMedication> findExpiredMedicationsForUser(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT um.prescribingDoctor FROM UserMedication um WHERE um.user.userId = :userId")
    List<String> findDoctorsForUser(@Param("userId") UUID userId);
}
