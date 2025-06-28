package com.example.mediaid.dal.UMLS_terms;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lab_tests")
@Data
@NoArgsConstructor
public class LabTest extends BaseUmlsEntity {

}
