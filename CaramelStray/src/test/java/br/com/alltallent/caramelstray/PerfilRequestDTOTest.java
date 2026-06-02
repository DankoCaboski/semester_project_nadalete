package br.com.alltallent.caramelstray;

import br.com.alltallent.dto.PerfilRequestDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerfilRequestDTOTest {

    @Test
    void testNoArgsConstructor() {
        PerfilRequestDTO dto = new PerfilRequestDTO();

        assertNull(dto.getNome());
        assertNull(dto.getDescricao());
    }

    @Test
    void testGettersAndSetters() {
        PerfilRequestDTO dto = new PerfilRequestDTO();
        dto.setNome("Administrador");
        dto.setDescricao("Perfil com acesso total");

        assertEquals("Administrador", dto.getNome());
        assertEquals("Perfil com acesso total", dto.getDescricao());
    }

    @Test
    void testSetNomeNull() {
        PerfilRequestDTO dto = new PerfilRequestDTO();
        dto.setNome(null);

        assertNull(dto.getNome());
    }

    @Test
    void testSetDescricaoNull() {
        PerfilRequestDTO dto = new PerfilRequestDTO();
        dto.setDescricao(null);

        assertNull(dto.getDescricao());
    }
}
