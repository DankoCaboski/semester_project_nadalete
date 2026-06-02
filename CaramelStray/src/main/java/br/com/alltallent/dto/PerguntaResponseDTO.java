package br.com.alltallent.dto; 

import br.com.alltallent.model.Pergunta;

public record PerguntaResponseDTO(
    Long codigo,
    String pergunta,
    Integer competenciaCodigo,
    String competenciaNome
    
) {
    public PerguntaResponseDTO(Pergunta entidade) {
        this(
            entidade.getCodigo(),
            entidade.getEnunciado(),
            entidade.getCompetencia() != null ? entidade.getCompetencia().getCodigo() : null,
            entidade.getCompetencia() != null ? entidade.getCompetencia().getNome() : null
        );
    }
    public record OpcaoRequest(String descricao, boolean isCorreta) {}

}