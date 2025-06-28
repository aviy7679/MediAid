package com.example.mediaid.dal.user_medical_history;

import com.example.mediaid.dal.UMLS_terms.LabTest;
import com.example.mediaid.dal.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(
        name = "user_lab_tests",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lab_test_id", "test_date"})
)
@Data
public class UserLabTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "lab_test_id", nullable = false)
    private LabTest labTest;

    @Column(name = "test_date")
    private LocalDate testDate;

    @Column(name = "result_value")
    private String resultValue;

    @Column(name = "result_unit")
    private String resultUnit;

    @Column(name = "reference_range")
    private String referenceRange;

    @Column(name = "ordering_doctor")
    private String orderingDoctor;

    @Column(name = "lab_facility")
    private String labFacility;

    @Column(name = "is_abnormal")
    private Boolean isAbnormal;

    @Column(name = "notes")
    private String notes;
}
