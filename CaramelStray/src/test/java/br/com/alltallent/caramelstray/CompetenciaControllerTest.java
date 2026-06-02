package br.com.alltallent.caramelstray;

import br.com.alltallent.config.JwtService;
import br.com.alltallent.controller.CompetenciaController;
import br.com.alltallent.model.Competencia;
import br.com.alltallent.repository.CompetenciaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompetenciaController.class)
class CompetenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompetenciaRepository competenciaRepository;

    @MockitoBean
    private JwtService jwtService;

    // --- POST /api/competencia ---

    @Test
    @WithMockUser
    void criar_deveRetornar201ComCompetenciaCriada() throws Exception {
        Competencia salva = new Competencia(1, "Java", "Programação", null);
        when(competenciaRepository.existsByNomeIgnoreCase("Java")).thenReturn(false);
        when(competenciaRepository.save(any(Competencia.class))).thenReturn(salva);

        String body = """
                {
                    "nome": "Java",
                    "categoria": "Programação"
                }
                """;

        mockMvc.perform(post("/api/competencia")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.nome").value("Java"))
                .andExpect(jsonPath("$.categoria").value("Programação"));
    }

    @Test
    @WithMockUser
    void criar_deveRetornar400QuandoNomeJaExiste() throws Exception {
        when(competenciaRepository.existsByNomeIgnoreCase("Java")).thenReturn(true);

        String body = """
                {
                    "nome": "Java",
                    "categoria": "Programação"
                }
                """;

        mockMvc.perform(post("/api/competencia")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(competenciaRepository, never()).save(any());
    }

    @Test
    @WithMockUser
    void criar_naoDeveAceitarCodigoDoCliente() throws Exception {
        Competencia salva = new Competencia(99, "Python", "Programação", null);
        when(competenciaRepository.existsByNomeIgnoreCase("Python")).thenReturn(false);
        when(competenciaRepository.save(any(Competencia.class))).thenReturn(salva);

        String bodyComCodigo = """
                {
                    "codigo": 42,
                    "nome": "Python",
                    "categoria": "Programação"
                }
                """;

        mockMvc.perform(post("/api/competencia")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyComCodigo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(99));
    }

    // --- GET /api/competencia ---

    @Test
    @WithMockUser
    void listar_deveRetornar200ComListaDeCompetencias() throws Exception {
        List<Competencia> competencias = List.of(
                new Competencia(1, "Java", "Programação", null),
                new Competencia(2, "SQL", "Banco de Dados", null)
        );
        when(competenciaRepository.findAll()).thenReturn(competencias);

        mockMvc.perform(get("/api/competencia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Java"))
                .andExpect(jsonPath("$[1].nome").value("SQL"));
    }

    @Test
    @WithMockUser
    void listar_deveRetornar200ComListaVazia() throws Exception {
        when(competenciaRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/competencia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- GET /api/competencia/{id} ---

    @Test
    @WithMockUser
    void buscarPorId_deveRetornar200QuandoEncontrado() throws Exception {
        Competencia competencia = new Competencia(1, "Java", "Programação", null);
        when(competenciaRepository.findById(1)).thenReturn(Optional.of(competencia));

        mockMvc.perform(get("/api/competencia/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.nome").value("Java"));
    }

    @Test
    @WithMockUser
    void buscarPorId_deveRetornar404QuandoNaoEncontrado() throws Exception {
        when(competenciaRepository.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/competencia/99"))
                .andExpect(status().isNotFound());
    }

    // --- PUT /api/competencia/{id} ---

    @Test
    @WithMockUser
    void atualizar_deveRetornar200ComDadosAtualizados() throws Exception {
        Competencia existente = new Competencia(1, "Java", "Programação", null);
        Competencia atualizada = new Competencia(1, "Java Avançado", "Programação", null);
        when(competenciaRepository.findById(1)).thenReturn(Optional.of(existente));
        when(competenciaRepository.save(any(Competencia.class))).thenReturn(atualizada);

        String body = """
                {
                    "nome": "Java Avançado",
                    "categoria": "Programação"
                }
                """;

        mockMvc.perform(put("/api/competencia/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Java Avançado"));
    }

    @Test
    @WithMockUser
    void atualizar_deveRetornar404QuandoNaoEncontrado() throws Exception {
        when(competenciaRepository.findById(99)).thenReturn(Optional.empty());

        String body = """
                {
                    "nome": "Java Avançado",
                    "categoria": "Programação"
                }
                """;

        mockMvc.perform(put("/api/competencia/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/competencia/{id} ---

    @Test
    @WithMockUser
    void deletar_deveRetornar204QuandoDeletado() throws Exception {
        when(competenciaRepository.existsById(1)).thenReturn(true);
        doNothing().when(competenciaRepository).deleteById(1);

        mockMvc.perform(delete("/api/competencia/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(competenciaRepository).deleteById(1);
    }

    @Test
    @WithMockUser
    void deletar_deveRetornar404QuandoNaoEncontrado() throws Exception {
        when(competenciaRepository.existsById(99)).thenReturn(false);

        mockMvc.perform(delete("/api/competencia/99").with(csrf()))
                .andExpect(status().isNotFound());

        verify(competenciaRepository, never()).deleteById(any());
    }
}
