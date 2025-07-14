package com.example.mediaid.dal.user_medical_history;

import com.example.mediaid.dal.UMLS_terms.Disease;
import com.example.mediaid.dal.User;
import com.example.mediaid.security.encryption.EncryptedStringAttributeConverter;
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

    @Convert(converter = EncryptedStringAttributeConverter.class)
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(converter = EncryptedStringAttributeConverter.class)
    @ManyToOne
    @JoinColumn(name = "disease_id", nullable = false)
    private Disease disease;


    @Column(name = "diagnosis_date")
    private LocalDate diagnosisDate;  //תאריך אבחון

    @Column(name = "end_date")
    private LocalDate endDate;   //תאריך החלמה

    @Column(name = "status")
    private String status;   //סטטוס

    @Column(name = "severity")
    private String severity;   //רמת חומרה

    @Column(name = "notes")
    private String notes;   //הערות
}
