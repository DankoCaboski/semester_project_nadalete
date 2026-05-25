package br.com.AllTallent.caramelstray;

import br.com.AllTallent.dto.*;
import br.com.AllTallent.model.Area;
import br.com.AllTallent.model.Avaliacao;
import br.com.AllTallent.model.AvaliacaoFuncionario;
import br.com.AllTallent.model.Competencia;
import br.com.AllTallent.model.Funcionario;
import br.com.AllTallent.repository.AvaliacaoFuncionarioRepository;
import br.com.AllTallent.repository.AvaliacaoRepository;
import br.com.AllTallent.repository.FuncionarioRepository;
import br.com.AllTallent.repository.RespostaColaboradorRepository;
import br.com.AllTallent.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private FuncionarioRepository funcionarioRepo;
    @Mock private AvaliacaoRepository avaliacaoRepo;
    @Mock private AvaliacaoFuncionarioRepository avaliacaoFuncionarioRepo;
    @Mock private RespostaColaboradorRepository respostaColaboradorRepo;

    @InjectMocks private DashboardService dashboardService;

    // Helper: cria um MesQuantidadeProjection via classe anônima.
    // Evita Mockito.mock() inline, que com STRICT_STUBS pode lançar
    // UnnecessaryStubbingException se o MockitoSession considerar os
    // stubs não utilizados durante a verificação pós-teste.
    private MesQuantidadeProjection projecao(String mes, long qtd) {
        return new MesQuantidadeProjection() {
            @Override public String getMes()      { return mes; }
            @Override public Long   getQuantidade() { return qtd; }
        };
    }

    // -------------------------------------------------------------------------
    // getDashboardData — sem filtro de área
    // -------------------------------------------------------------------------

    @Test
    void getDashboardData_semFiltro_retornaTodosOsDados() {
        when(funcionarioRepo.count()).thenReturn(10L);
        when(avaliacaoFuncionarioRepo.countTotalPendentes()).thenReturn(3);
        when(avaliacaoFuncionarioRepo.countConcluidasNoMes(any(LocalDate.class), any(LocalDate.class))).thenReturn(5);
        when(avaliacaoFuncionarioRepo.countAprovadasNoMes(any(LocalDate.class), any(LocalDate.class))).thenReturn(4);
        when(funcionarioRepo.findEvolucaoMensal()).thenReturn(List.of(projecao("Janeiro", 2L)));
        when(funcionarioRepo.countFuncionariosPorArea()).thenReturn(List.of(new AreaQuantidadeDTO("TI", 5L)));
        when(funcionarioRepo.countFuncionariosPorCompetencia()).thenReturn(List.of());
        when(avaliacaoFuncionarioRepo.findTopCompetenciasMaisAvaliadas(any(Pageable.class))).thenReturn(List.of());

        DashboardResponseDTO result = dashboardService.getDashboardData(null);

        assertNotNull(result);
        assertEquals(10L, result.getTotalColaboradores());
        assertEquals(3, result.getTotalPendencias());
        assertEquals(5, result.getAvaliacoesConcluidasMes());
        // meta = 4/5 * 100 = 80.0
        assertEquals(80.0, result.getMetaMensal());
        assertEquals(1, result.getEvolucaoMensal().size());
        assertEquals("Janeiro", result.getEvolucaoMensal().get(0).getMes());
        assertEquals(1, result.getTotalColaboradoresArea().size());
    }

    @Test
    void getDashboardData_semFiltro_chamaSomenteQueriesSemArea() {
        when(funcionarioRepo.count()).thenReturn(5L);
        when(avaliacaoFuncionarioRepo.countTotalPendentes()).thenReturn(0);
        when(avaliacaoFuncionarioRepo.countConcluidasNoMes(any(), any())).thenReturn(0);
        when(avaliacaoFuncionarioRepo.countAprovadasNoMes(any(), any())).thenReturn(0);
        when(funcionarioRepo.findEvolucaoMensal()).thenReturn(List.of());
        when(funcionarioRepo.countFuncionariosPorArea()).thenReturn(List.of());
        when(funcionarioRepo.countFuncionariosPorCompetencia()).thenReturn(List.of());
        when(avaliacaoFuncionarioRepo.findTopCompetenciasMaisAvaliadas(any())).thenReturn(List.of());

        dashboardService.getDashboardData(null);

        verify(funcionarioRepo).count();
        verify(funcionarioRepo, never()).countByAreaCodigo(any());
        verify(avaliacaoFuncionarioRepo).countTotalPendentes();
        verify(avaliacaoFuncionarioRepo, never()).countTotalPendentesByArea(any());
    }

    // -------------------------------------------------------------------------
    // getDashboardData — com filtro de área
    // -------------------------------------------------------------------------

    @Test
    void getDashboardData_comFiltroArea_retornaDadosFiltrados() {
        when(funcionarioRepo.countByAreaCodigo(2)).thenReturn(5L);
        when(avaliacaoFuncionarioRepo.countTotalPendentesByArea(2)).thenReturn(1);
        when(avaliacaoFuncionarioRepo.countConcluidasNoMesByArea(any(), any(), eq(2))).thenReturn(3);
        when(avaliacaoFuncionarioRepo.countAprovadasNoMesByArea(any(), any(), eq(2))).thenReturn(3);
        when(funcionarioRepo.findEvolucaoMensalByArea(2)).thenReturn(List.of());
        when(funcionarioRepo.countFuncionariosPorArea()).thenReturn(List.of());
        when(funcionarioRepo.countFuncionariosPorCompetencia()).thenReturn(List.of());
        when(avaliacaoFuncionarioRepo.findTopCompetenciasMaisAvaliadas(any())).thenReturn(List.of());

        DashboardResponseDTO result = dashboardService.getDashboardData(2);

        assertEquals(5L, result.getTotalColaboradores());
        assertEquals(1, result.getTotalPendencias());
        // meta = 3/3 * 100 = 100.0
        assertEquals(100.0, result.getMetaMensal());
    }

    @Test
    void getDashboardData_comFiltroArea_chamaSomenteQueriesComArea() {
        when(funcionarioRepo.countByAreaCodigo(2)).thenReturn(5L);
        when(avaliacaoFuncionarioRepo.countTotalPendentesByArea(2)).thenReturn(0);
        when(avaliacaoFuncionarioRepo.countConcluidasNoMesByArea(any(), any(), eq(2))).thenReturn(0);
        when(avaliacaoFuncionarioRepo.countAprovadasNoMesByArea(any(), any(), eq(2))).thenReturn(0);
        when(funcionarioRepo.findEvolucaoMensalByArea(2)).thenReturn(List.of());
        when(funcionarioRepo.countFuncionariosPorArea()).thenReturn(List.of());
        when(funcionarioRepo.countFuncionariosPorCompetencia()).thenReturn(List.of());
        when(avaliacaoFuncionarioRepo.findTopCompetenciasMaisAvaliadas(any())).thenReturn(List.of());

        dashboardService.getDashboardData(2);

        verify(funcionarioRepo).countByAreaCodigo(2);
        verify(funcionarioRepo, never()).count();
        verify(avaliacaoFuncionarioRepo).countTotalPendentesByArea(2);
        verify(avaliacaoFuncionarioRepo, never()).countTotalPendentes();
    }

    // -------------------------------------------------------------------------
    // getDashboardData — cálculo de metaMensal
    // -------------------------------------------------------------------------

    @Test
    void getDashboardData_metaMensalZero_quandoZeroConcluidosNoMes() {
        when(funcionarioRepo.count()).thenReturn(5L);
        when(avaliacaoFuncionarioRepo.countTotalPendentes()).thenReturn(0);
        when(avaliacaoFuncionarioRepo.countConcluidasNoMes(any(), any())).thenReturn(0);
        when(avaliacaoFuncionarioRepo.countAprovadasNoMes(any(), any())).thenReturn(0);
        when(funcionarioRepo.findEvolucaoMensal()).thenReturn(List.of());
        when(funcionarioRepo.countFuncionariosPorArea()).thenReturn(List.of());
        when(funcionarioRepo.countFuncionariosPorCompetencia()).thenReturn(List.of());
        when(avaliacaoFuncionarioRepo.findTopCompetenciasMaisAvaliadas(any())).thenReturn(List.of());

        DashboardResponseDTO result = dashboardService.getDashboardData(null);

        assertEquals(0.0, result.getMetaMensal());
    }

    @Test
    void getDashboardData_metaMensalZero_quandoConcluidosNull() {
        when(funcionarioRepo.count()).thenReturn(5L);
        when(avaliacaoFuncionarioRepo.countTotalPendentes()).thenReturn(0);
        when(avaliacaoFuncionarioRepo.countConcluidasNoMes(any(), any())).thenReturn(null);
        when(avaliacaoFuncionarioRepo.countAprovadasNoMes(any(), any())).thenReturn(null);
        when(funcionarioRepo.findEvolucaoMensal()).thenReturn(List.of());
        when(funcionarioRepo.countFuncionariosPorArea()).thenReturn(List.of());
        when(funcionarioRepo.countFuncionariosPorCompetencia()).thenReturn(List.of());
        when(avaliacaoFuncionarioRepo.findTopCompetenciasMaisAvaliadas(any())).thenReturn(List.of());

        DashboardResponseDTO result = dashboardService.getDashboardData(null);

        assertEquals(0.0, result.getMetaMensal());
    }

    // -------------------------------------------------------------------------
    // Métodos auxiliares de gráficos
    // -------------------------------------------------------------------------

    @Test
    void getTotalColaboradoresArea_delegaParaRepositorio() {
        List<AreaQuantidadeDTO> esperado = List.of(
                new AreaQuantidadeDTO("TI", 10L),
                new AreaQuantidadeDTO("RH", 5L)
        );
        when(funcionarioRepo.countFuncionariosPorArea()).thenReturn(esperado);

        List<AreaQuantidadeDTO> result = dashboardService.getTotalColaboradoresArea();

        assertEquals(esperado, result);
        verify(funcionarioRepo).countFuncionariosPorArea();
    }

    @Test
    void getTotalColaboradoresCompetencia_delegaParaRepositorio() {
        List<CompetenciaQuantidadeDTO> esperado = List.of(new CompetenciaQuantidadeDTO("Java", 8L));
        when(funcionarioRepo.countFuncionariosPorCompetencia()).thenReturn(esperado);

        assertEquals(esperado, dashboardService.getTotalColaboradoresCompetencia());
    }

    @Test
    void getTop5CompetenciasMaisAvaliadas_retornaTop5ComPageRequest() {
        List<CompetenciaQuantidadeDTO> top5 = List.of(
                new CompetenciaQuantidadeDTO("Java", 20L),
                new CompetenciaQuantidadeDTO("SQL", 15L),
                new CompetenciaQuantidadeDTO("Spring", 10L),
                new CompetenciaQuantidadeDTO("Docker", 8L),
                new CompetenciaQuantidadeDTO("Git", 6L)
        );
        when(avaliacaoFuncionarioRepo.findTopCompetenciasMaisAvaliadas(any(Pageable.class))).thenReturn(top5);

        List<CompetenciaQuantidadeDTO> result = dashboardService.getTop5CompetenciasMaisAvaliadas();

        assertEquals(5, result.size());
        assertEquals("Java", result.get(0).getNomeCompetencia());
        assertEquals(20L, result.get(0).getQuantidade());
    }

    // -------------------------------------------------------------------------
    // gerarResumo
    // -------------------------------------------------------------------------

    @Test
    void gerarResumo_retornaMapaComTodosOsCamposEsperados() {
        Funcionario func = new Funcionario();
        func.setCodigo(1);
        func.setNomeCompleto("João Silva");

        Avaliacao avConcluida = mock(Avaliacao.class);
        when(avConcluida.getStatus()).thenReturn("CONCLUIDO");

        Avaliacao avPendente = mock(Avaliacao.class);
        when(avPendente.getStatus()).thenReturn("PENDENTE");

        AvaliacaoFuncionario instanciaPendente = mock(AvaliacaoFuncionario.class);
        when(instanciaPendente.getResultadoStatus()).thenReturn("PENDENTE");
        when(instanciaPendente.getFuncionario()).thenReturn(func);
        when(instanciaPendente.getCodigo()).thenReturn(10L);

        when(funcionarioRepo.findAll()).thenReturn(List.of(func));
        when(avaliacaoRepo.findAll()).thenReturn(List.of(avConcluida, avPendente));
        when(avaliacaoFuncionarioRepo.findAll()).thenReturn(List.of(instanciaPendente));
        when(respostaColaboradorRepo.findByAvaliacaoFuncionarioCodigo(10L)).thenReturn(List.of());

        Map<String, Object> resumo = dashboardService.gerarResumo();

        assertNotNull(resumo);
        assertEquals(5, resumo.size());
        assertEquals(1L, resumo.get("totalColaboradores"));
        assertEquals(1L, resumo.get("avaliacoesConcluidas"));
        assertEquals(1L, resumo.get("avaliacoesPendentes"));
        assertTrue(((List<?>) resumo.get("colaboradoresPendentes")).contains("João Silva"));
        assertTrue(((List<?>) resumo.get("colaboradoresSemEntrega")).contains("João Silva"));
    }

    @Test
    void gerarResumo_colaboradorNaoApareceSemEntrega_quandoTemRespostas() {
        Funcionario func = new Funcionario();
        func.setCodigo(1);
        func.setNomeCompleto("Maria Souza");

        AvaliacaoFuncionario instancia = mock(AvaliacaoFuncionario.class);
        when(instancia.getResultadoStatus()).thenReturn("APROVADO");
        when(instancia.getCodigo()).thenReturn(20L);

        when(funcionarioRepo.findAll()).thenReturn(List.of(func));
        when(avaliacaoRepo.findAll()).thenReturn(List.of());
        when(avaliacaoFuncionarioRepo.findAll()).thenReturn(List.of(instancia));
        when(respostaColaboradorRepo.findByAvaliacaoFuncionarioCodigo(20L))
                .thenReturn(List.of(mock(br.com.AllTallent.model.RespostaColaborador.class)));

        Map<String, Object> resumo = dashboardService.gerarResumo();

        assertTrue(((List<?>) resumo.get("colaboradoresSemEntrega")).isEmpty());
    }

    // -------------------------------------------------------------------------
    // getDistribuicaoPorArea
    // -------------------------------------------------------------------------

    @Test
    void getDistribuicaoPorArea_agrupaPorNomeDaArea() {
        Area ti = new Area(1, "Tecnologia", "Desc");
        Funcionario f1 = new Funcionario(); f1.setArea(ti);
        Funcionario f2 = new Funcionario(); f2.setArea(ti);
        Funcionario f3 = new Funcionario(); f3.setArea(new Area(2, "RH", "Desc"));

        when(funcionarioRepo.findAll()).thenReturn(List.of(f1, f2, f3));

        Map<String, Long> result = dashboardService.getDistribuicaoPorArea();

        assertEquals(2L, result.get("Tecnologia"));
        assertEquals(1L, result.get("RH"));
    }

    @Test
    void getDistribuicaoPorArea_usaChaveSemArea_quandoAreaNula() {
        Funcionario semArea = new Funcionario(); semArea.setArea(null);

        when(funcionarioRepo.findAll()).thenReturn(List.of(semArea));

        Map<String, Long> result = dashboardService.getDistribuicaoPorArea();

        assertEquals(1L, result.get("Sem área"));
    }

    // -------------------------------------------------------------------------
    // getDistribuicaoPorCompetencias
    // -------------------------------------------------------------------------

    @Test
    void getDistribuicaoPorCompetencias_agrupaPorNomeDaCompetencia() {
        Competencia java = new Competencia(); java.setCodigo(1); java.setNome("Java");
        Competencia sql = new Competencia();  sql.setCodigo(2);  sql.setNome("SQL");

        Funcionario f1 = new Funcionario(); f1.setCompetencias(Set.of(java, sql));
        Funcionario f2 = new Funcionario(); f2.setCompetencias(Set.of(java));

        when(funcionarioRepo.findAll()).thenReturn(List.of(f1, f2));

        Map<String, Long> result = dashboardService.getDistribuicaoPorCompetencias();

        assertEquals(2L, result.get("Java"));
        assertEquals(1L, result.get("SQL"));
    }

    @Test
    void getDistribuicaoPorCompetencias_usaChaveSemNome_quandoNomeNuloOuVazio() {
        Competencia semNome = new Competencia(); semNome.setCodigo(3); semNome.setNome("");
        Funcionario f = new Funcionario(); f.setCompetencias(Set.of(semNome));

        when(funcionarioRepo.findAll()).thenReturn(List.of(f));

        Map<String, Long> result = dashboardService.getDistribuicaoPorCompetencias();

        assertEquals(1L, result.get("Sem nome"));
    }
}