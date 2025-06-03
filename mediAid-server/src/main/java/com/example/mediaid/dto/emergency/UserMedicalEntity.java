package com.example.mediaid.dto.emergency;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMedicalEntity {
    private String cui;
    private String name;
    private String type; // "medication", "disease", "risk_factor", etc.
    private String status; // "active", "inactive", "resolved"
    private LocalDate startDate;
    private LocalDate endDate;
    private String dosage; // למדיקציות
    private String frequency; // למדיקציות
    private String severity; // למחלות
    private Map<String, Object> additionalData;
}