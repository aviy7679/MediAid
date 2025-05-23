package com.example.mediaid.dal.user_medical_history;

import com.example.mediaid.dal.UMLS_terms.AnatomicalStructure;
import com.example.mediaid.dal.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(
        name = "user_anatomical_structures",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "anatomical_structure_id", "condition_date"})
)
@Data
public class UserAnatomicalStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "anatomical_structure_id", nullable = false)
    private AnatomicalStructure anatomicalStructure;

    @Column(name = "condition_date")
    private LocalDate conditionDate;

    @Column(name = "condition_type") // abnormality, injury, surgery, etc.
    private String conditionType;

    @Column(name = "severity")
    private String severity;

    @Column(name = "status") // active, resolved, chronic
    private String status;

    @Column(name = "notes")
    private String notes;
}
