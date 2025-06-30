package com.example.mediaid.dto.emergency;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImmediateAction {
    
    public enum ActionType {
        STOP_MEDICATION("Stop taking medication"),
        TAKE_MEDICATION("Take medication"),
        CALL_EMERGENCY("Call emergency services"),
        MONITOR_SYMPTOMS("Monitor symptoms"),
        APPLY_FIRST_AID("Apply first aid"),
        REST("Rest and avoid activity"),
        SEEK_IMMEDIATE_CARE("Seek immediate medical care");
        
        private final String description;
        
        ActionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private ActionType type;
    private String description;
    private String reason;
    private Integer priority; // 1 = הכי חשוב, 5 = פחות חשוב
    private String medicationName; // אם רלוונטי
    private String dosage; // אם רלוונטי
}