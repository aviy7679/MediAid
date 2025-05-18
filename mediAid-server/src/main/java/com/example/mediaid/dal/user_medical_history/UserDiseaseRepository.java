package com.example.mediaid.dal.user_medical_history;

import com.example.mediaid.dal.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDiseaseRepository extends JpaRepository<UserDisease, Long> {
    List<UserDisease> findByUser(User user);
    List<UserDisease> findByUserAndStatus(User user, String status);

    // מציאת כל המחלות הפעילות של משתמש (ללא תאריך סיום או תאריך סיום עתידי)
    List<UserDisease> findByUserAndEndDateIsNullOrEndDateGreaterThanEqual(User user, java.time.LocalDate currentDate);
}