package com.example.mediaid.dal.user_medical_history;

import com.example.mediaid.dal.UMLS_terms.BiologicalFunction;
import com.example.mediaid.dal.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(
        name = "user_biological_functions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "biological_function_id", "assessment_date"})
)
@Data
public class UserBiologicalFunction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "biological_function_id", nullable = false)
    private BiologicalFunction biologicalFunction;

    @Column(name = "assessment_date")
    private LocalDate assessmentDate;

    @Column(name = "function_status") // normal, impaired, enhanced
    private String functionStatus;

    @Column(name = "measurement_value")
    private String measurementValue;

    @Column(name = "measurement_unit")
    private String measurementUnit;

    @Column(name = "notes")
    private String notes;
}