package com.example.mediaid.encryption;

import com.example.mediaid.utils.Config;
import jakarta.persistence.AttributeConverter;
import org.jasypt.util.text.AES256TextEncryptor;

public class AttributeEncryptor implements AttributeConverter<String, String> {
    private final AES256TextEncryptor textEncryptor;
    public AttributeEncryptor() {
        textEncryptor = new AES256TextEncryptor();
        textEncryptor.setPassword(Config.getProperty("A"));
    }
    @Override
    public String convertToDatabaseColumn(String s) {
        return "";
    }

    @Override
    public String convertToEntityAttribute(String s) {
        return "";
    }
}
