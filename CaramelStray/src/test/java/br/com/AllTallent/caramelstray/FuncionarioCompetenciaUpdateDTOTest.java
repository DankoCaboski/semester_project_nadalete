package br.com.AllTallent.caramelstray;

import br.com.AllTallent.dto.FuncionarioCompetenciaUpdateDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FuncionarioCompetenciaUpdateDTOTest {

    @Test
    void construtorDefineListaCorretamente() {
        FuncionarioCompetenciaUpdateDTO dto = new FuncionarioCompetenciaUpdateDTO(List.of(1, 2, 3));

        assertEquals(List.of(1, 2, 3), dto.codigosCompetencia());
    }

    @Test
    void listaPodesSerVazia() {
        FuncionarioCompetenciaUpdateDTO dto = new FuncionarioCompetenciaUpdateDTO(List.of());

        assertNotNull(dto.codigosCompetencia());
        assertTrue(dto.codigosCompetencia().isEmpty());
    }

    @Test
    void listaPodesSerNula() {
        FuncionarioCompetenciaUpdateDTO dto = new FuncionarioCompetenciaUpdateDTO(null);

        assertNull(dto.codigosCompetencia());
    }

    @Test
    void listaPreservaOrdem() {
        List<Integer> codigos = List.of(10, 5, 30, 2);
        FuncionarioCompetenciaUpdateDTO dto = new FuncionarioCompetenciaUpdateDTO(codigos);

        assertEquals(codigos, dto.codigosCompetencia());
    }
}
