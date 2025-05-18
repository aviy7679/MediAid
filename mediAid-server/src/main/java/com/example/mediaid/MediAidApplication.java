package com.example.mediaid;

import com.example.mediaid.utils.EncryptionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(EncryptionProperties.class)
public class MediAidApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediAidApplication.class, args);
    }

}
