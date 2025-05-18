package com.example.mediaid.config;

import com.example.mediaid.security.encryption.EncryptionKeyManager;
import com.example.mediaid.security.encryption.EncryptionService;
import com.example.mediaid.utils.EncryptionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {

    @Bean
    public EncryptionKeyManager encryptionKeyManager(EncryptionProperties properties) {
        EncryptionKeyManager keyManager = new EncryptionKeyManager(properties.getKeyStorePath());
        // אם המפתח לא קיים, יצירת מפתח חדש
        if (!keyManager.containsKey(properties.getKeyAlias())) {
            keyManager.generateKey(properties.getKeyAlias(), properties.getKeyPassword());
        }
        return keyManager;
    }

    @Bean
    public EncryptionService encryptionService(EncryptionKeyManager keyManager) {
        return new EncryptionService(keyManager);
    }
}
