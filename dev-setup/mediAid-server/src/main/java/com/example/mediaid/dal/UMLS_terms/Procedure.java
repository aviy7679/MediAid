package com.example.mediaid.dal.UMLS_terms;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "procedures")
@Data
@NoArgsConstructor
public class Procedure extends BaseUmlsEntity {

}