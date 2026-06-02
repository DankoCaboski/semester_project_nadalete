package br.com.alltallent.caramelstray;

import br.com.alltallent.config.JwtService;
import br.com.alltallent.controller.PerfilController;
import br.com.alltallent.model.Perfil;
import br.com.alltallent.repository.PerfilRepository;
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

@WebMvcTest(PerfilController.class)
class PerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PerfilRepository perfilRepository;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser
    void createPerfil_deveRetornar201ComPerfilCriado() throws Exception {
        Perfil perfilSalvo = new Perfil(1, "Administrador", "Perfil com acesso total");
        when(perfilRepository.save(any(Perfil.class))).thenReturn(perfilSalvo);

        String body = """
                {
                    "nome": "Administrador",
                    "descricao": "Perfil com acesso total"
                }
                """;

        mockMvc.perform(post("/api/perfil")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.nome").value("Administrador"))
                .andExpect(jsonPath("$.descricao").value("Perfil com acesso total"));
    }

    @Test
    @WithMockUser
    void createPerfil_naoDeveAceitarCodigoDoCliente() throws Exception {
        Perfil perfilSalvo = new Perfil(99, "Operador", "Perfil operacional");
        when(perfilRepository.save(any(Perfil.class))).thenReturn(perfilSalvo);

        String bodyComCodigo = """
                {
                    "codigo": 42,
                    "nome": "Operador",
                    "descricao": "Perfil operacional"
                }
                """;

        mockMvc.perform(post("/api/perfil")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyComCodigo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(99));
    }

    @Test
    @WithMockUser
    void getAllPerfis_deveRetornar200ComListaDePerfis() throws Exception {
        List<Perfil> perfis = List.of(
                new Perfil(1, "Administrador", "Perfil com acesso total"),
                new Perfil(2, "Operador", "Perfil operacional")
        );
        when(perfilRepository.findAll()).thenReturn(perfis);

        mockMvc.perform(get("/api/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Administrador"))
                .andExpect(jsonPath("$[1].nome").value("Operador"));
    }

    @Test
    @WithMockUser
    void getAllPerfis_deveRetornar200ComListaVazia() throws Exception {
        when(perfilRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
