package com.example.mediaid.dto.emergency;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorVisit {
    
    public enum DoctorType {
        EMERGENCY_ROOM("Emergency Room"),
        FAMILY_DOCTOR("Family Doctor"),
        CARDIOLOGIST("Cardiologist"),
        DERMATOLOGIST("Dermatologist"),
        NEUROLOGIST("Neurologist"),
        GASTROENTEROLOGIST("Gastroenterologist"),
        PULMONOLOGIST("Pulmonologist"),
        ORTHOPEDIST("Orthopedist"),
        OPHTHALMOLOGIST("Ophthalmologist"),
        ENT("ENT Specialist"),
        PSYCHIATRIST("Psychiatrist"),
        ENDOCRINOLOGIST("Endocrinologist");
        
        private final String description;
        
        DoctorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private DoctorType type;
    private String reason;
    private String urgency; // "Immediately", "Same day", "Within week"
    private String preparation; // הכנה לביקור
}