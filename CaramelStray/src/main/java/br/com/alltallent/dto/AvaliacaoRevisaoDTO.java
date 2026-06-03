package br.com.alltallent.dto;

import br.com.alltallent.model.Avaliacao;
import br.com.alltallent.model.AvaliacaoFuncionario;
import br.com.alltallent.model.RespostaColaborador; 

import java.util.Collections;
import java.util.List;

public record AvaliacaoRevisaoDTO(
    Long avaliacaoFuncionarioCodigo,
    String nomeFuncionario,
    String tituloAvaliacao,
    String comentarioColaborador,
    String statusAtual,
    List<PerguntaComRespostaDTO> perguntasComRespostas
) {
    public AvaliacaoRevisaoDTO(AvaliacaoFuncionario instancia, Avaliacao avaliacaoBase) {
        this(
            instancia.getCodigo(),
            (instancia.getFuncionario() != null) ? instancia.getFuncionario().getNomeCompleto() : null,
            avaliacaoBase.getTitulo(),
            instancia.getComentarioColaborador(),
            instancia.getResultadoStatus(),
            mapPerguntas(instancia, avaliacaoBase)
        );
    }

    private static List<PerguntaComRespostaDTO> mapPerguntas(AvaliacaoFuncionario instancia, Avaliacao avaliacaoBase) {
        List<RespostaColaborador> respostas = instancia.getRespostas() != null
            ? List.copyOf(instancia.getRespostas())
            : Collections.emptyList();
        if (avaliacaoBase.getPerguntas() == null) {
            return Collections.emptyList();
        }
        return avaliacaoBase.getPerguntas().stream()
            .map(pergunta -> new PerguntaComRespostaDTO(pergunta, respostas))
            .toList();
    }
}
