//package com.example.mediaid.dal;
//
//import jakarta.persistence.*;
//@Entity
//public class Users {
//    @Id
//    @GeneratedValue
//    private Long id;
//    private String firstName;
//    private String lastName;
//    @Column(unique=true)
//    private String email;
//    @Column(length = 5)
//    private String password;
//    public Users() {}
//
//    public Users(String firstName, String lastName, String email) {
//        this.firstName = firstName;
//        this.lastName = lastName;
//        this.email = email;
//    }
//    public Long getId() {
//        return id;
//    }
//    public void setId(Long id) {
//        this.id = id;
//    }
//    public String getFirstName() {
//        return firstName;
//    }
//    public void setFirstName(String firstName) {
//        this.firstName = firstName;
//    }
//    public String getLastName() {
//        return lastName;
//    }
//    public void setLastName(String lastName) {
//        this.lastName = lastName;
//    }
//    public String getEmail() {
//        return email;
//    }
//    public void setEmail(String email) {
//        this.email = email;
//    }
//    public String getPassword() {
//        return password;
//    }
//    public void setPassword(String password) {
//        this.password = password;
//    }
//    public String getFullName() {
//        return firstName + " " + lastName;
//    }
//
//
//
//}

package com.example.mediaid.dal;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long userId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String gender;

    private Float height;

    private Float weight;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

//    // Relationships (Uncomment if needed)
//    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
// private List<MedicalCondition> medicalConditions;
//
// @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
// private List<Medication> medications;
//
// @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
// private List<Allergy> allergies;
//
// @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
// private List<Hospitalization> hospitalizations;
//
//    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
//    private List<SymptomReport> symptomReports;
}