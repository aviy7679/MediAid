package com.example.mediaid.dal.user_medical_history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// UserLabTestRepository.java
@Repository
public interface UserLabTestRepository extends JpaRepository<UserLabTest, Long> {

    List<UserLabTest> findByUser_UserId(UUID userId);

    List<UserLabTest> findByLabTest_Id(Long labTestId);

    Optional<UserLabTest> findByUser_UserIdAndLabTest_IdAndTestDate(UUID userId, Long labTestId, LocalDate testDate);

    List<UserLabTest> findByUser_UserIdAndIsAbnormal(UUID userId, Boolean isAbnormal);

    List<UserLabTest> findByUser_UserIdAndOrderingDoctor(UUID userId, String doctor);

    List<UserLabTest> findByUser_UserIdAndTestDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    List<UserLabTest> findByUser_UserIdAndLabFacility(UUID userId, String labFacility);

    @Query("SELECT ult FROM UserLabTest ult WHERE ult.user.userId = :userId ORDER BY ult.testDate DESC")
    List<UserLabTest> findLabTestsForUserOrderedByDate(@Param("userId") UUID userId);

    @Query("SELECT ult FROM UserLabTest ult WHERE ult.user.userId = :userId AND ult.labTest.id = :labTestId ORDER BY ult.testDate DESC")
    List<UserLabTest> findLabTestHistoryForUser(@Param("userId") UUID userId, @Param("labTestId") Long labTestId);

    @Query("SELECT ult FROM UserLabTest ult WHERE ult.user.userId = :userId AND ult.isAbnormal = true ORDER BY ult.testDate DESC")
    List<UserLabTest> findAbnormalLabTestsForUser(@Param("userId") UUID userId);

    @Query("SELECT ult FROM UserLabTest ult WHERE ult.user.userId = :userId AND ult.testDate >= :date")
    List<UserLabTest> findRecentLabTestsForUser(@Param("userId") UUID userId, @Param("date") LocalDate date);

    @Query("SELECT DISTINCT ult.orderingDoctor FROM UserLabTest ult WHERE ult.user.userId = :userId")
    List<String> findOrderingDoctorsForUser(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT ult.labFacility FROM UserLabTest ult WHERE ult.user.userId = :userId")
    List<String> findLabFacilitiesForUser(@Param("userId") UUID userId);
}
