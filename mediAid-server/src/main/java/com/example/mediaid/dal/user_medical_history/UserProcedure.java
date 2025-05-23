package com.example.mediaid.dal.user_medical_history;

import com.example.mediaid.dal.UMLS_terms.Procedure;
import com.example.mediaid.dal.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(
        name = "user_procedures",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "procedure_id", "procedure_date"})
)
@Data
public class UserProcedure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "procedure_id", nullable = false)
    private Procedure procedure;

    @Column(name = "procedure_date")
    private LocalDate procedureDate;

    @Column(name = "performing_doctor")
    private String performingDoctor;

    @Column(name = "hospital_clinic")
    private String hospitalClinic;

    @Column(name = "outcome") // successful, complications, etc.
    private String outcome;

    @Column(name = "notes")
    private String notes;
}
