package br.com.alltallent.dto;

import br.com.alltallent.model.PerguntaOpcao; 

public record PerguntaOpcaoDTO(
    Long codigo,         
    String descricaoOpcao 
) {
    
    public PerguntaOpcaoDTO(PerguntaOpcao entidade) {
        this(entidade.getCodigo(), entidade.getDescricaoOpcao());
    }
}