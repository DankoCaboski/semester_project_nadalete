package br.com.AllTallent.caramelstray;

import br.com.AllTallent.dto.CompetenciaDTO;
import br.com.AllTallent.dto.FuncionarioCompetenciasResponseDTO;
import br.com.AllTallent.model.Competencia;
import br.com.AllTallent.model.Funcionario;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FuncionarioCompetenciasResponseDTOTest {

    // -------------------------------------------------------------------------
    // Construtor canônico
    // -------------------------------------------------------------------------

    @Test
    void construtorCanonico_defineListaCorretamente() {
        List<CompetenciaDTO> lista = List.of(new CompetenciaDTO(1, "Java", "Backend"));

        FuncionarioCompetenciasResponseDTO dto = new FuncionarioCompetenciasResponseDTO(lista);

        assertEquals(1, dto.competencias().size());
        assertEquals("Java", dto.competencias().get(0).nome());
    }

    @Test
    void construtorCanonico_listaPodesSerVazia() {
        FuncionarioCompetenciasResponseDTO dto = new FuncionarioCompetenciasResponseDTO(List.of());

        assertNotNull(dto.competencias());
        assertTrue(dto.competencias().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Construtor que recebe Funcionario
    // -------------------------------------------------------------------------

    @Test
    void construtorFuncionario_mapeiaCompetenciasCorretamente() {
        Competencia c1 = new Competencia();
        c1.setCodigo(10);
        c1.setNome("Spring Boot");
        c1.setCategoria("Backend");

        Competencia c2 = new Competencia();
        c2.setCodigo(20);
        c2.setNome("React");
        c2.setCategoria("Frontend");

        Funcionario funcionario = new Funcionario();
        funcionario.setCompetencias(Set.of(c1, c2));

        FuncionarioCompetenciasResponseDTO dto = new FuncionarioCompetenciasResponseDTO(funcionario);

        assertNotNull(dto.competencias());
        assertEquals(2, dto.competencias().size());

        List<Integer> ids = dto.competencias().stream()
                .map(CompetenciaDTO::id)
                .toList();
        assertTrue(ids.containsAll(List.of(10, 20)));
    }

    @Test
    void construtorFuncionario_retornaListaVazia_quandoCompetenciasNulas() {
        Funcionario funcionario = new Funcionario();
        funcionario.setCompetencias(null);

        FuncionarioCompetenciasResponseDTO dto = new FuncionarioCompetenciasResponseDTO(funcionario);

        assertNotNull(dto.competencias());
        assertTrue(dto.competencias().isEmpty());
    }

    @Test
    void construtorFuncionario_retornaListaVazia_quandoSetVazio() {
        Funcionario funcionario = new Funcionario();
        funcionario.setCompetencias(Set.of());

        FuncionarioCompetenciasResponseDTO dto = new FuncionarioCompetenciasResponseDTO(funcionario);

        assertNotNull(dto.competencias());
        assertTrue(dto.competencias().isEmpty());
    }

    @Test
    void construtorFuncionario_mapeiaCategoria() {
        Competencia c = new Competencia();
        c.setCodigo(5);
        c.setNome("SQL");
        c.setCategoria("Banco de Dados");

        Funcionario funcionario = new Funcionario();
        funcionario.setCompetencias(Set.of(c));

        FuncionarioCompetenciasResponseDTO dto = new FuncionarioCompetenciasResponseDTO(funcionario);

        CompetenciaDTO competenciaDTO = dto.competencias().get(0);
        assertEquals(5, competenciaDTO.id());
        assertEquals("SQL", competenciaDTO.nome());
        assertEquals("Banco de Dados", competenciaDTO.categoria());
    }
}
