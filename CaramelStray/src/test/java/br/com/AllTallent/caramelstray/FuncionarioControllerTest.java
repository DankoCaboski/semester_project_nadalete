package br.com.AllTallent.caramelstray;

import br.com.AllTallent.config.JwtService;
import br.com.AllTallent.controller.FuncionarioController;
import br.com.AllTallent.dto.*;
import br.com.AllTallent.repository.FuncionarioRepository;
import br.com.AllTallent.service.FuncionarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FuncionarioController.class)
class FuncionarioControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
        /**
         * Fornece um MethodSecurityExpressionHandler com o ApplicationContext injetado,
         * garantindo que referências de bean (@beanName) em expressões @PreAuthorize
         * sejam resolvidas corretamente no contexto de teste.
         */
        @Bean
        @Primary
        MethodSecurityExpressionHandler methodSecurityExpressionHandler(ApplicationContext ctx) {
            DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
            handler.setApplicationContext(ctx);
            return handler;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    // name explícito para que @PreAuthorize("@funcionarioService.xxx()") resolva o bean pelo nome
    @MockitoBean(name = "funcionarioService")
    private FuncionarioService funcionarioService;
    @MockitoBean private FuncionarioRepository funcionarioRepository;
    @MockitoBean private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // --- Helpers ---

    private FuncionarioResponseDTO responseVazio(int codigo) {
        return new FuncionarioResponseDTO(
                codigo, "Funcionário " + codigo, "f@test.com",
                null, null, null, null, null, null, null,
                null, null, null, null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    private FuncionarioRequestDTO requestDTO() {
        return new FuncionarioRequestDTO(
                "João Silva", "joao@test.com", null, null, null,
                null, null, null, null, null, null);
    }

    // =========================================================================
    // GET /api/funcionario — isAuthenticated()
    // =========================================================================

    @Test
    void listarTodos_retorna401_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/api/funcionario"))
                .andExpect(status().isUnauthorized());

        verify(funcionarioService, never()).listarTodos(any());
    }

    @Test
    @WithMockUser
    void listarTodos_retorna200_quandoAutenticado() throws Exception {
        when(funcionarioService.listarTodos(null)).thenReturn(List.of(responseVazio(1)));

        mockMvc.perform(get("/api/funcionario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value(1));

        verify(funcionarioService).listarTodos(null);
    }

    @Test
    @WithMockUser
    void listarTodos_repassaFiltroDeTexto() throws Exception {
        when(funcionarioService.listarTodos("João")).thenReturn(List.of(responseVazio(2)));

        mockMvc.perform(get("/api/funcionario").param("texto", "João"))
                .andExpect(status().isOk());

        verify(funcionarioService).listarTodos("João");
    }

    // =========================================================================
    // GET /api/funcionario/{id} — hasAnyRole(ADMIN, GESTOR) or principal.codigo == id
    // =========================================================================

    @Test
    void buscarPorId_retorna401_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/api/funcionario/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void buscarPorId_retorna200_quandoAdmin() throws Exception {
        when(funcionarioService.buscarPorId(1)).thenReturn(responseVazio(1));

        mockMvc.perform(get("/api/funcionario/1"))
                .andExpect(status().isOk());

        verify(funcionarioService).buscarPorId(1);
    }

    @Test
    @WithMockUser(roles = "GESTOR")
    void buscarPorId_retorna200_quandoGestor() throws Exception {
        when(funcionarioService.buscarPorId(5)).thenReturn(responseVazio(5));

        mockMvc.perform(get("/api/funcionario/5"))
                .andExpect(status().isOk());
    }

    @Test
    @WithCustomUser(codigo = 7, perfilCodigo = 3)
    void buscarPorId_retorna200_quandoProprioFuncionario() throws Exception {
        when(funcionarioService.buscarPorId(7)).thenReturn(responseVazio(7));

        mockMvc.perform(get("/api/funcionario/7"))
                .andExpect(status().isOk());
    }

    @Test
    @WithCustomUser(codigo = 7, perfilCodigo = 3)
    void buscarPorId_retorna403_quandoColaboradorAcessaOutro() throws Exception {
        mockMvc.perform(get("/api/funcionario/99"))
                .andExpect(status().isForbidden());

        verify(funcionarioService, never()).buscarPorId(any());
    }

    // =========================================================================
    // POST /api/funcionario — hasRole('ADMIN')
    // =========================================================================

    @Test
    void criar_retorna401_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(post("/api/funcionario")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void criar_retorna201_quandoAdmin() throws Exception {
        when(funcionarioService.criar(any())).thenReturn(responseVazio(10));

        mockMvc.perform(post("/api/funcionario")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO())))
                .andExpect(status().isCreated());

        verify(funcionarioService).criar(any());
    }

    @Test
    @WithMockUser(roles = "GESTOR")
    void criar_retorna403_quandoGestor() throws Exception {
        mockMvc.perform(post("/api/funcionario")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO())))
                .andExpect(status().isForbidden());

        verify(funcionarioService, never()).criar(any());
    }

    // =========================================================================
    // PUT /api/funcionario/{id} — principal.codigo == id
    // =========================================================================

    @Test
    @WithCustomUser(codigo = 5, perfilCodigo = 3)
    void atualizar_retorna200_quandoProprioFuncionario() throws Exception {
        when(funcionarioService.atualizar(eq(5), any())).thenReturn(responseVazio(5));

        mockMvc.perform(put("/api/funcionario/5")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO())))
                .andExpect(status().isOk());

        verify(funcionarioService).atualizar(eq(5), any());
    }

    @Test
    @WithCustomUser(codigo = 5, perfilCodigo = 3)
    void atualizar_retorna403_quandoOutroFuncionario() throws Exception {
        mockMvc.perform(put("/api/funcionario/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO())))
                .andExpect(status().isForbidden());

        verify(funcionarioService, never()).atualizar(any(), any());
    }

    // =========================================================================
    // DELETE /api/funcionario/{id} — hasRole('ADMIN')
    // =========================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletar_retorna204_quandoAdmin() throws Exception {
        doNothing().when(funcionarioService).deletar(1);

        mockMvc.perform(delete("/api/funcionario/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(funcionarioService).deletar(1);
    }

    @Test
    @WithMockUser(roles = "USER")
    void deletar_retorna403_quandoUser() throws Exception {
        mockMvc.perform(delete("/api/funcionario/1").with(csrf()))
                .andExpect(status().isForbidden());

        verify(funcionarioService, never()).deletar(any());
    }

    // =========================================================================
    // GET /api/funcionario/{id}/perfil — hasAnyRole(ADMIN, GESTOR) or principal.codigo == id
    // =========================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    void buscarPerfil_retorna200_quandoAdmin() throws Exception {
        FuncionarioPerfilDTO dto = new FuncionarioPerfilDTO(
                1, "Func", "f@t.com", null, null, null, null, null, null, null,
                Collections.emptyList(), Collections.emptyList());

        when(funcionarioService.buscarPerfilPorId(1)).thenReturn(dto);

        mockMvc.perform(get("/api/funcionario/1/perfil"))
                .andExpect(status().isOk());
    }

    @Test
    @WithCustomUser(codigo = 3, perfilCodigo = 3)
    void buscarPerfil_retorna200_quandoProprioFuncionario() throws Exception {
        FuncionarioPerfilDTO dto = new FuncionarioPerfilDTO(
                3, "Func", "f@t.com", null, null, null, null, null, null, null,
                Collections.emptyList(), Collections.emptyList());

        when(funcionarioService.buscarPerfilPorId(3)).thenReturn(dto);

        mockMvc.perform(get("/api/funcionario/3/perfil"))
                .andExpect(status().isOk());
    }

    @Test
    @WithCustomUser(codigo = 3, perfilCodigo = 3)
    void buscarPerfil_retorna403_quandoColaboradorAcessaOutro() throws Exception {
        mockMvc.perform(get("/api/funcionario/99/perfil"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // POST /api/funcionario/{id}/certificados — hasRole('ADMIN') or principal.codigo == id
    // =========================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    void adicionarCertificado_retorna201_quandoAdmin() throws Exception {
        CertificadoDTO certDTO = new CertificadoDTO(1, "AWS");
        when(funcionarioService.adicionarCertificado(eq(1), any())).thenReturn(certDTO);

        mockMvc.perform(post("/api/funcionario/1/certificados")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CertificadoRequestDTO("AWS"))))
                .andExpect(status().isCreated());
    }

    @Test
    @WithCustomUser(codigo = 5, perfilCodigo = 3)
    void adicionarCertificado_retorna201_quandoProprioFuncionario() throws Exception {
        CertificadoDTO certDTO = new CertificadoDTO(2, "GCP");
        when(funcionarioService.adicionarCertificado(eq(5), any())).thenReturn(certDTO);

        mockMvc.perform(post("/api/funcionario/5/certificados")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CertificadoRequestDTO("GCP"))))
                .andExpect(status().isCreated());
    }

    @Test
    @WithCustomUser(codigo = 5, perfilCodigo = 3)
    void adicionarCertificado_retorna403_quandoColaboradorAdicionaEmOutro() throws Exception {
        mockMvc.perform(post("/api/funcionario/99/certificados")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CertificadoRequestDTO("X"))))
                .andExpect(status().isForbidden());

        verify(funcionarioService, never()).adicionarCertificado(any(), any());
    }

    // =========================================================================
    // DELETE /api/funcionario/certificados/{id}
    //   — hasRole('ADMIN') or @funcionarioService.usuarioPodeRemoverCertificado(...)
    // =========================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    void removerCertificado_retorna204_quandoAdmin() throws Exception {
        doNothing().when(funcionarioService).removerCertificado(10);

        mockMvc.perform(delete("/api/funcionario/certificados/10").with(csrf()))
                .andExpect(status().isNoContent());

        verify(funcionarioService).removerCertificado(10);
    }

    @Test
    @WithCustomUser(codigo = 5, perfilCodigo = 3)
    void removerCertificado_retorna204_quandoDono() throws Exception {
        // Bean method retorna true → usuário pode remover
        when(funcionarioService.usuarioPodeRemoverCertificado(10, 5)).thenReturn(true);
        doNothing().when(funcionarioService).removerCertificado(10);

        mockMvc.perform(delete("/api/funcionario/certificados/10").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithCustomUser(codigo = 5, perfilCodigo = 3)
    void removerCertificado_retorna403_quandoNaoDono() throws Exception {
        // Bean method retorna false → acesso negado
        when(funcionarioService.usuarioPodeRemoverCertificado(10, 5)).thenReturn(false);

        mockMvc.perform(delete("/api/funcionario/certificados/10").with(csrf()))
                .andExpect(status().isForbidden());

        verify(funcionarioService, never()).removerCertificado(any());
    }

    // =========================================================================
    // PUT /api/funcionario/{id}/competencias — isAuthenticated()
    // =========================================================================

    @Test
    void atualizarCompetencias_retorna401_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(put("/api/funcionario/1/competencias")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FuncionarioCompetenciaUpdateDTO(List.of(1, 2)))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void atualizarCompetencias_retorna204_quandoAutenticado() throws Exception {
        doNothing().when(funcionarioService).associarCompetencias(eq(1), any());

        mockMvc.perform(put("/api/funcionario/1/competencias")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FuncionarioCompetenciaUpdateDTO(List.of(1, 2)))))
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // GET /api/funcionario/{id}/experiencias — hasAnyRole(ADMIN, GESTOR) or principal.codigo == id
    // =========================================================================

    @Test
    @WithMockUser(roles = "GESTOR")
    void listarExperiencias_retorna200_quandoGestor() throws Exception {
        FuncionarioExperienciasResponseDTO dto =
                new FuncionarioExperienciasResponseDTO(1, "Func", Collections.emptyList());

        when(funcionarioService.listarExperienciasPorFuncionario(1)).thenReturn(dto);

        mockMvc.perform(get("/api/funcionario/1/experiencias"))
                .andExpect(status().isOk());
    }

    @Test
    @WithCustomUser(codigo = 3, perfilCodigo = 3)
    void listarExperiencias_retorna403_quandoColaboradorAcessaOutro() throws Exception {
        mockMvc.perform(get("/api/funcionario/99/experiencias"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // POST /api/funcionario/{id}/experiencias — hasRole('ADMIN') or principal.codigo == id
    // =========================================================================

    @Test
    @WithCustomUser(codigo = 5, perfilCodigo = 3)
    void adicionarExperiencia_retorna201_quandoProprioFuncionario() throws Exception {
        ExperienciaRequestDTO requestDTO = new ExperienciaRequestDTO(
                "Dev", "Empresa", LocalDate.of(2022, 1, 1), null, null);
        ExperienciaDTO resposta = new ExperienciaDTO(
                1, "Dev", "Empresa", null, LocalDate.of(2022, 1, 1), null);

        when(funcionarioService.adicionarExperiencia(eq(5), any())).thenReturn(resposta);

        mockMvc.perform(post("/api/funcionario/5/experiencias")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithCustomUser(codigo = 5, perfilCodigo = 3)
    void adicionarExperiencia_retorna403_quandoColaboradorAdicionaEmOutro() throws Exception {
        ExperienciaRequestDTO requestDTO = new ExperienciaRequestDTO(
                "Dev", "Empresa", LocalDate.of(2022, 1, 1), null, null);

        mockMvc.perform(post("/api/funcionario/99/experiencias")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // PUT /api/funcionario/experiencias/{id}
    //   — hasRole('ADMIN') or @funcionarioService.usuarioPodeEditarExperiencia(...)
    // =========================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    void atualizarExperiencia_retorna200_quandoAdmin() throws Exception {
        ExperienciaRequestDTO requestDTO = new ExperienciaRequestDTO(
                "Sênior", "Corp", LocalDate.of(2020, 3, 1), null, null);
        ExperienciaDTO resposta = new ExperienciaDTO(
                1, "Sênior", "Corp", null, LocalDate.of(2020, 3, 1), null);

        when(funcionarioService.atualizarExperiencia(eq(1), any())).thenReturn(resposta);

        mockMvc.perform(put("/api/funcionario/experiencias/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @WithCustomUser(codigo = 5, perfilCodigo = 3)
    void atualizarExperiencia_retorna200_quandoDono() throws Exception {
        // Bean method retorna true → usuário pode editar
        when(funcionarioService.usuarioPodeEditarExperiencia(10, 5)).thenReturn(true);

        ExperienciaRequestDTO requestDTO = new ExperienciaRequestDTO(
                "Júnior", "Corp", LocalDate.of(2021, 1, 1), null, null);
        ExperienciaDTO resposta = new ExperienciaDTO(
                10, "Júnior", "Corp", null, LocalDate.of(2021, 1, 1), null);

        when(funcionarioService.atualizarExperiencia(eq(10), any())).thenReturn(resposta);

        mockMvc.perform(put("/api/funcionario/experiencias/10")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @WithCustomUser(codigo = 5, perfilCodigo = 3)
    void atualizarExperiencia_retorna403_quandoNaoDono() throws Exception {
        // Bean method retorna false → acesso negado
        when(funcionarioService.usuarioPodeEditarExperiencia(10, 5)).thenReturn(false);

        ExperienciaRequestDTO requestDTO = new ExperienciaRequestDTO(
                "Pleno", "Corp", LocalDate.of(2021, 1, 1), null, null);

        mockMvc.perform(put("/api/funcionario/experiencias/10")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());

        verify(funcionarioService, never()).atualizarExperiencia(any(), any());
    }
}