package br.com.AllTallent.caramelstray;

import br.com.AllTallent.dto.FuncionarioResponseDTO;
import br.com.AllTallent.model.*;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FuncionarioResponseDTOTest {

    private Funcionario funcionarioBase() {
        Funcionario f = new Funcionario();
        f.setCodigo(1);
        f.setNomeCompleto("João Silva");
        f.setEmail("joao@test.com");
        return f;
    }

    private Area area(int codigo, String nome) {
        Area a = new Area();
        a.setCodigo(codigo);
        a.setNome(nome);
        return a;
    }

    private Perfil perfil(int codigo, String nome) {
        Perfil p = new Perfil();
        p.setCodigo(codigo);
        p.setNome(nome);
        return p;
    }

    private Funcionario gestor(int codigo, String nome) {
        Funcionario g = new Funcionario();
        g.setCodigo(codigo);
        g.setNomeCompleto(nome);
        return g;
    }

    private FuncionarioCertificado certificado(int codigo, String nome) {
        FuncionarioCertificado c = new FuncionarioCertificado();
        c.setCodigo(codigo);
        c.setCertificado(nome);
        return c;
    }

    private Competencia competencia(int codigo, String nome, String categoria) {
        Competencia c = new Competencia();
        c.setCodigo(codigo);
        c.setNome(nome);
        c.setCategoria(categoria);
        return c;
    }

    private Experiencia experiencia(int codigo, String cargo, String empresa) {
        Experiencia e = new Experiencia();
        e.setCodigo(codigo);
        e.setCargo(cargo);
        e.setEmpresa(empresa);
        return e;
    }

    @Test
    void fromFuncionario_mapeiaScalarsCorretamente() {
        Funcionario f = funcionarioBase();
        f.setTelefone("11999999999");
        f.setTituloProfissional("Engenheiro");
        f.setLocalizacao("São Paulo");
        f.setResumo("Resumo profissional");
        OffsetDateTime agora = OffsetDateTime.now();
        f.setDataCadastro(agora);

        var dto = new FuncionarioResponseDTO(f);

        assertEquals(1, dto.codigo());
        assertEquals("João Silva", dto.nomeCompleto());
        assertEquals("joao@test.com", dto.email());
        assertEquals("11999999999", dto.telefone());
        assertEquals("Engenheiro", dto.tituloProfissional());
        assertEquals("São Paulo", dto.localizacao());
        assertEquals("Resumo profissional", dto.resumo());
        assertEquals(agora, dto.dataCadastro());
    }

    @Test
    void fromFuncionario_comArea_mapeiaAreaIdENome() {
        Funcionario f = funcionarioBase();
        f.setArea(area(10, "Tecnologia"));

        var dto = new FuncionarioResponseDTO(f);

        assertEquals(10, dto.areaId());
        assertEquals("Tecnologia", dto.nomeArea());
    }

    @Test
    void fromFuncionario_semArea_areaIdENomeNulos() {
        Funcionario f = funcionarioBase();
        f.setArea(null);

        var dto = new FuncionarioResponseDTO(f);

        assertNull(dto.areaId());
        assertNull(dto.nomeArea());
    }

    @Test
    void fromFuncionario_comPerfil_mapeiaPerfilIdENome() {
        Funcionario f = funcionarioBase();
        f.setPerfil(perfil(2, "Supervisor"));

        var dto = new FuncionarioResponseDTO(f);

        assertEquals(2, dto.perfilId());
        assertEquals("Supervisor", dto.nomePerfil());
    }

    @Test
    void fromFuncionario_semPerfil_perfilIdENomeNulos() {
        Funcionario f = funcionarioBase();
        f.setPerfil(null);

        var dto = new FuncionarioResponseDTO(f);

        assertNull(dto.perfilId());
        assertNull(dto.nomePerfil());
    }

    @Test
    void fromFuncionario_comGestor_mapeiaGestorIdENome() {
        Funcionario f = funcionarioBase();
        f.setGestor(gestor(5, "Maria Gestora"));

        var dto = new FuncionarioResponseDTO(f);

        assertEquals(5, dto.gestorId());
        assertEquals("Maria Gestora", dto.nomeGestor());
    }

    @Test
    void fromFuncionario_semGestor_gestorIdENomeNulos() {
        Funcionario f = funcionarioBase();
        f.setGestor(null);

        var dto = new FuncionarioResponseDTO(f);

        assertNull(dto.gestorId());
        assertNull(dto.nomeGestor());
    }

    @Test
    void fromFuncionario_comCertificados_mapeiaLista() {
        Funcionario f = funcionarioBase();
        f.setCertificados(Set.of(certificado(1, "AWS"), certificado(2, "Java")));

        var dto = new FuncionarioResponseDTO(f);

        assertEquals(2, dto.certificados().size());
        assertTrue(dto.certificados().stream().anyMatch(c -> c.nome().equals("AWS")));
        assertTrue(dto.certificados().stream().anyMatch(c -> c.nome().equals("Java")));
    }

    @Test
    void fromFuncionario_certificadosNulos_retornaListaVazia() {
        Funcionario f = funcionarioBase();
        f.setCertificados(null);

        var dto = new FuncionarioResponseDTO(f);

        assertTrue(dto.certificados().isEmpty());
    }

    @Test
    void fromFuncionario_comCompetencias_mapeiaLista() {
        Funcionario f = funcionarioBase();
        f.setCompetencias(Set.of(competencia(10, "Spring", "Backend")));

        var dto = new FuncionarioResponseDTO(f);

        assertEquals(1, dto.competencias().size());
        assertEquals("Spring", dto.competencias().get(0).nome());
        assertEquals("Backend", dto.competencias().get(0).categoria());
    }

    @Test
    void fromFuncionario_competenciasNulas_retornaListaVazia() {
        Funcionario f = funcionarioBase();
        f.setCompetencias(null);

        var dto = new FuncionarioResponseDTO(f);

        assertTrue(dto.competencias().isEmpty());
    }

    @Test
    void fromFuncionario_comExperiencias_mapeiaLista() {
        Funcionario f = funcionarioBase();
        f.setExperiencias(Set.of(experiencia(20, "Dev", "Empresa X")));

        var dto = new FuncionarioResponseDTO(f);

        assertEquals(1, dto.experiencias().size());
        assertEquals("Dev", dto.experiencias().get(0).cargo());
        assertEquals("Empresa X", dto.experiencias().get(0).empresa());
    }

    @Test
    void fromFuncionario_experienciasNulas_retornaListaVazia() {
        Funcionario f = funcionarioBase();
        f.setExperiencias(null);

        var dto = new FuncionarioResponseDTO(f);

        assertTrue(dto.experiencias().isEmpty());
    }

    @Test
    void fromFuncionario_todasRelacoesCombinadas() {
        Funcionario f = funcionarioBase();
        f.setArea(area(3, "RH"));
        f.setPerfil(perfil(1, "Diretor"));
        f.setGestor(gestor(9, "Chefe"));
        f.setCertificados(Set.of(certificado(1, "PMP")));
        f.setCompetencias(Set.of(competencia(2, "Liderança", "Soft")));
        f.setExperiencias(Set.of(experiencia(3, "Gerente", "Corp")));

        var dto = new FuncionarioResponseDTO(f);

        assertEquals(3, dto.areaId());
        assertEquals(1, dto.perfilId());
        assertEquals(9, dto.gestorId());
        assertEquals(1, dto.certificados().size());
        assertEquals(1, dto.competencias().size());
        assertEquals(1, dto.experiencias().size());
    }

    @Test
    void canonicalConstructor_defineCamposCorretamente() {
        var dto = new FuncionarioResponseDTO(
                1, "Nome", "email@test.com", "11999", "TI", "Dev",
                "Gestor", 10, 3, 5, "Título", "Cidade", "Resumo",
                null, List.of(), List.of(), List.of());

        assertEquals(1, dto.codigo());
        assertEquals("Nome", dto.nomeCompleto());
        assertEquals("email@test.com", dto.email());
        assertEquals("11999", dto.telefone());
        assertEquals("TI", dto.nomeArea());
        assertEquals("Dev", dto.nomePerfil());
        assertEquals("Gestor", dto.nomeGestor());
        assertEquals(10, dto.areaId());
        assertEquals(3, dto.perfilId());
        assertEquals(5, dto.gestorId());
        assertEquals("Título", dto.tituloProfissional());
        assertEquals("Cidade", dto.localizacao());
        assertEquals("Resumo", dto.resumo());
        assertNull(dto.dataCadastro());
    }
}