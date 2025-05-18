package com.example.mediaid.dal.user_medical_history;

import com.example.mediaid.dal.UMLS_terms.Disease;
import com.example.mediaid.dal.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(
        name = "user_diseases",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "disease_id"})
)
@Data
public class UserDisease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "disease_id", nullable = false)
    private Disease disease;

    @Column(name = "diagnosis_date")
    private LocalDate diagnosisDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status")
    private String status;

    @Column(name = "severity")
    private String severity;

    @Column(name = "notes")
    private String notes;
}
