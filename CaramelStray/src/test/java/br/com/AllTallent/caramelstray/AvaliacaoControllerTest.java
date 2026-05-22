package br.com.AllTallent.caramelstray;

import br.com.AllTallent.config.JwtService;
import br.com.AllTallent.controller.AvaliacaoController;
import br.com.AllTallent.dto.*;
import br.com.AllTallent.exception.ResourceNotFoundException;
import br.com.AllTallent.service.AvaliacaoService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AvaliacaoController.class)
class AvaliacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvaliacaoService avaliacaoService;

    @MockitoBean
    private JwtService jwtService;

    // -------------------------------------------------------------------------
    // POST /api/avaliacoes
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void criarAvaliacao_deveRetornar201QuandoCriada() throws Exception {
        AvaliacaoResponseDTO resposta = new AvaliacaoResponseDTO(
                1, "Avaliação Anual", "ABERTA", LocalDate.now(), LocalDate.now().plusDays(30), "Sistema"
        );
        when(avaliacaoService.criarAvaliacaoCompleta(any(AvaliacaoRequestDTO.class))).thenReturn(resposta);

        String body = """
                {
                    "titulo": "Avaliação Anual",
                    "dataPrazo": "2026-06-30",
                    "codigosFuncionarios": [1, 2],
                    "codigosPerguntas": [10, 11]
                }
                """;

        mockMvc.perform(post("/api/avaliacoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.titulo").value("Avaliação Anual"))
                .andExpect(jsonPath("$.status").value("ABERTA"));
    }

    @Test
    @WithMockUser(roles = "GESTOR")
    void criarAvaliacao_deveRetornar400QuandoEntidadeNaoEncontrada() throws Exception {
        when(avaliacaoService.criarAvaliacaoCompleta(any(AvaliacaoRequestDTO.class)))
                .thenThrow(new EntityNotFoundException("Funcionário não encontrado"));

        String body = """
                {
                    "titulo": "Avaliação Anual",
                    "dataPrazo": "2026-06-30",
                    "codigosFuncionarios": [999],
                    "codigosPerguntas": [10]
                }
                """;

        mockMvc.perform(post("/api/avaliacoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void criarAvaliacao_deveRetornar500QuandoErroInesperado() throws Exception {
        when(avaliacaoService.criarAvaliacaoCompleta(any(AvaliacaoRequestDTO.class)))
                .thenThrow(new RuntimeException("Erro interno"));

        String body = """
                {
                    "titulo": "Avaliação Anual",
                    "dataPrazo": "2026-06-30",
                    "codigosFuncionarios": [1],
                    "codigosPerguntas": [10]
                }
                """;

        mockMvc.perform(post("/api/avaliacoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "USER")
    void criarAvaliacao_deveRetornar403QuandoSemPermissao() throws Exception {
        String body = """
                {
                    "titulo": "Avaliação Anual",
                    "dataPrazo": "2026-06-30",
                    "codigosFuncionarios": [1],
                    "codigosPerguntas": [10]
                }
                """;

        mockMvc.perform(post("/api/avaliacoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        verify(avaliacaoService, never()).criarAvaliacaoCompleta(any());
    }

    // -------------------------------------------------------------------------
    // GET /api/avaliacoes
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void listarTodasAvaliacoes_deveRetornar200ComLista() throws Exception {
        List<AvaliacaoResponseDTO> lista = List.of(
                new AvaliacaoResponseDTO(1, "Avaliação A", "ABERTA", LocalDate.now(), LocalDate.now().plusDays(10), "Sistema"),
                new AvaliacaoResponseDTO(2, "Avaliação B", "FECHADA", LocalDate.now(), LocalDate.now().plusDays(5), "Admin")
        );
        when(avaliacaoService.listarTodasAvaliacoes()).thenReturn(lista);

        mockMvc.perform(get("/api/avaliacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].titulo").value("Avaliação A"))
                .andExpect(jsonPath("$[1].titulo").value("Avaliação B"));
    }

    @Test
    @WithMockUser(roles = "GESTOR")
    void listarTodasAvaliacoes_deveRetornar200ComListaVazia() throws Exception {
        when(avaliacaoService.listarTodasAvaliacoes()).thenReturn(List.of());

        mockMvc.perform(get("/api/avaliacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "USER")
    void listarTodasAvaliacoes_deveRetornar403QuandoSemPermissao() throws Exception {
        mockMvc.perform(get("/api/avaliacoes"))
                .andExpect(status().isForbidden());

        verify(avaliacaoService, never()).listarTodasAvaliacoes();
    }

    // -------------------------------------------------------------------------
    // GET /api/avaliacoes/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void buscarAvaliacaoDetalhada_deveRetornar200QuandoEncontrada() throws Exception {
        AvaliacaoDetalhadaDTO dto = new AvaliacaoDetalhadaDTO(
                1, "Avaliação Anual", "ABERTA", LocalDate.now(), LocalDate.now().plusDays(30),
                "Sistema", List.of(), List.of()
        );
        when(avaliacaoService.buscarAvaliacaoDetalhada(1)).thenReturn(dto);

        mockMvc.perform(get("/api/avaliacoes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.titulo").value("Avaliação Anual"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void buscarAvaliacaoDetalhada_deveRetornar404QuandoNaoEncontrada() throws Exception {
        when(avaliacaoService.buscarAvaliacaoDetalhada(99))
                .thenThrow(new ResourceNotFoundException("Avaliação não encontrada"));

        mockMvc.perform(get("/api/avaliacoes/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // GET /api/avaliacoes/{id}/instancias
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void buscarInstanciasPorAvaliacao_deveRetornar200ComLista() throws Exception {
        when(avaliacaoService.buscarInstanciasPorAvaliacao(1)).thenReturn(List.of());

        mockMvc.perform(get("/api/avaliacoes/1/instancias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void buscarInstanciasPorAvaliacao_deveRetornar404QuandoAvaliacaoNaoExiste() throws Exception {
        when(avaliacaoService.buscarInstanciasPorAvaliacao(99))
                .thenThrow(new EntityNotFoundException("Avaliação não encontrada"));

        mockMvc.perform(get("/api/avaliacoes/99/instancias"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/avaliacoes/respostas
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser
    void salvarResposta_deveRetornar200QuandoSalva() throws Exception {
        RespostaColaboradorResponseDTO resposta = new RespostaColaboradorResponseDTO(
                1L, 10L, 5L, "Resposta texto", null
        );
        when(avaliacaoService.salvarOuAtualizarResposta(any(RespostaColaboradorRequestDTO.class))).thenReturn(resposta);

        String body = """
                {
                    "funcionarioAvaliacaoCodigo": 10,
                    "perguntaCodigo": 5,
                    "respostaTexto": "Resposta texto"
                }
                """;

        mockMvc.perform(post("/api/avaliacoes/respostas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.respostaTexto").value("Resposta texto"));
    }

    @Test
    @WithMockUser
    void salvarResposta_deveRetornar400QuandoEntidadeNaoEncontrada() throws Exception {
        when(avaliacaoService.salvarOuAtualizarResposta(any(RespostaColaboradorRequestDTO.class)))
                .thenThrow(new EntityNotFoundException("Instância não encontrada"));

        String body = """
                {
                    "funcionarioAvaliacaoCodigo": 999,
                    "perguntaCodigo": 5,
                    "respostaTexto": "Texto"
                }
                """;

        mockMvc.perform(post("/api/avaliacoes/respostas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void salvarResposta_deveRetornar400QuandoArgumentoInvalido() throws Exception {
        when(avaliacaoService.salvarOuAtualizarResposta(any(RespostaColaboradorRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Resposta inválida"));

        String body = """
                {
                    "funcionarioAvaliacaoCodigo": 10,
                    "perguntaCodigo": 5,
                    "respostaTexto": "Texto"
                }
                """;

        mockMvc.perform(post("/api/avaliacoes/respostas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void salvarResposta_deveRetornar500QuandoErroInesperado() throws Exception {
        when(avaliacaoService.salvarOuAtualizarResposta(any(RespostaColaboradorRequestDTO.class)))
                .thenThrow(new RuntimeException("Erro interno"));

        String body = """
                {
                    "funcionarioAvaliacaoCodigo": 10,
                    "perguntaCodigo": 5,
                    "respostaTexto": "Texto"
                }
                """;

        mockMvc.perform(post("/api/avaliacoes/respostas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError());
    }

    // -------------------------------------------------------------------------
    // GET /api/avaliacoes/instancias/{instanciaId}/respostas
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void buscarRespostasPorInstancia_deveRetornar200ComLista() throws Exception {
        List<RespostaColaboradorResponseDTO> respostas = List.of(
                new RespostaColaboradorResponseDTO(1L, 10L, 5L, "Resposta A", null)
        );
        when(avaliacaoService.buscarRespostasPorInstancia(10L)).thenReturn(respostas);

        mockMvc.perform(get("/api/avaliacoes/instancias/10/respostas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].respostaTexto").value("Resposta A"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void buscarRespostasPorInstancia_deveRetornar404QuandoInstanciaNaoExiste() throws Exception {
        when(avaliacaoService.buscarRespostasPorInstancia(99L))
                .thenThrow(new EntityNotFoundException("Instância não encontrada"));

        mockMvc.perform(get("/api/avaliacoes/instancias/99/respostas"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // GET /api/avaliacoes/revisao/{codigoAvaliacaoFuncionario}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDadosParaRevisao_deveRetornar200ComDados() throws Exception {
        List<RevisaoDetalhadaDTO> revisao = List.of(
                RevisaoDetalhadaDTO.builder().perguntaTexto("Pergunta 1").respostaDada("Sim").build()
        );
        when(avaliacaoService.buscarDadosRevisao(10L)).thenReturn(revisao);

        mockMvc.perform(get("/api/avaliacoes/revisao/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].perguntaTexto").value("Pergunta 1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDadosParaRevisao_deveRetornar404QuandoNaoEncontrado() throws Exception {
        when(avaliacaoService.buscarDadosRevisao(99L))
                .thenThrow(new EntityNotFoundException("Instância não encontrada"));

        mockMvc.perform(get("/api/avaliacoes/revisao/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // PUT /api/avaliacoes/instancias/{instanciaId}/revisar
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "GESTOR")
    void salvarRevisaoSupervisor_deveRetornar200QuandoRevisado() throws Exception {
        when(avaliacaoService.salvarRevisaoSupervisor(eq(1L), any(RevisaoSupervisorRequestDTO.class)))
                .thenReturn(null);

        String body = """
                {
                    "comentarioSupervisao": "Bom desempenho",
                    "comentarioParaColaborador": "Continue assim",
                    "resultadoStatus": "APROVADO"
                }
                """;

        mockMvc.perform(put("/api/avaliacoes/instancias/1/revisar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void salvarRevisaoSupervisor_deveRetornar404QuandoInstanciaNaoExiste() throws Exception {
        when(avaliacaoService.salvarRevisaoSupervisor(eq(99L), any(RevisaoSupervisorRequestDTO.class)))
                .thenThrow(new EntityNotFoundException("Instância não encontrada"));

        String body = """
                {
                    "comentarioSupervisao": "Bom desempenho",
                    "comentarioParaColaborador": "Continue assim",
                    "resultadoStatus": "APROVADO"
                }
                """;

        mockMvc.perform(put("/api/avaliacoes/instancias/99/revisar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void salvarRevisaoSupervisor_deveRetornar500QuandoErroInesperado() throws Exception {
        when(avaliacaoService.salvarRevisaoSupervisor(eq(1L), any(RevisaoSupervisorRequestDTO.class)))
                .thenThrow(new RuntimeException("Erro interno"));

        String body = """
                {
                    "comentarioSupervisao": "Bom desempenho",
                    "comentarioParaColaborador": "Continue assim",
                    "resultadoStatus": "APROVADO"
                }
                """;

        mockMvc.perform(put("/api/avaliacoes/instancias/1/revisar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError());
    }

    // -------------------------------------------------------------------------
    // GET /api/avaliacoes/pendentes/{funcionarioId}
    // -------------------------------------------------------------------------

    @Test
    @WithCustomUser(codigo = 5)
    void buscarAvaliacoesPendentes_deveRetornar200QuandoPrincipalCoinicde() throws Exception {
        when(avaliacaoService.buscarPendentesPorFuncionario(5)).thenReturn(List.of());

        mockMvc.perform(get("/api/avaliacoes/pendentes/5"))
                .andExpect(status().isOk());
    }

    @Test
    @WithCustomUser(codigo = 5)
    void buscarAvaliacoesPendentes_deveRetornar403QuandoPrincipalNaoCoinicde() throws Exception {
        mockMvc.perform(get("/api/avaliacoes/pendentes/99"))
                .andExpect(status().isForbidden());

        verify(avaliacaoService, never()).buscarPendentesPorFuncionario(any());
    }

    // -------------------------------------------------------------------------
    // GET /api/avaliacoes/instancias/{instanciaId}/responder
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser
    void buscarAvaliacaoParaResponder_deveRetornar200QuandoEncontrada() throws Exception {
        AvaliacaoParaResponderDTO dto = new AvaliacaoParaResponderDTO(
                1L, "Avaliação Anual", LocalDate.now().plusDays(10), List.of()
        );
        when(avaliacaoService.buscarParaResponder(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/avaliacoes/instancias/1/responder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tituloAvaliacao").value("Avaliação Anual"));
    }

    @Test
    @WithMockUser
    void buscarAvaliacaoParaResponder_deveRetornar404QuandoNaoEncontrada() throws Exception {
        when(avaliacaoService.buscarParaResponder(99L))
                .thenThrow(new EntityNotFoundException("Instância não encontrada"));

        mockMvc.perform(get("/api/avaliacoes/instancias/99/responder"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // PUT /api/avaliacoes/instancias/{instanciaId}/finalizar
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser
    void finalizarAvaliacaoColaborador_deveRetornar204QuandoFinalizada() throws Exception {
        doNothing().when(avaliacaoService).finalizarPeloColaborador(1L);

        mockMvc.perform(put("/api/avaliacoes/instancias/1/finalizar").with(csrf()))
                .andExpect(status().isNoContent());

        verify(avaliacaoService).finalizarPeloColaborador(1L);
    }

    @Test
    @WithMockUser
    void finalizarAvaliacaoColaborador_deveRetornar404QuandoInstanciaNaoExiste() throws Exception {
        doThrow(new EntityNotFoundException("Instância não encontrada"))
                .when(avaliacaoService).finalizarPeloColaborador(99L);

        mockMvc.perform(put("/api/avaliacoes/instancias/99/finalizar").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void finalizarAvaliacaoColaborador_deveRetornar409QuandoJaFinalizada() throws Exception {
        doThrow(new IllegalStateException("Avaliação já finalizada"))
                .when(avaliacaoService).finalizarPeloColaborador(1L);

        mockMvc.perform(put("/api/avaliacoes/instancias/1/finalizar").with(csrf()))
                .andExpect(status().isConflict());
    }
}