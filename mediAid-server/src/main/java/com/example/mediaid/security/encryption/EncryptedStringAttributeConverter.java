package com.example.mediaid.security.encryption;

import com.example.mediaid.utils.EncryptionProperties;
import jakarta.persistence.AttributeConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EncryptedStringAttributeConverter implements AttributeConverter<String, String> {

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private EncryptionProperties encryptionProperties;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        try {
            return encryptionService.encrypt(attribute, encryptionProperties.getKeyAlias(),encryptionProperties.getKeyPassword());
        }catch (Exception e) {
            throw new RuntimeException("Error encrypting data",e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        try{
            return encryptionService.decrypt(dbData, encryptionProperties.getKeyAlias(),encryptionProperties.getKeyPassword());
        }catch (Exception e) {
            throw new RuntimeException("Error decrypting data",e);
        }
    }
}
