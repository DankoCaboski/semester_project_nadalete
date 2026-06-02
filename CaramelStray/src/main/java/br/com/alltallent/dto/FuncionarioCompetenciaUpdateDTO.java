package br.com.alltallent.dto;

import java.util.List;

public record FuncionarioCompetenciaUpdateDTO(
    List<Integer> codigosCompetencia 
) {}