package com.example.mediaid.dto;

import org.springframework.web.multipart.MultipartFile;

public class DiagnosisData {
    private String text;
    private MultipartFile image;
    private MultipartFile audio;

    public DiagnosisData() {}

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }

    public MultipartFile getAudio() {
        return audio;
    }

    public void setAudio(MultipartFile audio) {
        this.audio = audio;
    }

    public String analyzeImage() {
//        if (image != null && !image.isEmpty()) {
//            Text_from_image analysis = new Text_from_image(image);
//            return analysis.processOCR();
//        }
        return "No image provided";
    }
}
