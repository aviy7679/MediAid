package com.example.mediaid.utils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Setter
@Getter
@ConfigurationProperties(prefix = "encryption")
public class EncryptionProperties {

    private String keyStorePath;
    private String keyAlias;
    private String keyPassword;

}
