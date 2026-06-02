package br.com.alltallent.caramelstray;

import br.com.alltallent.model.Area;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AreaTest {

    @Test
    void testGettersAndSetters() {
        Area area = new Area();

        area.setCodigo(1);
        area.setNome("Tecnologia");
        area.setDescricao("Área de tecnologia da informação");

        assertEquals(1, area.getCodigo());
        assertEquals("Tecnologia", area.getNome());
        assertEquals("Área de tecnologia da informação", area.getDescricao());
    }

    @Test
    void testAllArgsConstructor() {
        Area area = new Area(2, "Saúde", "Área da saúde");

        assertEquals(2, area.getCodigo());
        assertEquals("Saúde", area.getNome());
        assertEquals("Área da saúde", area.getDescricao());
    }

    @Test
    void testNoArgsConstructor() {
        Area area = new Area();

        assertNull(area.getCodigo());
        assertNull(area.getNome());
        assertNull(area.getDescricao());
    }

    @Test
    void testEqualsAndHashCode() {
        Area area1 = new Area(1, "Tecnologia", "Desc");
        Area area2 = new Area(1, "Outro nome", "Outra desc");
        Area area3 = new Area(2, "Tecnologia", "Desc");

        assertEquals(area1, area2);
        assertNotEquals(area1, area3);
        assertEquals(area1.hashCode(), area2.hashCode());
    }
}
