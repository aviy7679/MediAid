package com.example.mediaid.dal.user_medical_history;

import com.example.mediaid.dal.UMLS_terms.RiskFactor;
import com.example.mediaid.dal.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(
        name = "user_risk_factors",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "risk_factor_id"})
)
@Data
public class UserRiskFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "risk_factor_id", nullable = false)
    private RiskFactor riskFactor;

    @Column(name = "identified_date")
    private LocalDate identifiedDate;

    @Column(name = "risk_level") // low, moderate, high
    private String riskLevel;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "notes")
    private String notes;
}
