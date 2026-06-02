package br.com.alltallent.dto;

import br.com.alltallent.model.Competencia;

public record CompetenciaDTO(Integer id, String nome, String categoria) {
    public CompetenciaDTO(Competencia competencia) {
        this(
            competencia.getCodigo(), 
            competencia.getNome(),
            competencia.getCategoria() // O parêntese de fechamento vem AQUI, depois do último argumento
        );
    }
}