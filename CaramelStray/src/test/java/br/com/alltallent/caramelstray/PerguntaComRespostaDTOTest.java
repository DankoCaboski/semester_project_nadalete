package br.com.alltallent.caramelstray;

import br.com.alltallent.dto.PerguntaComRespostaDTO;
import br.com.alltallent.model.Pergunta;
import br.com.alltallent.model.PerguntaOpcao;
import br.com.alltallent.model.RespostaColaborador;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PerguntaComRespostaDTOTest {

    private Pergunta pergunta(long codigo, String texto, String tipo) {
        Pergunta p = new Pergunta();
        p.setCodigo(codigo);
        p.setEnunciado(texto);
        p.setTipoPergunta(tipo);
        return p;
    }

    private PerguntaOpcao opcao(long codigo, String descricao) {
        PerguntaOpcao o = new PerguntaOpcao();
        o.setCodigo(codigo);
        o.setDescricaoOpcao(descricao);
        return o;
    }

    private RespostaColaborador resposta(Pergunta pergunta, String texto, PerguntaOpcao opcaoSelecionada) {
        RespostaColaborador r = new RespostaColaborador();
        r.setPergunta(pergunta);
        r.setRespostaTexto(texto);
        r.setOpcaoSelecionada(opcaoSelecionada);
        return r;
    }

    @Test
    void canonicalConstructor_defineCamposCorretamente() {
        var dto = new PerguntaComRespostaDTO(1L, "Texto?", "ABERTA", List.of(), "Resposta", null);

        assertEquals(1L, dto.perguntaCodigo());
        assertEquals("Texto?", dto.perguntaTexto());
        assertEquals("ABERTA", dto.tipoPergunta());
        assertTrue(dto.opcoes().isEmpty());
        assertEquals("Resposta", dto.respostaTexto());
        assertNull(dto.opcaoSelecionadaCodigo());
    }

    @Test
    void fromPerguntaEResposta_respostaNula_camposRespostaNulos() {
        Pergunta p = pergunta(10L, "Como foi?", "ABERTA");
        p.setOpcoes(Set.of());

        var dto = new PerguntaComRespostaDTO(p, (RespostaColaborador) null);

        assertEquals(10L, dto.perguntaCodigo());
        assertEquals("Como foi?", dto.perguntaTexto());
        assertNull(dto.respostaTexto());
        assertNull(dto.opcaoSelecionadaCodigo());
    }

    @Test
    void fromPerguntaEResposta_respostaComTexto_mapeiaTexto() {
        Pergunta p = pergunta(10L, "Descreva.", "ABERTA");
        p.setOpcoes(Set.of());
        RespostaColaborador r = resposta(p, "Minha resposta", null);

        var dto = new PerguntaComRespostaDTO(p, r);

        assertEquals("Minha resposta", dto.respostaTexto());
        assertNull(dto.opcaoSelecionadaCodigo());
    }

    @Test
    void fromPerguntaEResposta_respostaComOpcao_mapeiaOpcaoSelecionadaCodigo() {
        Pergunta p = pergunta(10L, "Qual?", "MULTIPLA_ESCOLHA");
        PerguntaOpcao op = opcao(5L, "Opção A");
        p.setOpcoes(Set.of(op));
        RespostaColaborador r = resposta(p, null, op);

        var dto = new PerguntaComRespostaDTO(p, r);

        assertEquals(5L, dto.opcaoSelecionadaCodigo());
        assertNull(dto.respostaTexto());
        assertEquals(1, dto.opcoes().size());
        assertEquals(5L, dto.opcoes().get(0).codigo());
        assertEquals("Opção A", dto.opcoes().get(0).descricaoOpcao());
    }

    @Test
    void fromPerguntaEResposta_opcoesNulas_retornaListaVazia() {
        Pergunta p = pergunta(10L, "Texto?", "MULTIPLA_ESCOLHA");
        p.setOpcoes(null);

        var dto = new PerguntaComRespostaDTO(p, (RespostaColaborador) null);

        assertTrue(dto.opcoes().isEmpty());
    }

    @Test
    void fromPerguntaEListaRespostas_encontraRespostaDaPergunta() {
        Pergunta p = pergunta(10L, "Como?", "ABERTA");
        p.setOpcoes(Set.of());
        RespostaColaborador r = resposta(p, "Bem", null);

        var dto = new PerguntaComRespostaDTO(p, List.of(r));

        assertEquals("Bem", dto.respostaTexto());
        assertNull(dto.opcaoSelecionadaCodigo());
    }

    @Test
    void fromPerguntaEListaRespostas_nenhumaRespostaBate_camposNulos() {
        Pergunta p = pergunta(10L, "Texto?", "ABERTA");
        p.setOpcoes(Set.of());

        Pergunta outra = pergunta(99L, "Outra?", "ABERTA");
        RespostaColaborador r = resposta(outra, "Irrelevante", null);

        var dto = new PerguntaComRespostaDTO(p, List.of(r));

        assertNull(dto.respostaTexto());
        assertNull(dto.opcaoSelecionadaCodigo());
    }

    @Test
    void fromPerguntaEListaRespostas_respostaComOpcaoSelecionada_mapeiaOpcao() {
        Pergunta p = pergunta(10L, "Qual?", "MULTIPLA_ESCOLHA");
        PerguntaOpcao op = opcao(7L, "Opção B");
        p.setOpcoes(Set.of(op));
        RespostaColaborador r = resposta(p, null, op);

        var dto = new PerguntaComRespostaDTO(p, List.of(r));

        assertEquals(7L, dto.opcaoSelecionadaCodigo());
    }

    @Test
    void fromPerguntaEListaRespostas_listaVazia_camposNulos() {
        Pergunta p = pergunta(10L, "Texto?", "ABERTA");
        p.setOpcoes(Set.of());

        var dto = new PerguntaComRespostaDTO(p, List.of());

        assertNull(dto.respostaTexto());
        assertNull(dto.opcaoSelecionadaCodigo());
    }

    @Test
    void fromPerguntaEListaRespostas_multiplosItens_usaPrimeiroBatimento() {
        Pergunta p = pergunta(10L, "Como?", "ABERTA");
        p.setOpcoes(Set.of());
        RespostaColaborador r1 = resposta(p, "Primeira", null);
        RespostaColaborador r2 = resposta(p, "Segunda", null);

        var dto = new PerguntaComRespostaDTO(p, List.of(r1, r2));

        assertEquals("Primeira", dto.respostaTexto());
    }
}