package com.example.mediaid.dal.UMLS_terms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "symptoms")
@Data
@NoArgsConstructor
public class Symptom extends BaseUmlsEntity {

}
