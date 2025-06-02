package com.example.mediaid.dto.emergency;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalTest {
    
    public enum TestType {
        BLOOD_TEST("Blood Test"),
        ECG("Electrocardiogram"),
        XRAY("X-Ray"),
        ULTRASOUND("Ultrasound"),
        CT_SCAN("CT Scan"),
        MRI("MRI"),
        BLOOD_PRESSURE("Blood Pressure Check"),
        BLOOD_SUGAR("Blood Sugar Test"),
        URINE_TEST("Urine Test"),
        ALLERGY_TEST("Allergy Test");
        
        private final String description;
        
        TestType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private TestType type;
    private String description;
    private String reason;
    private String urgency; // "ASAP", "Within 24h", "Within week"
    private List<String> specificTests; // בדיקות ספציפיות כמו "CBC", "Troponin"
}