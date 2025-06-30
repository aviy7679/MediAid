package com.example.mediaid.dal.UMLS_terms;

import com.example.mediaid.dal.UMLS_terms.BaseUmlsEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "diseases")
@Data
@NoArgsConstructor
public class Disease extends BaseUmlsEntity {

}