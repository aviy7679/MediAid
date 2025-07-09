package com.example.mediaid.config;

import com.example.mediaid.constants.DatabaseConstants;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    private static final Properties prop = new Properties();
    static {
        try {
            FileInputStream fis = new FileInputStream(DatabaseConstants.CONFIG_FILE_PATH);
            prop.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public static String getProperty(String key) {
        return prop.getProperty(key);
    }

}