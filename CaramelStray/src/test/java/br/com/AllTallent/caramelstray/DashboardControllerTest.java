package br.com.AllTallent.caramelstray;

import br.com.AllTallent.config.JwtService;
import br.com.AllTallent.controller.DashboardController;
import br.com.AllTallent.dto.*;
import br.com.AllTallent.model.Area;
import br.com.AllTallent.model.Funcionario;
import br.com.AllTallent.repository.FuncionarioRepository;
import br.com.AllTallent.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private DashboardService dashboardService;
    @MockitoBean private FuncionarioRepository funcionarioRepository;
    @MockitoBean private JwtService jwtService;

    // Helper: monta um DashboardResponseDTO mínimo para evitar repetição
    private DashboardResponseDTO responseVazio() {
        return DashboardResponseDTO.builder()
                .totalColaboradores(0L)
                .avaliacoesConcluidasMes(0)
                .metaMensal(0.0)
                .totalPendencias(0)
                .evolucaoMensal(List.of())
                .totalColaboradoresArea(List.of())
                .totalColaboradoresCompetencia(List.of())
                .top5CompetenciasMaisAvaliadas(List.of())
                .build();
    }

    // -------------------------------------------------------------------------
    // Sem autenticação → 403
    // -------------------------------------------------------------------------

    @Test
    void getDashboardData_deveRetornar401_quandoNaoAutenticado() throws Exception {
        // Sem credenciais → Spring Security responde 401 (AuthenticationEntryPoint).
        // 403 só ocorreria se o usuário estivesse autenticado mas sem permissão.
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());

        verify(dashboardService, never()).getDashboardData(any());
    }

    // -------------------------------------------------------------------------
    // ADMIN (perfilCodigo=1) — vê tudo, sem filtro forçado
    // -------------------------------------------------------------------------

    @Test
    @WithCustomUser(codigo = 1, perfilCodigo = 1)
    void getDashboardData_deveRetornar200_quandoAdminSemFiltroDeArea() throws Exception {
        when(dashboardService.getDashboardData(null)).thenReturn(responseVazio());

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalColaboradores").value(0));

        verify(dashboardService).getDashboardData(null);
    }

    @Test
    @WithCustomUser(codigo = 1, perfilCodigo = 1)
    void getDashboardData_deveRepassarFiltroDeArea_quandoAdminPassaCodigoArea() throws Exception {
        when(dashboardService.getDashboardData(5)).thenReturn(responseVazio());

        mockMvc.perform(get("/api/dashboard").param("codigoArea", "5"))
                .andExpect(status().isOk());

        verify(dashboardService).getDashboardData(5);
    }

    // -------------------------------------------------------------------------
    // GESTOR (perfilCodigo=2) — filtro forçado para a área do gestor
    // -------------------------------------------------------------------------

    @Test
    @WithCustomUser(codigo = 10, perfilCodigo = 2)
    void getDashboardData_deveForcarFiltroDeArea_quandoGestorTemArea() throws Exception {
        Area area = new Area(3, "Tecnologia", "Área de TI");
        Funcionario gestor = new Funcionario();
        gestor.setCodigo(10);
        gestor.setArea(area);

        when(funcionarioRepository.findById(10)).thenReturn(Optional.of(gestor));
        when(dashboardService.getDashboardData(3)).thenReturn(responseVazio());

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk());

        // O filtro deve ser substituído pelo código da área do gestor
        verify(dashboardService).getDashboardData(3);
        verify(dashboardService, never()).getDashboardData(null);
    }

    @Test
    @WithCustomUser(codigo = 10, perfilCodigo = 2)
    void getDashboardData_naoForcaFiltro_quandoGestorSemArea() throws Exception {
        Funcionario gestorSemArea = new Funcionario();
        gestorSemArea.setCodigo(10);
        gestorSemArea.setArea(null);

        when(funcionarioRepository.findById(10)).thenReturn(Optional.of(gestorSemArea));
        when(dashboardService.getDashboardData(null)).thenReturn(responseVazio());

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk());

        verify(dashboardService).getDashboardData(null);
    }

    @Test
    @WithCustomUser(codigo = 10, perfilCodigo = 2)
    void getDashboardData_deveIgnorarFiltroPassado_quandoGestorTemArea() throws Exception {
        // Mesmo que o gestor passe ?codigoArea=99, o filtro é substituído pela sua área
        Area area = new Area(3, "Tecnologia", "Área de TI");
        Funcionario gestor = new Funcionario();
        gestor.setCodigo(10);
        gestor.setArea(area);

        when(funcionarioRepository.findById(10)).thenReturn(Optional.of(gestor));
        when(dashboardService.getDashboardData(3)).thenReturn(responseVazio());

        mockMvc.perform(get("/api/dashboard").param("codigoArea", "99"))
                .andExpect(status().isOk());

        verify(dashboardService).getDashboardData(3);
        verify(dashboardService, never()).getDashboardData(99);
    }

    // -------------------------------------------------------------------------
    // Gestor não encontrado no banco → 500
    // -------------------------------------------------------------------------

    @Test
    @WithCustomUser(codigo = 99, perfilCodigo = 2)
    void getDashboardData_deveRetornar500_quandoGestorNaoEncontradoNoBanco() throws Exception {
        when(funcionarioRepository.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isInternalServerError());
    }

    // -------------------------------------------------------------------------
    // Erro no service → 500
    // -------------------------------------------------------------------------

    @Test
    @WithCustomUser(codigo = 1, perfilCodigo = 1)
    void getDashboardData_deveRetornar500_quandoServiceLancaExcecao() throws Exception {
        when(dashboardService.getDashboardData(any())).thenThrow(new RuntimeException("Erro ao consultar dados"));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isInternalServerError());
    }

    // -------------------------------------------------------------------------
    // Colaborador (perfilCodigo=0, ROLE_USER) — isAuthenticated() passa
    // -------------------------------------------------------------------------

    @Test
    @WithCustomUser(codigo = 5, perfilCodigo = 0)
    void getDashboardData_deveRetornar200_quandoColaboradorAutenticado() throws Exception {
        // Colaborador tem ROLE_USER: isAuthenticated() = true, isAdmin = false, isGestor = false
        // Portanto, nenhum filtro forçado, filtroAreaId vem do param (null aqui)
        when(dashboardService.getDashboardData(null)).thenReturn(responseVazio());

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk());

        verify(dashboardService).getDashboardData(null);
        verify(funcionarioRepository, never()).findById(any());
    }
}