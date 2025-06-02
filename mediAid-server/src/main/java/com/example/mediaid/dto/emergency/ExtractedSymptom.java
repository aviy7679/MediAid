package com.example.mediaid.dto.emergency;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedSymptom {
    private String cui;
    private String name;
    private String detectedName;
    private Double confidence;
    private Double probability;
    private String source; // "text" או "image"
    private Integer startPosition; // רק לטקסט
    private Integer endPosition; // רק לטקסט
    private String status; // Present, Absent, Unknown
    private String analyzerType; // "MedCAT" או "BiomedCLIP"
}