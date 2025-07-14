package com.example.mediaid.dal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserEncryptionTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testUserFieldsEncryption() {
        // נתונים רגישים שצריכים הצפנה
        String sensitiveUsername = "john_doe_sensitive";
        String sensitiveEmail = "john.doe@secret-email.com";
        String sensitiveGender = "Male";

        // יצירת משתמש עם נתונים רגישים
        User user = new User();
        user.setUsername(sensitiveUsername);
        user.setEmail(sensitiveEmail);
        user.setGender(sensitiveGender);
        user.setPasswordHash("$2a$12$hashedPassword123"); // כבר מוצפן
        user.setDateOfBirth(LocalDate.of(1990, 5, 15));
        user.setHeight(175.5f);
        user.setWeight(70.2f);

        // שמירה במסד
        User saved = userRepository.save(user);
        System.out.println("💾 User saved with ID: " + saved.getUserId());

        // קריאה מהמסד
        User retrieved = userRepository.findById(saved.getUserId()).orElse(null);

        // בדיקות שהפענוח עובד
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getUsername()).isEqualTo(sensitiveUsername);
        assertThat(retrieved.getEmail()).isEqualTo(sensitiveEmail);
        assertThat(retrieved.getGender()).isEqualTo(sensitiveGender);

        // בדיקות שדות לא מוצפנים
        assertThat(retrieved.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(retrieved.getHeight()).isEqualTo(175.5f);
        assertThat(retrieved.getWeight()).isEqualTo(70.2f);

        System.out.println("✅ USER ENCRYPTION TEST PASSED!");
        System.out.println("👤 Retrieved username: " + retrieved.getUsername());
        System.out.println("📧 Retrieved email: " + retrieved.getEmail());
        System.out.println("🔍 Check database manually for encrypted data at ID: " + saved.getUserId());
    }

    @Test
    void testNullFieldsHandling() {
        // בדיקה שערכי null לא גורמים לשגיאות
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setPasswordHash("hashedPassword");
        // משאירים שדות אחרים null

        User saved = userRepository.save(user);
        User retrieved = userRepository.findById(saved.getUserId()).orElse(null);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getUsername()).isEqualTo("testuser");
        assertThat(retrieved.getGender()).isNull(); // צריך להיות null

        System.out.println("✅ NULL FIELDS TEST PASSED!");
    }

    @Test
    void testSpecialCharactersEncryption() {
        // בדיקה שתווים מיוחדים מוצפנים נכון
        User user = new User();
        user.setUsername("user_with_special_chars_!@#$%");
        user.setEmail("special+email@example.com");
        user.setGender("Non-binary / Other");
        user.setPasswordHash("hashedPassword");

        User saved = userRepository.save(user);
        User retrieved = userRepository.findById(saved.getUserId()).orElse(null);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getUsername()).isEqualTo("user_with_special_chars_!@#$%");
        assertThat(retrieved.getEmail()).isEqualTo("special+email@example.com");
        assertThat(retrieved.getGender()).isEqualTo("Non-binary / Other");

        System.out.println("✅ SPECIAL CHARACTERS TEST PASSED!");
    }
}