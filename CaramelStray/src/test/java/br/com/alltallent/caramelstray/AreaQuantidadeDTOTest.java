package br.com.alltallent.caramelstray;

import br.com.alltallent.dto.AreaQuantidadeDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AreaQuantidadeDTOTest {

    @Test
    void allArgsConstructor_defineCamposCorretamente() {
        var dto = new AreaQuantidadeDTO("Tecnologia", 10L);
        assertEquals("Tecnologia", dto.getNomeArea());
        assertEquals(10L, dto.getQuantidade());
    }

    @Test
    void setter_nomeArea() {
        var dto = new AreaQuantidadeDTO("X", 0L);
        dto.setNomeArea("RH");
        assertEquals("RH", dto.getNomeArea());
    }

    @Test
    void setter_quantidade() {
        var dto = new AreaQuantidadeDTO("X", 0L);
        dto.setQuantidade(99L);
        assertEquals(99L, dto.getQuantidade());
    }

    @Test
    void camposPodemSerNulos() {
        var dto = new AreaQuantidadeDTO(null, null);
        assertNull(dto.getNomeArea());
        assertNull(dto.getQuantidade());
    }
}