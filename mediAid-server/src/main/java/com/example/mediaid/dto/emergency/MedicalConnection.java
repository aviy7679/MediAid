package com.example.mediaid.dto.emergency;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalConnection {
    
    public enum ConnectionType {
        SIDE_EFFECT("Side Effect"),
        DISEASE_SYMPTOM("Disease Symptom"),
        MEDICATION_INTERACTION("Medication Interaction"),
        COMPLICATION("Complication"),
        RISK_FACTOR("Risk Factor"),
        CONTRAINDICATION("Contraindication");
        
        private final String description;
        
        ConnectionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private ConnectionType type;
    private String fromEntity; // ממה (תרופה, מחלה, וכו')
    private String toEntity; // אל מה (סימפטום, מחלה, וכו')
    private String fromCui;
    private String toCui;
    private Double confidence;  //אמינות
    private String explanation;
}
