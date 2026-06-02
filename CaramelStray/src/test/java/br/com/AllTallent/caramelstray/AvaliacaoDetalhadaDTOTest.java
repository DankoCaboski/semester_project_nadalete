package br.com.AllTallent.caramelstray;

import br.com.AllTallent.dto.AvaliacaoDetalhadaDTO;
import br.com.AllTallent.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AvaliacaoDetalhadaDTOTest {

    private Avaliacao avaliacaoBase() {
        Avaliacao a = new Avaliacao();
        a.setCodigo(1);
        a.setTitulo("Avaliação Anual");
        a.setStatus("Rascunho");
        a.setDataCriacao(LocalDate.of(2025, 1, 10));
        a.setDataPrazo(LocalDate.of(2025, 3, 31));
        return a;
    }

    private Funcionario funcionario(int codigo, String nome) {
        Funcionario f = new Funcionario();
        f.setCodigo(codigo);
        f.setNomeCompleto(nome);
        return f;
    }

    private Pergunta pergunta(long codigo, String texto) {
        Pergunta p = new Pergunta();
        p.setCodigo(codigo);
        p.setEnunciado(texto);
        return p;
    }

    private AvaliacaoFuncionario instancia(Funcionario f, Avaliacao a, String status) {
        AvaliacaoFuncionario af = new AvaliacaoFuncionario(f, a);
        af.setCodigo(Long.valueOf(f.getCodigo()));
        af.setResultadoStatus(status);
        return af;
    }

    @Test
    void canonicalConstructor_defineCamposCorretamente() {
        var dto = new AvaliacaoDetalhadaDTO(
                99, "Título", "Ativo",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                "Criador X", List.of(), List.of());

        assertEquals(99, dto.codigo());
        assertEquals("Título", dto.titulo());
        assertEquals("Ativo", dto.status());
        assertEquals(LocalDate.of(2025, 1, 1), dto.dataCriacao());
        assertEquals(LocalDate.of(2025, 12, 31), dto.dataPrazo());
        assertEquals("Criador X", dto.nomeCriador());
        assertTrue(dto.perguntas().isEmpty());
        assertTrue(dto.instancias().isEmpty());
    }

    @Test
    void fromAvaliacao_mapeiaScalarsCorretamente() {
        Avaliacao a = avaliacaoBase();

        var dto = new AvaliacaoDetalhadaDTO(a);

        assertEquals(1, dto.codigo());
        assertEquals("Avaliação Anual", dto.titulo());
        assertEquals("Rascunho", dto.status());
        assertEquals(LocalDate.of(2025, 1, 10), dto.dataCriacao());
        assertEquals(LocalDate.of(2025, 3, 31), dto.dataPrazo());
    }

    @Test
    void fromAvaliacao_comCriador_mapeiaNameCriador() {
        Avaliacao a = avaliacaoBase();
        a.setCriador(funcionario(5, "Maria Criadora"));

        var dto = new AvaliacaoDetalhadaDTO(a);

        assertEquals("Maria Criadora", dto.nomeCriador());
    }

    @Test
    void fromAvaliacao_semCriador_nomeCriadorEhSistema() {
        Avaliacao a = avaliacaoBase();
        a.setCriador(null);

        var dto = new AvaliacaoDetalhadaDTO(a);

        assertEquals("Sistema", dto.nomeCriador());
    }

    @Test
    void fromAvaliacao_perguntasNulas_retornaListaVazia() {
        Avaliacao a = avaliacaoBase();
        a.setPerguntas(null);

        var dto = new AvaliacaoDetalhadaDTO(a);

        assertTrue(dto.perguntas().isEmpty());
    }

    @Test
    void fromAvaliacao_setVazioDePerguntas_retornaListaVazia() {
        Avaliacao a = avaliacaoBase();
        a.setPerguntas(new HashSet<>());

        var dto = new AvaliacaoDetalhadaDTO(a);

        assertTrue(dto.perguntas().isEmpty());
    }

    @Test
    void fromAvaliacao_comPerguntas_mapeiaLista() {
        Avaliacao a = avaliacaoBase();
        a.setPerguntas(Set.of(
                pergunta(10L, "Como foi seu desempenho?"),
                pergunta(11L, "Quais foram seus desafios?")));

        var dto = new AvaliacaoDetalhadaDTO(a);

        assertEquals(2, dto.perguntas().size());
        assertTrue(dto.perguntas().stream().anyMatch(p -> p.pergunta().equals("Como foi seu desempenho?")));
        assertTrue(dto.perguntas().stream().anyMatch(p -> p.pergunta().equals("Quais foram seus desafios?")));
    }

    @Test
    void fromAvaliacao_instanciasNulas_retornaListaVazia() {
        Avaliacao a = avaliacaoBase();
        a.setInstanciasAvaliacao(null);

        var dto = new AvaliacaoDetalhadaDTO(a);

        assertTrue(dto.instancias().isEmpty());
    }

    @Test
    void fromAvaliacao_setVazioDeInstancias_retornaListaVazia() {
        Avaliacao a = avaliacaoBase();
        a.setInstanciasAvaliacao(new HashSet<>());

        var dto = new AvaliacaoDetalhadaDTO(a);

        assertTrue(dto.instancias().isEmpty());
    }

    @Test
    void fromAvaliacao_comInstancias_mapeiaLista() {
        Avaliacao a = avaliacaoBase();
        Funcionario f1 = funcionario(10, "Pedro");
        Funcionario f2 = funcionario(20, "Lucia");
        a.setInstanciasAvaliacao(Set.of(
                instancia(f1, a, "PENDENTE"),
                instancia(f2, a, "CONCLUIDO")));

        var dto = new AvaliacaoDetalhadaDTO(a);

        assertEquals(2, dto.instancias().size());
        assertTrue(dto.instancias().stream().anyMatch(i -> "Pedro".equals(i.getFuncionarioNome())));
        assertTrue(dto.instancias().stream().anyMatch(i -> "Lucia".equals(i.getFuncionarioNome())));
    }

    @Test
    void fromAvaliacao_dataPrazoNula_propagaNulo() {
        Avaliacao a = avaliacaoBase();
        a.setDataPrazo(null);

        var dto = new AvaliacaoDetalhadaDTO(a);

        assertNull(dto.dataPrazo());
    }

    @Test
    void fromAvaliacao_todasRelacoesCombinadas() {
        Avaliacao a = avaliacaoBase();
        a.setCriador(funcionario(1, "Diretor"));
        a.setPerguntas(Set.of(pergunta(1L, "Pergunta?")));
        Funcionario avaliado = funcionario(2, "Colaborador");
        a.setInstanciasAvaliacao(Set.of(instancia(avaliado, a, "PENDENTE")));

        var dto = new AvaliacaoDetalhadaDTO(a);

        assertEquals("Diretor", dto.nomeCriador());
        assertEquals(1, dto.perguntas().size());
        assertEquals(1, dto.instancias().size());
    }
}