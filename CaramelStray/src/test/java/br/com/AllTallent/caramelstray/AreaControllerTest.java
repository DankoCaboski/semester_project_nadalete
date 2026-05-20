package br.com.AllTallent.caramelstray;

import br.com.AllTallent.config.JwtService;
import br.com.AllTallent.controller.AreaController;
import br.com.AllTallent.model.Area;
import br.com.AllTallent.repository.AreaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AreaController.class)
class AreaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AreaRepository areaRepository;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser
    void createArea_deveRetornar201ComAreaCriada() throws Exception {
        Area areaSalva = new Area(1, "Tecnologia", "Área de TI");
        when(areaRepository.save(any(Area.class))).thenReturn(areaSalva);

        String body = """
                {
                    "nome": "Tecnologia",
                    "descricao": "Área de TI"
                }
                """;

        mockMvc.perform(post("/api/area")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.nome").value("Tecnologia"))
                .andExpect(jsonPath("$.descricao").value("Área de TI"));
    }

    @Test
    @WithMockUser
    void getAllAreas_deveRetornar200ComListaDeAreas() throws Exception {
        List<Area> areas = List.of(
                new Area(1, "Tecnologia", "Área de TI"),
                new Area(2, "Saúde", "Área da saúde")
        );
        when(areaRepository.findAll()).thenReturn(areas);

        mockMvc.perform(get("/api/area"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Tecnologia"))
                .andExpect(jsonPath("$[1].nome").value("Saúde"));
    }

    @Test
    @WithMockUser
    void getAllAreas_deveRetornar200ComListaVazia() throws Exception {
        when(areaRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/area"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
