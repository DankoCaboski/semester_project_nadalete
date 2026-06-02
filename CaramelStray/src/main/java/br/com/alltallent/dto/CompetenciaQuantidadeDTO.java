package br.com.alltallent.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CompetenciaQuantidadeDTO {
    private String nomeCompetencia;
    private Long quantidade;
}
