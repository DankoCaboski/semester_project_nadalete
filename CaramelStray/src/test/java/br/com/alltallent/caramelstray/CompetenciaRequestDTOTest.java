package br.com.alltallent.caramelstray;

import br.com.alltallent.dto.CompetenciaRequestDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompetenciaRequestDTOTest {

    @Test
    void testNoArgsConstructor() {
        CompetenciaRequestDTO dto = new CompetenciaRequestDTO();

        assertNull(dto.getNome());
        assertNull(dto.getCategoria());
    }

    @Test
    void testGettersAndSetters() {
        CompetenciaRequestDTO dto = new CompetenciaRequestDTO();
        dto.setNome("Java");
        dto.setCategoria("Programação");

        assertEquals("Java", dto.getNome());
        assertEquals("Programação", dto.getCategoria());
    }

    @Test
    void testSetNomeNull() {
        CompetenciaRequestDTO dto = new CompetenciaRequestDTO();
        dto.setNome(null);

        assertNull(dto.getNome());
    }

    @Test
    void testSetCategoriaNull() {
        CompetenciaRequestDTO dto = new CompetenciaRequestDTO();
        dto.setCategoria(null);

        assertNull(dto.getCategoria());
    }

    @Test
    void testCamposIndependentes() {
        CompetenciaRequestDTO dto = new CompetenciaRequestDTO();
        dto.setNome("Spring Boot");

        assertEquals("Spring Boot", dto.getNome());
        assertNull(dto.getCategoria());
    }
}
