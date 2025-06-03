package com.example.mediaid.bl.emergency;

import com.example.mediaid.dal.UserRepository;
import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TreatmentRecommendationEngine {

    private static final Logger logger = LoggerFactory.getLogger(TreatmentRecommendationEngine.class);

    @Autowired
    private Driver neo4jDriver;

    @Autowired
    private UserRepository userRepository;

    

}
