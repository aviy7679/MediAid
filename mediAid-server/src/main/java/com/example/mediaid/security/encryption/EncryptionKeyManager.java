package com.example.mediaid.security.encryption;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Base64;

public class EncryptionKeyManager {
    //פורמט של Keystore
    private static final String KEY_STORE_TYPE = "PKCS12";
    private static final String KEY_ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    //מיקום קובץ Keystores מהמשתמש
    private final String keyStorePath;
    private KeyStore keyStore;

    public EncryptionKeyManager(String keyStorePath) {
        this.keyStorePath = keyStorePath;
        initializeKeyStore();
    }

    private void initializeKeyStore() {
        try {
            keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
            File keystoreFile = new File(keyStorePath);
            if(keystoreFile.exists()) {
                try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                    keyStore.load(fis,null);
                }
            }else{
                keyStore.load(null, null);
                saveKeyStore(null);
            }
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new SecurityException("Error initialize key store: ",e);
        }
    }
    public void storeKey(String keyAlias, SecretKey key, String password) {
        KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(key);
        try{
            KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(password.toCharArray());

            keyStore.setEntry(keyAlias, secretKeyEntry, protParam);

            saveKeyStore(password);
        }catch(KeyStoreException e){
            throw new SecurityException("Error saving key: ",e);
        }
    }

    public  SecretKey retrieveKey(String keyAlias, String password) {
        try{
            if(!keyStore.containsAlias(keyAlias))
                throw new SecurityException("Key " + keyAlias + " not found");
            Key key = keyStore.getKey(keyAlias, password.toCharArray());
            if(key instanceof SecretKey)
                return (SecretKey) key;
            throw new SecurityException("the key " + keyAlias + " is not a SecretKey");
        } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
            throw new SecurityException("Error retrieving key: ",e);
        }
    }

    public SecretKey generateKey(String keyAlias, String password) {
        try{
            KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM);
            keyGen.init(KEY_SIZE);
            SecretKey key = keyGen.generateKey();
            storeKey(keyAlias, key, password);
            return key;
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException("Error creating encryption key: ",e);
        }
    }

    public void rotateKey(String keyAlias, String password) throws KeyStoreException {
        try{
            if(keyStore.containsAlias(keyAlias)){
                SecretKey newKey = generateKey(keyAlias+"_new", password);
                ///////////////////////////////////////////////
                ////עדכון כל השדות המוצפנים שיוצפנו במפתח החדש/
                ///////////////////////////////////////////////
                keyStore.deleteEntry(keyAlias);
                SecretKey renamedKey = retrieveKey(keyAlias+"_new", password);
                storeKey(keyAlias, renamedKey, password);
                keyStore.deleteEntry(keyAlias+"_new");
                saveKeyStore(password);
            }else{
                throw new SecurityException("Key " + keyAlias + " not found");
            }
        }catch(KeyStoreException e){
            throw new SecurityException("Error rotating key: ",e);
        }
    }

    public String exportKey(String keyAlias, String password) {
        SecretKey key = retrieveKey(keyAlias, password);
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public void importKey(String keyAlias, String encodedKey, String password) {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        SecretKey key = new SecretKeySpec(decodedKey, 0, decodedKey.length, KEY_ALGORITHM);
        storeKey(keyAlias, key, password);
    }

    private void saveKeyStore(String password) {
        try(FileOutputStream fos = new FileOutputStream(keyStorePath)){
            keyStore.store(fos,password!=null?password.toCharArray():null);
        }catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            throw new SecurityException("Error saving keyStore: ",e);
        }
    }

    public boolean containsKey(String keyAlias) {
        try {
            return keyStore.containsAlias(keyAlias);
        } catch (KeyStoreException e) {
            throw new SecurityException("Error checking the key", e);
        }
    }

    public void deleteKey(String keyAlias) {
        try {
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias);
                saveKeyStore(null);
            }
        } catch (KeyStoreException e) {
            throw new SecurityException("Error delete key", e);
        }
    }

}
