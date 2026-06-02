package br.com.AllTallent.caramelstray;

import br.com.AllTallent.model.Avaliacao;
import br.com.AllTallent.model.AvaliacaoFuncionario;
import br.com.AllTallent.model.Funcionario;
import br.com.AllTallent.model.Pergunta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class AvaliacaoTest {

    private Pergunta novaPergunta(long codigo) {
        Pergunta p = new Pergunta();
        p.setCodigo(codigo);
        return p;
    }

    private AvaliacaoFuncionario instancia(Avaliacao avaliacao) {
        Funcionario f = new Funcionario();
        f.setCodigo(1);
        return new AvaliacaoFuncionario(f, avaliacao);
    }

    @Test
    void onCreate_defineDataCriacaoEStatusRascunho_quandoStatusNulo() {
        Avaliacao a = new Avaliacao();
        a.setStatus(null);

        ReflectionTestUtils.invokeMethod(a, "onCreate");

        assertEquals(LocalDate.now(), a.getDataCriacao());
        assertEquals("Rascunho", a.getStatus());
    }

    @ParameterizedTest
    @CsvSource({
        "'',     Rascunho",
        "'   ',  Rascunho",
        "Ativo,  Ativo"
    })
    void onCreate_defineStatusCorreto(String statusInicial, String statusEsperado) {
        Avaliacao a = new Avaliacao();
        a.setStatus(statusInicial);

        ReflectionTestUtils.invokeMethod(a, "onCreate");

        assertEquals(statusEsperado.trim(), a.getStatus());
    }

    @Test
    void addPergunta_adicionaPergunta_quandoSetNulo() {
        Avaliacao a = new Avaliacao();
        a.setPerguntas(null);
        Pergunta p = novaPergunta(1L);

        a.addPergunta(p);

        assertNotNull(a.getPerguntas());
        assertEquals(1, a.getPerguntas().size());
        assertTrue(a.getPerguntas().contains(p));
    }

    @Test
    void addPergunta_adicionaPergunta_quandoSetJaExiste() {
        Avaliacao a = new Avaliacao();
        a.setPerguntas(new HashSet<>());
        a.addPergunta(novaPergunta(1L));

        a.addPergunta(novaPergunta(2L));

        assertEquals(2, a.getPerguntas().size());
    }

    @Test
    void removePergunta_removePergunta_quandoPresente() {
        Avaliacao a = new Avaliacao();
        Pergunta p = novaPergunta(1L);
        a.setPerguntas(new HashSet<>());
        a.getPerguntas().add(p);

        a.removePergunta(p);

        assertTrue(a.getPerguntas().isEmpty());
    }

    @Test
    void removePergunta_naoLancaExcecao_quandoSetNulo() {
        Avaliacao a = new Avaliacao();
        a.setPerguntas(null);

        assertDoesNotThrow(() -> a.removePergunta(novaPergunta(1L)));
    }

    @Test
    void addInstancia_adicionaInstanciaEAtribuiAvaliacao_quandoSetNulo() {
        Avaliacao a = new Avaliacao();
        a.setInstanciasAvaliacao(null);
        AvaliacaoFuncionario inst = instancia(a);

        a.addInstancia(inst);

        assertNotNull(a.getInstanciasAvaliacao());
        assertEquals(1, a.getInstanciasAvaliacao().size());
        assertSame(a, inst.getAvaliacao());
    }

    @Test
    void addInstancia_adicionaInstancia_quandoSetJaExiste() {
        Avaliacao a = new Avaliacao();
        a.setInstanciasAvaliacao(new HashSet<>());
        AvaliacaoFuncionario inst1 = instancia(a);
        inst1.setCodigo(1L);
        AvaliacaoFuncionario inst2 = instancia(a);
        inst2.setCodigo(2L);

        a.addInstancia(inst1);
        a.addInstancia(inst2);

        assertEquals(2, a.getInstanciasAvaliacao().size());
    }

    @Test
    void removeInstancia_removeInstanciaENulificaAvaliacao_quandoPresente() {
        Avaliacao a = new Avaliacao();
        a.setInstanciasAvaliacao(new HashSet<>());
        AvaliacaoFuncionario inst = instancia(a);
        inst.setCodigo(10L);
        a.getInstanciasAvaliacao().add(inst);

        a.removeInstancia(inst);

        assertTrue(a.getInstanciasAvaliacao().isEmpty());
        assertNull(inst.getAvaliacao());
    }

    @Test
    void removeInstancia_naoLancaExcecao_quandoSetNulo() {
        Avaliacao a = new Avaliacao();
        a.setInstanciasAvaliacao(null);
        AvaliacaoFuncionario inst = instancia(a);

        assertDoesNotThrow(() -> a.removeInstancia(inst));
    }

    @Test
    void equalsHashCode_baseadoEmCodigo() {
        Avaliacao a1 = new Avaliacao();
        a1.setCodigo(1);
        Avaliacao a2 = new Avaliacao();
        a2.setCodigo(1);
        Avaliacao a3 = new Avaliacao();
        a3.setCodigo(2);

        assertEquals(a1, a2);
        assertNotEquals(a1, a3);
        assertEquals(a1.hashCode(), a2.hashCode());
    }
}