package br.com.alltallent.caramelstray;

import br.com.alltallent.dto.FuncionarioPerfilDTO;
import br.com.alltallent.model.*;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FuncionarioPerfilDTOTest {

    private Funcionario funcionarioBase() {
        Funcionario f = new Funcionario();
        f.setCodigo(1);
        f.setNomeCompleto("Ana Costa");
        f.setEmail("ana@test.com");
        return f;
    }

    private Area area(String nome) {
        Area a = new Area();
        a.setCodigo(10);
        a.setNome(nome);
        return a;
    }

    private Funcionario gestor(String nome) {
        Funcionario g = new Funcionario();
        g.setCodigo(99);
        g.setNomeCompleto(nome);
        return g;
    }

    private Competencia competencia(int codigo, String nome, String categoria) {
        Competencia c = new Competencia();
        c.setCodigo(codigo);
        c.setNome(nome);
        c.setCategoria(categoria);
        return c;
    }

    private FuncionarioCertificado certificado(int codigo, String nome) {
        FuncionarioCertificado c = new FuncionarioCertificado();
        c.setCodigo(codigo);
        c.setCertificado(nome);
        return c;
    }

    @Test
    void fromFuncionario_mapeiaScalarsCorretamente() {
        Funcionario f = funcionarioBase();
        f.setTelefone("11999990000");
        f.setTituloProfissional("Desenvolvedora");
        f.setResumo("Resumo da Ana");
        f.setLocalizacao("Rio de Janeiro");
        OffsetDateTime agora = OffsetDateTime.now();
        f.setDataCadastro(agora);

        var dto = new FuncionarioPerfilDTO(f);

        assertEquals(1, dto.codigo());
        assertEquals("Ana Costa", dto.nomeCompleto());
        assertEquals("ana@test.com", dto.email());
        assertEquals("11999990000", dto.telefone());
        assertEquals("Desenvolvedora", dto.tituloProfissional());
        assertEquals("Resumo da Ana", dto.resumo());
        assertEquals("Rio de Janeiro", dto.localizacao());
        assertEquals(agora, dto.dataCadastro());
    }

    @Test
    void fromFuncionario_comArea_mapeiaAreaNome() {
        Funcionario f = funcionarioBase();
        f.setArea(area("Engenharia"));

        var dto = new FuncionarioPerfilDTO(f);

        assertEquals("Engenharia", dto.nomeArea());
    }

    @Test
    void fromFuncionario_semArea_nomeAreaNulo() {
        Funcionario f = funcionarioBase();
        f.setArea(null);

        var dto = new FuncionarioPerfilDTO(f);

        assertNull(dto.nomeArea());
    }

    @Test
    void fromFuncionario_comGestor_mapeiaGestorNome() {
        Funcionario f = funcionarioBase();
        f.setGestor(gestor("Carlos Gestor"));

        var dto = new FuncionarioPerfilDTO(f);

        assertEquals("Carlos Gestor", dto.nomeGestor());
    }

    @Test
    void fromFuncionario_semGestor_nomeGestorNulo() {
        Funcionario f = funcionarioBase();
        f.setGestor(null);

        var dto = new FuncionarioPerfilDTO(f);

        assertNull(dto.nomeGestor());
    }

    @Test
    void fromFuncionario_comCompetencias_mapeiaLista() {
        Funcionario f = funcionarioBase();
        f.setCompetencias(Set.of(competencia(1, "Docker", "DevOps")));

        var dto = new FuncionarioPerfilDTO(f);

        assertEquals(1, dto.competencias().size());
        assertEquals("Docker", dto.competencias().get(0).nome());
        assertEquals("DevOps", dto.competencias().get(0).categoria());
    }

    @Test
    void fromFuncionario_competenciasNulas_retornaListaVazia() {
        Funcionario f = funcionarioBase();
        f.setCompetencias(null);

        var dto = new FuncionarioPerfilDTO(f);

        assertTrue(dto.competencias().isEmpty());
    }

    @Test
    void fromFuncionario_comCertificados_mapeiaLista() {
        Funcionario f = funcionarioBase();
        f.setCertificados(Set.of(certificado(5, "Scrum Master")));

        var dto = new FuncionarioPerfilDTO(f);

        assertEquals(1, dto.certificados().size());
        assertEquals(5, dto.certificados().get(0).codigo());
        assertEquals("Scrum Master", dto.certificados().get(0).nome());
    }

    @Test
    void fromFuncionario_certificadosNulos_retornaListaVazia() {
        Funcionario f = funcionarioBase();
        f.setCertificados(null);

        var dto = new FuncionarioPerfilDTO(f);

        assertTrue(dto.certificados().isEmpty());
    }

    @Test
    void fromFuncionario_multiplosItensEmCadaLista() {
        Funcionario f = funcionarioBase();
        f.setCompetencias(Set.of(
                competencia(1, "Java", "Backend"),
                competencia(2, "React", "Frontend")));
        f.setCertificados(Set.of(
                certificado(1, "AWS"),
                certificado(2, "GCP")));

        var dto = new FuncionarioPerfilDTO(f);

        assertEquals(2, dto.competencias().size());
        assertEquals(2, dto.certificados().size());
    }

    @Test
    void canonicalConstructor_defineCamposCorretamente() {
        var dto = new FuncionarioPerfilDTO(
                2, "Nome", "email@test.com", "119",
                null, "Título", "Resumo", "Cidade",
                "TI", "Gestor X", List.of(), List.of());

        assertEquals(2, dto.codigo());
        assertEquals("Nome", dto.nomeCompleto());
        assertEquals("email@test.com", dto.email());
        assertEquals("119", dto.telefone());
        assertNull(dto.dataCadastro());
        assertEquals("Título", dto.tituloProfissional());
        assertEquals("Resumo", dto.resumo());
        assertEquals("Cidade", dto.localizacao());
        assertEquals("TI", dto.nomeArea());
        assertEquals("Gestor X", dto.nomeGestor());
        assertTrue(dto.competencias().isEmpty());
        assertTrue(dto.certificados().isEmpty());
    }
}