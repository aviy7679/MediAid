package com.example.mediaid.bl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Neo4jInitializer implements CommandLineRunner {

    private final DataSynchronizationService synchronizationService;

    @Autowired
    public Neo4jInitializer(DataSynchronizationService synchronizationService) {
        this.synchronizationService = synchronizationService;
    }
    @Override
    public void run(String... args) {
        System.out.println("Initializing Neo4j");
        synchronizationService.initialSync();
    }
}
