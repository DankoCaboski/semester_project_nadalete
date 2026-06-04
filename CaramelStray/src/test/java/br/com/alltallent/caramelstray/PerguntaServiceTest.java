package br.com.alltallent.caramelstray;

import br.com.alltallent.dto.OpcaoRequest;
import br.com.alltallent.dto.PerguntaRequestDTO;
import br.com.alltallent.dto.PerguntaResponseDTO;
import br.com.alltallent.model.Competencia;
import br.com.alltallent.model.Pergunta;
import br.com.alltallent.repository.CompetenciaRepository;
import br.com.alltallent.repository.PerguntaRepository;
import br.com.alltallent.service.PerguntaService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerguntaServiceTest {

    @Mock private PerguntaRepository perguntaRepository;
    @Mock private CompetenciaRepository competenciaRepository;

    @InjectMocks
    private PerguntaService perguntaService;

    private Competencia competencia(int codigo, String nome) {
        Competencia c = new Competencia();
        c.setCodigo(codigo);
        c.setNome(nome);
        return c;
    }

    private Pergunta pergunta(Long codigo, String enunciado, Competencia competencia) {
        Pergunta p = new Pergunta();
        p.setCodigo(codigo);
        p.setEnunciado(enunciado);
        p.setCompetencia(competencia);
        return p;
    }

    // -------------------------------------------------------------------------
    // criarPergunta
    // -------------------------------------------------------------------------

    @Test
    void criarPergunta_salvaERetornaDTO_quandoPerguntaSimples() {
        Competencia comp = competencia(1, "Comunicação");
        PerguntaRequestDTO dto = new PerguntaRequestDTO("Como você se comunica?", 1, "Dissertativa", null);

        Pergunta salva = pergunta(10L, "Como você se comunica?", comp);

        when(competenciaRepository.findById(1)).thenReturn(Optional.of(comp));
        when(perguntaRepository.save(any())).thenReturn(salva);

        PerguntaResponseDTO resultado = perguntaService.criarPergunta(dto);

        assertNotNull(resultado);
        assertEquals(10L, resultado.codigo());
        assertEquals("Como você se comunica?", resultado.pergunta());
        verify(perguntaRepository).save(any());
    }

    @Test
    void criarPergunta_adicionaOpcoes_quandoTipoContemMultiplaComAcento() {
        Competencia comp = competencia(1, "Liderança");
        List<OpcaoRequest> opcoes = List.of(
                new OpcaoRequest("Opção A", true),
                new OpcaoRequest("Opção B", false)
        );
        PerguntaRequestDTO dto = new PerguntaRequestDTO("Qual sua liderança?", 1, "Múltipla Escolha", opcoes);

        Pergunta salva = pergunta(20L, "Qual sua liderança?", comp);

        when(competenciaRepository.findById(1)).thenReturn(Optional.of(comp));
        when(perguntaRepository.save(any())).thenReturn(salva);

        perguntaService.criarPergunta(dto);

        ArgumentCaptor<Pergunta> captor = ArgumentCaptor.forClass(Pergunta.class);
        verify(perguntaRepository).save(captor.capture());
        assertNotNull(captor.getValue().getOpcoes());
        assertEquals(2, captor.getValue().getOpcoes().size());
    }

    @Test
    void criarPergunta_adicionaOpcoes_quandoTipoContemMultiplaSemAcento() {
        Competencia comp = competencia(1, "Liderança");
        List<OpcaoRequest> opcoes = List.of(new OpcaoRequest("Sim", true));
        PerguntaRequestDTO dto = new PerguntaRequestDTO("Pergunta?", 1, "multipla escolha", opcoes);

        Pergunta salva = pergunta(21L, "Pergunta?", comp);

        when(competenciaRepository.findById(1)).thenReturn(Optional.of(comp));
        when(perguntaRepository.save(any())).thenReturn(salva);

        perguntaService.criarPergunta(dto);

        ArgumentCaptor<Pergunta> captor = ArgumentCaptor.forClass(Pergunta.class);
        verify(perguntaRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getOpcoes().size());
    }

    @Test
    void criarPergunta_ignoraOpcaoComDescricaoNula() {
        Competencia comp = competencia(1, "Técnica");
        List<OpcaoRequest> opcoes = List.of(
                new OpcaoRequest(null, false),
                new OpcaoRequest("Válida", true)
        );
        PerguntaRequestDTO dto = new PerguntaRequestDTO("Pergunta?", 1, "Múltipla Escolha", opcoes);

        Pergunta salva = pergunta(30L, "Pergunta?", comp);

        when(competenciaRepository.findById(1)).thenReturn(Optional.of(comp));
        when(perguntaRepository.save(any())).thenReturn(salva);

        perguntaService.criarPergunta(dto);

        ArgumentCaptor<Pergunta> captor = ArgumentCaptor.forClass(Pergunta.class);
        verify(perguntaRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getOpcoes().size());
    }

    @Test
    void criarPergunta_ignoraOpcaoComDescricaoEmBranco() {
        Competencia comp = competencia(1, "Técnica");
        List<OpcaoRequest> opcoes = List.of(
                new OpcaoRequest("   ", false),
                new OpcaoRequest("Válida", true)
        );
        PerguntaRequestDTO dto = new PerguntaRequestDTO("Pergunta?", 1, "Múltipla Escolha", opcoes);

        Pergunta salva = pergunta(31L, "Pergunta?", comp);

        when(competenciaRepository.findById(1)).thenReturn(Optional.of(comp));
        when(perguntaRepository.save(any())).thenReturn(salva);

        perguntaService.criarPergunta(dto);

        ArgumentCaptor<Pergunta> captor = ArgumentCaptor.forClass(Pergunta.class);
        verify(perguntaRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getOpcoes().size());
    }

    @Test
    void criarPergunta_naoAdicionaOpcoes_quandoTipoMultiplaEListaDeOpcoesNula() {
        Competencia comp = competencia(1, "Técnica");
        PerguntaRequestDTO dto = new PerguntaRequestDTO("Pergunta?", 1, "Múltipla Escolha", null);

        Pergunta salva = pergunta(32L, "Pergunta?", comp);

        when(competenciaRepository.findById(1)).thenReturn(Optional.of(comp));
        when(perguntaRepository.save(any())).thenReturn(salva);

        perguntaService.criarPergunta(dto);

        ArgumentCaptor<Pergunta> captor = ArgumentCaptor.forClass(Pergunta.class);
        verify(perguntaRepository).save(captor.capture());
        assertNull(captor.getValue().getOpcoes());
    }

    @Test
    void criarPergunta_naoAdicionaOpcoes_quandoTipoNaoEMultipla() {
        Competencia comp = competencia(1, "Técnica");
        List<OpcaoRequest> opcoes = List.of(new OpcaoRequest("Sim", true));
        PerguntaRequestDTO dto = new PerguntaRequestDTO("Pergunta?", 1, "Dissertativa", opcoes);

        Pergunta salva = pergunta(33L, "Pergunta?", comp);

        when(competenciaRepository.findById(1)).thenReturn(Optional.of(comp));
        when(perguntaRepository.save(any())).thenReturn(salva);

        perguntaService.criarPergunta(dto);

        ArgumentCaptor<Pergunta> captor = ArgumentCaptor.forClass(Pergunta.class);
        verify(perguntaRepository).save(captor.capture());
        assertNull(captor.getValue().getOpcoes());
    }

    @Test
    void criarPergunta_naoAdicionaOpcoes_quandoTipoNulo() {
        Competencia comp = competencia(1, "Técnica");
        List<OpcaoRequest> opcoes = List.of(new OpcaoRequest("Sim", true));
        PerguntaRequestDTO dto = new PerguntaRequestDTO("Pergunta?", 1, null, opcoes);

        Pergunta salva = pergunta(34L, "Pergunta?", comp);

        when(competenciaRepository.findById(1)).thenReturn(Optional.of(comp));
        when(perguntaRepository.save(any())).thenReturn(salva);

        perguntaService.criarPergunta(dto);

        ArgumentCaptor<Pergunta> captor = ArgumentCaptor.forClass(Pergunta.class);
        verify(perguntaRepository).save(captor.capture());
        assertNull(captor.getValue().getOpcoes());
    }

    @Test
    void criarPergunta_lancaEntityNotFound_quandoCompetenciaNaoExiste() {
        PerguntaRequestDTO dto = new PerguntaRequestDTO("Pergunta?", 99, "Dissertativa", null);

        when(competenciaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> perguntaService.criarPergunta(dto));
        verify(perguntaRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // listarTodas
    // -------------------------------------------------------------------------

    @Test
    void listarTodas_retornaListaMapeada() {
        Competencia comp = competencia(1, "Comunicação");
        Pergunta p1 = pergunta(1L, "Pergunta 1", comp);
        Pergunta p2 = pergunta(2L, "Pergunta 2", comp);

        when(perguntaRepository.findAll()).thenReturn(List.of(p1, p2));

        List<PerguntaResponseDTO> resultado = perguntaService.listarTodas();

        assertEquals(2, resultado.size());
        assertEquals(1L, resultado.get(0).codigo());
        assertEquals(2L, resultado.get(1).codigo());
    }

    @Test
    void listarTodas_retornaListaVazia_quandoNaoHaPerguntas() {
        when(perguntaRepository.findAll()).thenReturn(List.of());

        List<PerguntaResponseDTO> resultado = perguntaService.listarTodas();

        assertTrue(resultado.isEmpty());
    }

    // -------------------------------------------------------------------------
    // buscarPorId
    // -------------------------------------------------------------------------

    @Test
    void buscarPorId_retornaDTO_quandoEncontrado() {
        Competencia comp = competencia(1, "Liderança");
        Pergunta p = pergunta(5L, "Enunciado", comp);

        when(perguntaRepository.findById(5L)).thenReturn(Optional.of(p));

        PerguntaResponseDTO resultado = perguntaService.buscarPorId(5L);

        assertNotNull(resultado);
        assertEquals(5L, resultado.codigo());
        assertEquals("Enunciado", resultado.pergunta());
        assertEquals(1, resultado.competenciaCodigo());
        assertEquals("Liderança", resultado.competenciaNome());
    }

    @Test
    void buscarPorId_lancaEntityNotFound_quandoNaoEncontrado() {
        when(perguntaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> perguntaService.buscarPorId(99L));
    }

    // -------------------------------------------------------------------------
    // deletarPergunta
    // -------------------------------------------------------------------------

    @Test
    void deletarPergunta_deletaComSucesso_quandoExiste() {
        when(perguntaRepository.existsById(7L)).thenReturn(true);

        assertDoesNotThrow(() -> perguntaService.deletarPergunta(7L));

        verify(perguntaRepository).deleteById(7L);
    }

    @Test
    void deletarPergunta_lancaEntityNotFound_quandoNaoExiste() {
        when(perguntaRepository.existsById(99L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> perguntaService.deletarPergunta(99L));
        verify(perguntaRepository, never()).deleteById(any());
    }
}
