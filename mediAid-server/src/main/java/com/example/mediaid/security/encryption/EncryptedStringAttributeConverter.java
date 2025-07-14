package com.example.mediaid.security.encryption;

import com.example.mediaid.config.ApplicationContextProvider;
import com.example.mediaid.utils.EncryptionProperties;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptedStringAttributeConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        try {
            EncryptionService encryptionService = ApplicationContextProvider.getBean(EncryptionService.class);
            EncryptionProperties encryptionProperties = ApplicationContextProvider.getBean(EncryptionProperties.class);

            String result = encryptionService.encrypt(attribute,
                    encryptionProperties.getKeyAlias(),
                    encryptionProperties.getKeyPassword());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        try {
            EncryptionService encryptionService = ApplicationContextProvider.getBean(EncryptionService.class);
            EncryptionProperties encryptionProperties = ApplicationContextProvider.getBean(EncryptionProperties.class);

            String result = encryptionService.decrypt(dbData,
                    encryptionProperties.getKeyAlias(),
                    encryptionProperties.getKeyPassword());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error decrypting data", e);
        }
    }
}