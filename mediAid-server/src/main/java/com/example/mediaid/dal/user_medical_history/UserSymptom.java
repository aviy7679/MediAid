package com.example.mediaid.dal.user_medical_history;

import com.example.mediaid.dal.UMLS_terms.Symptom;
import com.example.mediaid.dal.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(
        name = "user_symptoms",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "symptom_id", "start_date"})
)
@Data
public class UserSymptom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "symptom_id", nullable = false)
    private Symptom symptom;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "severity") // רמת חומרה
    private String severity;

    @Column(name = "frequency") // תדירות הופעה
    private String frequency;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "notes")
    private String notes;
}
