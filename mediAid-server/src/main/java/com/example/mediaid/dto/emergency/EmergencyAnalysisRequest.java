package com.example.mediaid.dto.emergency;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyAnalysisRequest{
    private String text;
    private String description;   //תיאור נוסף
    private Double minConfidence; //ביטחון מינימלי
}
