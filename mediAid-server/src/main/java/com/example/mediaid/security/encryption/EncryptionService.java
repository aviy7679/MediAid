package com.example.mediaid.security.encryption;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionService {
    private final EncryptionKeyManager keyManager;

    public EncryptionService(EncryptionKeyManager keyManager) {
        this.keyManager = keyManager;
    }

    //הצפנה
    public String encrypt(String plainText, String keyAlias, String password) {
        try{
            SecretKey key= keyManager.retrieveKey(keyAlias, password);
            Cipher chipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = generateIV();
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            chipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] encryptedData = chipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length+encryptedData.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedData);

            return Base64.getEncoder().encodeToString(byteBuffer.array());

        }catch(Exception e){
            throw new SecurityException("Error encrypting text", e);
        }
    }

    //הצפנה
    public String decrypt(String encryptedText, String keyAlias, String password) {
        try{
            SecretKey key= keyManager.retrieveKey(keyAlias, password);
            byte[] encryptedData = Base64.getDecoder().decode(encryptedText);

            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[12];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher  = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] decryptedData = cipher.doFinal(cipherText);
            return new String(decryptedData, StandardCharsets.UTF_8);

        }catch (Exception e){
            throw new SecurityException("Error decrypting text", e);
        }
    }

    private byte[] generateIV() {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

}
