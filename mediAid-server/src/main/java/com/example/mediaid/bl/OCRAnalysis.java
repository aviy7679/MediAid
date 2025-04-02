//
//package com.example.mediaid.bl;
//
//import com.aspose.ocr.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.util.ArrayList;
//
//public class OCRAnalysis {
//    private AsposeOCR recognize;
//    private MultipartFile image;
//
//    public OCRAnalysis(MultipartFile file) {
//        this.image = file;
//    }
//
//    public String processOCR() {
//        recognize = new AsposeOCR();
//        OcrInput imageData = new OcrInput(InputType.SingleImage);
//
//        try {
//            imageData.add(image.getInputStream());
//        } catch (IOException e) {
//            return "OCR Error: Unable to read image input";
//        }
//
//        ArrayList<RecognitionResult> results;
//        try {
//            results = recognize.Recognize(imageData);
//        } catch (AsposeOCRException e) {
//            return "OCR Error: Recognition failed";
//        }
//
//        if (results == null || results.isEmpty()) {
//            return "OCR Error: No text recognized";
//        }
//
//        // איסוף כל תוצאות ה-OCR למחרוזת אחת
//        StringBuilder fullText = new StringBuilder();
//        for (RecognitionResult result : results) {
//            fullText.append(result.recognitionText).append("\n");
//        }
//
//        return fullText.toString().trim();
//    }
//}
package com.example.mediaid.bl;

import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OCRAnalysis {
    private MultipartFile image;

    public OCRAnalysis(MultipartFile file) {
        this.image = file;
    }

    public String processOCR() {
        try {
            // יצירת קובץ זמני מהתמונה שהועלתה
            Path tempDir = Files.createTempDirectory("ocr-temp");
            Path tempImage = tempDir.resolve("image" + getFileExtension());
            image.transferTo(tempImage.toFile());

            System.out.println("תמונה זמנית נוצרה ב: " + tempImage.toAbsolutePath());

            // נתיב לקובץ פלט טקסט
            Path outputTextFile = tempDir.resolve("output");

            // בניית פקודה להרצת Tesseract
            ProcessBuilder processBuilder;

            // בדיקה איזו מערכת הפעלה
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

            if (isWindows) {
                // בחלונות - צריך לציין את הנתיב המלא של Tesseract
                // שנה את זה לנתיב הנכון במחשב שלך אם צריך
                String tesseractPath = "C:\\Program Files\\Tesseract-OCR\\tesseract.exe";

                processBuilder = new ProcessBuilder(
                        tesseractPath,
                        tempImage.toString(),
                        outputTextFile.toString(),
                        "-l", "eng+heb"  // שימוש גם באנגלית וגם בעברית
                );
            } else {
                // בלינוקס / מק
                processBuilder = new ProcessBuilder(
                        "tesseract",
                        tempImage.toString(),
                        outputTextFile.toString(),
                        "-l", "eng"
                );
            }

            // בצע את התהליך
            Process process = processBuilder.start();

            // קרא הודעות שגיאה
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                    System.err.println("Tesseract error: " + line);
                }
            }

            // המתן לסיום התהליך
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                return "שגיאת OCR (קוד יציאה " + exitCode + "): " + errorOutput.toString();
            }

            // קרא את קובץ הפלט
            Path textFilePath = Paths.get(outputTextFile + ".txt");
            String result = new String(Files.readAllBytes(textFilePath));

            // נקה קבצים זמניים
            Files.deleteIfExists(textFilePath);
            Files.deleteIfExists(tempImage);
            Files.deleteIfExists(tempDir);

            return result.trim();

        } catch (IOException | InterruptedException e) {
            System.err.println("שגיאה בתהליך OCR: " + e.getMessage());
            e.printStackTrace();
            return "שגיאה: " + e.getMessage();
        }
    }

    private String getFileExtension() {
        String originalFilename = image.getOriginalFilename();
        return originalFilename != null && originalFilename.contains(".")
                ? "." + originalFilename.substring(originalFilename.lastIndexOf(".") + 1)
                : ".png";
    }
}