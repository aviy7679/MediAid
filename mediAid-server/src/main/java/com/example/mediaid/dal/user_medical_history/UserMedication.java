package com.example.mediaid.dal.user_medical_history;

import com.example.mediaid.dal.UMLS_terms.Medication;
import com.example.mediaid.dal.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(
        name = "user_medications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "medication_id"})
)
@Data
public class UserMedication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "dosage")
    private String dosage;

    @Column(name = "frequency")
    private String frequency;

    @Column(name = "administration_route")
    private String administrationRoute;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "notes")
    private String notes;
}