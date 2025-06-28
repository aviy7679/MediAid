package com.example.mediaid.dal.UMLS_terms;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "anatomical_structures")
@Data
@NoArgsConstructor
public class AnatomicalStructure extends BaseUmlsEntity {

}
