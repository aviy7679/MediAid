package com.example.mediaid.dal.user_medical_history;

import com.example.mediaid.dal.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserMedicationRepository extends JpaRepository<UserMedication, Long> {
    List<UserMedication> findByUser(User user);
    List<UserMedication> findByUserAndStatus(User user, String status);

    // מציאת כל התרופות הפעילות של משתמש (ללא תאריך סיום או תאריך סיום עתידי)
    List<UserMedication> findByUserAndEndDateIsNullOrEndDateGreaterThanEqual(User user, java.time.LocalDate currentDate);
}