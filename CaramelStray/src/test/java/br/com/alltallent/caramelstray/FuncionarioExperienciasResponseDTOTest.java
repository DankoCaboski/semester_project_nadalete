package br.com.alltallent.caramelstray;

import br.com.alltallent.dto.FuncionarioExperienciasResponseDTO;
import br.com.alltallent.model.Experiencia;
import br.com.alltallent.model.Funcionario;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FuncionarioExperienciasResponseDTOTest {

    private Funcionario funcionarioSimples(int codigo, String nome) {
        Funcionario f = new Funcionario();
        f.setCodigo(codigo);
        f.setNomeCompleto(nome);
        return f;
    }

    private Experiencia experiencia(int codigo, String cargo, String empresa,
                                    LocalDate inicio, LocalDate fim, String descricao) {
        Experiencia e = new Experiencia();
        e.setCodigo(codigo);
        e.setCargo(cargo);
        e.setEmpresa(empresa);
        e.setDataInicio(inicio);
        e.setDataFim(fim);
        e.setDescricao(descricao);
        return e;
    }

    @Test
    void canonicalConstructor_defineCamposCorretamente() {
        var dto = new FuncionarioExperienciasResponseDTO(1, "João", java.util.List.of());
        assertEquals(1, dto.codigoFuncionario());
        assertEquals("João", dto.nomeCompleto());
        assertTrue(dto.experiencias().isEmpty());
    }

    @Test
    void fromFuncionario_semExperiencias_retornaListaVazia() {
        Funcionario f = funcionarioSimples(2, "Maria");
        f.setExperiencias(null);

        var dto = new FuncionarioExperienciasResponseDTO(f);

        assertEquals(2, dto.codigoFuncionario());
        assertEquals("Maria", dto.nomeCompleto());
        assertTrue(dto.experiencias().isEmpty());
    }

    @Test
    void fromFuncionario_setVazio_retornaListaVazia() {
        Funcionario f = funcionarioSimples(3, "Pedro");
        f.setExperiencias(Set.of());

        var dto = new FuncionarioExperienciasResponseDTO(f);

        assertTrue(dto.experiencias().isEmpty());
    }

    @Test
    void fromFuncionario_comUmaExperiencia_mapeiaCorretamente() {
        Funcionario f = funcionarioSimples(4, "Ana");
        Experiencia exp = experiencia(10, "Dev", "Empresa X",
                LocalDate.of(2020, 1, 1), LocalDate.of(2022, 6, 30), "Backend");
        f.setExperiencias(Set.of(exp));

        var dto = new FuncionarioExperienciasResponseDTO(f);

        assertEquals(1, dto.experiencias().size());
        var expDto = dto.experiencias().get(0);
        assertEquals(10, expDto.codigo());
        assertEquals("Dev", expDto.cargo());
        assertEquals("Empresa X", expDto.empresa());
        assertEquals(LocalDate.of(2020, 1, 1), expDto.dataInicio());
        assertEquals(LocalDate.of(2022, 6, 30), expDto.dataFim());
        assertEquals("Backend", expDto.descricao());
    }

    @Test
    void fromFuncionario_comDuasExperiencias_retornaTamanhoCorreto() {
        Funcionario f = funcionarioSimples(5, "Carlos");
        Experiencia e1 = experiencia(1, "Dev", "A", LocalDate.of(2018, 1, 1), null, null);
        Experiencia e2 = experiencia(2, "QA",  "B", LocalDate.of(2021, 3, 1), null, null);
        Set<Experiencia> set = new LinkedHashSet<>();
        set.add(e1);
        set.add(e2);
        f.setExperiencias(set);

        var dto = new FuncionarioExperienciasResponseDTO(f);

        assertEquals(2, dto.experiencias().size());
    }

    @Test
    void fromFuncionario_experienciaSemDataFim_dataFimNula() {
        Funcionario f = funcionarioSimples(6, "Bia");
        Experiencia exp = experiencia(20, "Analista", "Corp", LocalDate.of(2023, 1, 1), null, null);
        f.setExperiencias(Set.of(exp));

        var dto = new FuncionarioExperienciasResponseDTO(f);

        assertNull(dto.experiencias().get(0).dataFim());
    }

    @Test
    void fromFuncionario_nomeCompletoNulo_propagaNulo() {
        Funcionario f = funcionarioSimples(7, null);
        f.setExperiencias(Set.of());

        var dto = new FuncionarioExperienciasResponseDTO(f);

        assertNull(dto.nomeCompleto());
    }
}