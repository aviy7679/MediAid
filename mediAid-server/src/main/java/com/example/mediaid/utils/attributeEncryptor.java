package com.example.mediaid.utils;

import com.example.mediaid.security.encryption.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Converter(autoApply = false)
public class attributeEncryptor implements AttributeConverter<String, String> {

    private final EncryptionService encryptionService;
    private final String keyAlias;
    private final String password;

    @Autowired
    public attributeEncryptor(EncryptionService encryptionService,
                              EncryptionProperties encryptionProperties) {
        this.encryptionService = encryptionService;
        this.keyAlias = encryptionProperties.getKeyAlias();
        this.password = encryptionProperties.getKeyPassword();
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptionService.encrypt(attribute, keyAlias, password);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return encryptionService.decrypt(dbData, keyAlias, password);
    }
}

