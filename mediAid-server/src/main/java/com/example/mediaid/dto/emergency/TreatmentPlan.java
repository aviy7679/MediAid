package com.example.mediaid.dto.emergency;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentPlan {
    
    public enum UrgencyLevel {
        LOW("Low - Can wait for regular appointment"),
        MEDIUM("Medium - Should see doctor within few days"),
        HIGH("High - Should see doctor today"),
        EMERGENCY("Emergency - Seek immediate medical attention");
        
        private final String description;
        
        UrgencyLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private UrgencyLevel urgencyLevel;
    private String mainConcern; // הדאגה העיקרית
    private String reasoning; // הסבר למשתמש
    
    // פעולות מיידיות
    private List<ImmediateAction> immediateActions;
    
    // בדיקות רפואיות מומלצות
    private List<MedicalTest> recommendedTests;
    
    // ביקורים אצל רופא
    private List<DoctorVisit> doctorVisits;
    
    // הקשרים שנמצאו (לדיבוג)
    private List<MedicalConnection> foundConnections;
    
    // מידע נוסף
    private Map<String, Object> additionalInfo;
}