package com.example.mediaid.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String email;
    private String password;
    private String username;
    private String gender;
    private String dateOfBirth;
    private Integer height;
    private Integer weight;
}
