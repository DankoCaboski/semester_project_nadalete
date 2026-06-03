package br.com.alltallent.caramelstray;

import br.com.alltallent.dto.AvaliacaoRevisaoDTO;
import br.com.alltallent.model.*;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AvaliacaoRevisaoDTOTest {

    private Avaliacao avaliacaoBase(String titulo) {
        Avaliacao aval = new Avaliacao();
        aval.setTitulo(titulo);
        return aval;
    }

    private Pergunta pergunta(Long codigo, String enunciado) {
        Pergunta p = new Pergunta();
        p.setCodigo(codigo);
        p.setEnunciado(enunciado);
        return p;
    }

    private RespostaColaborador resposta(Long codigo, Pergunta pergunta, String texto) {
        RespostaColaborador r = new RespostaColaborador();
        r.setCodigo(codigo);
        r.setPergunta(pergunta);
        r.setRespostaTexto(texto);
        return r;
    }

    @Test
    void construtor_mapeaEscalaresCorretamente() {
        Avaliacao aval = avaliacaoBase("Avaliação Anual");

        AvaliacaoFuncionario inst = new AvaliacaoFuncionario();
        inst.setCodigo(42L);
        inst.setResultadoStatus("PENDENTE");
        inst.setComentarioColaborador("Bom trimestre");

        AvaliacaoRevisaoDTO dto = new AvaliacaoRevisaoDTO(inst, aval);

        assertEquals(42L, dto.avaliacaoFuncionarioCodigo());
        assertEquals("Avaliação Anual", dto.tituloAvaliacao());
        assertEquals("Bom trimestre", dto.comentarioColaborador());
        assertEquals("PENDENTE", dto.statusAtual());
    }

    @Test
    void construtor_nomeFuncionario_nulo_quandoFuncionarioNulo() {
        AvaliacaoFuncionario inst = new AvaliacaoFuncionario();
        inst.setCodigo(1L);

        AvaliacaoRevisaoDTO dto = new AvaliacaoRevisaoDTO(inst, avaliacaoBase("T"));

        assertNull(dto.nomeFuncionario());
    }

    @Test
    void construtor_nomeFuncionario_mapeado_quandoFuncionarioPresente() {
        Funcionario f = new Funcionario();
        f.setNomeCompleto("Maria Silva");

        Avaliacao avalBase = avaliacaoBase("T");
        AvaliacaoFuncionario inst = new AvaliacaoFuncionario(f, new Avaliacao());
        inst.setCodigo(1L);

        AvaliacaoRevisaoDTO dto = new AvaliacaoRevisaoDTO(inst, avalBase);

        assertEquals("Maria Silva", dto.nomeFuncionario());
    }

    @Test
    void mapPerguntas_retornaVazio_quandoPerguntasNulo() {
        Avaliacao aval = avaliacaoBase("T");
        // perguntas não inicializado → getPerguntas() == null
        AvaliacaoFuncionario inst = new AvaliacaoFuncionario();
        inst.setCodigo(1L);

        AvaliacaoRevisaoDTO dto = new AvaliacaoRevisaoDTO(inst, aval);

        assertTrue(dto.perguntasComRespostas().isEmpty());
    }

    @Test
    void mapPerguntas_retornaVazio_quandoPerguntasVazio() {
        Avaliacao aval = avaliacaoBase("T");
        aval.setPerguntas(new java.util.HashSet<>());

        AvaliacaoFuncionario inst = new AvaliacaoFuncionario();
        inst.setCodigo(1L);

        AvaliacaoRevisaoDTO dto = new AvaliacaoRevisaoDTO(inst, aval);

        assertTrue(dto.perguntasComRespostas().isEmpty());
    }

    @Test
    void mapPerguntas_mapeiaTodasAsPerguntas_quandoRespostasNulas() {
        Pergunta p1 = pergunta(1L, "Pergunta 1");
        Pergunta p2 = pergunta(2L, "Pergunta 2");

        Avaliacao aval = avaliacaoBase("T");
        aval.setPerguntas(Set.of(p1, p2));

        // sem addResposta → getRespostas() == null
        AvaliacaoFuncionario inst = new AvaliacaoFuncionario();
        inst.setCodigo(1L);

        AvaliacaoRevisaoDTO dto = new AvaliacaoRevisaoDTO(inst, aval);

        assertEquals(2, dto.perguntasComRespostas().size());
        assertTrue(dto.perguntasComRespostas().stream().allMatch(p -> p.respostaTexto() == null));
    }

    @Test
    void mapPerguntas_associaRespostaCorreta_quandoRespostaParaPergunta() {
        Pergunta p = pergunta(10L, "Pergunta");
        RespostaColaborador r = resposta(1L, p, "Minha resposta");

        Avaliacao aval = avaliacaoBase("T");
        aval.setPerguntas(Set.of(p));

        AvaliacaoFuncionario inst = new AvaliacaoFuncionario();
        inst.setCodigo(1L);
        inst.addResposta(r);

        AvaliacaoRevisaoDTO dto = new AvaliacaoRevisaoDTO(inst, aval);

        assertEquals(1, dto.perguntasComRespostas().size());
        assertEquals("Minha resposta", dto.perguntasComRespostas().get(0).respostaTexto());
    }

    @Test
    void mapPerguntas_deixaRespostaTextoNula_quandoSemRespostaParaPergunta() {
        Pergunta p1 = pergunta(10L, "Com resposta");
        Pergunta p2 = pergunta(20L, "Sem resposta");
        RespostaColaborador r = resposta(1L, p1, "Texto A");

        Avaliacao aval = avaliacaoBase("T");
        aval.setPerguntas(Set.of(p1, p2));

        AvaliacaoFuncionario inst = new AvaliacaoFuncionario();
        inst.setCodigo(1L);
        inst.addResposta(r);

        AvaliacaoRevisaoDTO dto = new AvaliacaoRevisaoDTO(inst, aval);

        assertEquals(2, dto.perguntasComRespostas().size());
        long comResposta = dto.perguntasComRespostas().stream()
                .filter(p -> p.respostaTexto() != null).count();
        assertEquals(1, comResposta);
    }

    @Test
    void mapPerguntas_ignoraRespostasDeOutrasPergunta() {
        Pergunta p = pergunta(10L, "Pergunta alvo");
        Pergunta outra = pergunta(99L, "Outra pergunta");
        RespostaColaborador rDeOutra = resposta(5L, outra, "Resposta de outra");

        Avaliacao aval = avaliacaoBase("T");
        aval.setPerguntas(Set.of(p));

        AvaliacaoFuncionario inst = new AvaliacaoFuncionario();
        inst.setCodigo(1L);
        inst.addResposta(rDeOutra);

        AvaliacaoRevisaoDTO dto = new AvaliacaoRevisaoDTO(inst, aval);

        assertEquals(1, dto.perguntasComRespostas().size());
        assertNull(dto.perguntasComRespostas().get(0).respostaTexto());
    }
}
