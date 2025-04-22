package com.example.mediaid.bl.extract_data_from_EHR;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class Text_from_image {

    public String processOCR(MultipartFile image) {
        try {
            // יצירת קובץ זמני מהתמונה שהועלתה
            Path tempDir = Files.createTempDirectory("ocr-temp");
            Path tempImage = tempDir.resolve("image" + getFileExtension(image));
            image.transferTo(tempImage.toFile());

            System.out.println("temp picture: " + tempImage.toAbsolutePath());

            // נתיב לקובץ פלט טקסט
            Path outputTextFile = tempDir.resolve("output");

            ProcessBuilder processBuilder;

            // בווינדוס חייב לכתוב נתיב מלא
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

            if (isWindows) {
                String tesseractPath = "C:\\Program Files\\Tesseract-OCR\\tesseract.exe";

                processBuilder = new ProcessBuilder(
                        tesseractPath,
                        tempImage.toString(),
                        outputTextFile.toString(),
                        "-l", "eng+heb"  // שימוש גם באנגלית וגם בעברית
                );
            } else {
                processBuilder = new ProcessBuilder(
                        "tesseract",
                        tempImage.toString(),
                        outputTextFile.toString(),
                        "-l", "eng"
                );
            }

            Process process = processBuilder.start();

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
                return "OCR error (exit code " + exitCode + "): " + errorOutput.toString();
            }

            // הוצאת הפלט מהקובץ הזמני
            Path textFilePath = Paths.get(outputTextFile + ".txt");
            String result = new String(Files.readAllBytes(textFilePath));

            // מחיקת קבצים זמניים
            Files.deleteIfExists(textFilePath);
            Files.deleteIfExists(tempImage);
            Files.deleteIfExists(tempDir);

            return result.trim();

        } catch (IOException | InterruptedException e) {
            System.err.println("error in OCR process: " + e.getMessage());
            e.printStackTrace();
            return "error: " + e.getMessage();
        }
    }

    // הוצאת סוג הקובץ. ברירת מחדל: png
    private String getFileExtension(MultipartFile image) {
        String originalFilename = image.getOriginalFilename();
        return originalFilename != null && originalFilename.contains(".")
                ? "." + originalFilename.substring(originalFilename.lastIndexOf(".") + 1)
                : ".png";
    }
}