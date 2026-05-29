package br.com.AllTallent.caramelstray;

import br.com.AllTallent.config.CustomUserDetails;
import br.com.AllTallent.dto.*;
import br.com.AllTallent.exception.ResourceNotFoundException;
import br.com.AllTallent.exception.UnauthorizedActionException;
import br.com.AllTallent.model.*;
import br.com.AllTallent.repository.*;
import br.com.AllTallent.service.FuncionarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceTest {

    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private AreaRepository areaRepository;
    @Mock private PerfilRepository perfilRepository;
    @Mock private CompetenciaRepository competenciaRepository;
    @Mock private ExperienciaRepository experienciaRepository;
    @Mock private CertificadoRepository certificadoRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private FuncionarioService funcionarioService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Funcionario funcionarioSimples(int codigo) {
        Funcionario f = new Funcionario();
        f.setCodigo(codigo);
        f.setNomeCompleto("Funcionário " + codigo);
        f.setEmail("func" + codigo + "@test.com");
        f.setSenhaHash("hash");
        return f;
    }

    private Funcionario funcionarioComPerfilEArea(int codigo, int perfilCodigo, int areaCodigo) {
        Area area = new Area();
        area.setCodigo(areaCodigo);

        Perfil perfil = new Perfil();
        perfil.setCodigo(perfilCodigo);

        Funcionario f = funcionarioSimples(codigo);
        f.setPerfil(perfil);
        f.setArea(area);
        return f;
    }

    private void setarUsuarioLogado(CustomUserDetails principal) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @BeforeEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }


    @Test
    void listarTodos_semFiltro_retornaTodosOsFuncionarios() {
        when(funcionarioRepository.findAll()).thenReturn(
                List.of(funcionarioSimples(1), funcionarioSimples(2)));

        List<FuncionarioResponseDTO> resultado = funcionarioService.listarTodos(null);

        assertEquals(2, resultado.size());
        verify(funcionarioRepository).findAll();
        verify(funcionarioRepository, never()).buscarPorTexto(any());
    }

    @Test
    void listarTodos_comFiltro_usaBuscaPorTexto() {
        when(funcionarioRepository.buscarPorTexto("João"))
                .thenReturn(List.of(funcionarioSimples(1)));

        List<FuncionarioResponseDTO> resultado = funcionarioService.listarTodos("João");

        assertEquals(1, resultado.size());
        verify(funcionarioRepository).buscarPorTexto("João");
        verify(funcionarioRepository, never()).findAll();
    }

    @Test
    void listarTodos_filtroEmBranco_retornaTodos() {
        when(funcionarioRepository.findAll()).thenReturn(List.of(funcionarioSimples(1)));

        List<FuncionarioResponseDTO> resultado = funcionarioService.listarTodos("   ");

        assertEquals(1, resultado.size());
        verify(funcionarioRepository).findAll();
    }


    @Test
    void buscarPorId_retornaDTO_quandoEncontrado() {
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioSimples(1)));

        FuncionarioResponseDTO dto = funcionarioService.buscarPorId(1);

        assertNotNull(dto);
        assertEquals(1, dto.codigo());
    }

    @Test
    void buscarPorId_lancaResourceNotFound_quandoNaoExiste() {
        when(funcionarioRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> funcionarioService.buscarPorId(999));
    }


    @Test
    void criar_persisteESalvaFuncionario() {
        FuncionarioRequestDTO dto = new FuncionarioRequestDTO(
                "João Silva", "joao@test.com", null, null, null,
                null, null, null, null, null, null);

        Funcionario salvo = funcionarioSimples(1);
        salvo.setNomeCompleto("João Silva");

        when(funcionarioRepository.save(any())).thenReturn(salvo);

        FuncionarioResponseDTO resultado = funcionarioService.criar(dto);

        assertNotNull(resultado);
        verify(funcionarioRepository).save(any());
    }

    @Test
    void criar_codificaSenha_quandoFornecida() {
        FuncionarioRequestDTO dto = new FuncionarioRequestDTO(
                "Ana", "ana@test.com", null, null, "senha123",
                null, null, null, null, null, null);

        when(passwordEncoder.encode("senha123")).thenReturn("hash-codificado");
        Funcionario salvo = funcionarioSimples(2);
        when(funcionarioRepository.save(any())).thenReturn(salvo);

        funcionarioService.criar(dto);

        verify(passwordEncoder).encode("senha123");
    }

    @Test
    void criar_associaArea_quandoAreaIdFornecida() {
        Area area = new Area();
        area.setCodigo(5);
        area.setNome("TI");

        FuncionarioRequestDTO dto = new FuncionarioRequestDTO(
                "Maria", "maria@test.com", null, null, null,
                5, null, null, null, null, null);

        when(areaRepository.findById(5)).thenReturn(Optional.of(area));
        when(funcionarioRepository.save(any())).thenReturn(funcionarioSimples(3));

        funcionarioService.criar(dto);

        verify(areaRepository).findById(5);
    }

    @Test
    void criar_lancaResourceNotFound_quandoAreaNaoExiste() {
        FuncionarioRequestDTO dto = new FuncionarioRequestDTO(
                "Pedro", "pedro@test.com", null, null, null,
                999, null, null, null, null, null);

        when(areaRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> funcionarioService.criar(dto));
    }


    @Test
    void atualizar_atualizaDados_quandoFuncionarioExiste() {
        Funcionario existente = funcionarioSimples(1);
        FuncionarioRequestDTO dto = new FuncionarioRequestDTO(
                "Novo Nome", null, null, null, null,
                null, null, null, null, null, null);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(existente));
        when(funcionarioRepository.save(any())).thenReturn(existente);

        FuncionarioResponseDTO resultado = funcionarioService.atualizar(1, dto);

        assertNotNull(resultado);
        verify(funcionarioRepository).save(existente);
    }

    @Test
    void atualizar_lancaResourceNotFound_quandoFuncionarioNaoExiste() {
        FuncionarioRequestDTO dto = new FuncionarioRequestDTO(
                "Qualquer", null, null, null, null,
                null, null, null, null, null, null);

        when(funcionarioRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> funcionarioService.atualizar(999, dto));
    }


    @Test
    void deletar_removeFuncionario_quandoExiste() {
        when(funcionarioRepository.existsById(1)).thenReturn(true);

        assertDoesNotThrow(() -> funcionarioService.deletar(1));

        verify(funcionarioRepository).deleteById(1);
    }

    @Test
    void deletar_lancaResourceNotFound_quandoNaoExiste() {
        when(funcionarioRepository.existsById(99)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> funcionarioService.deletar(99));
    }


    @Test
    void associarCompetencias_permitido_quandoProprioFuncionario() {
        // Funcionário logado edita as próprias competências
        Funcionario logado = funcionarioComPerfilEArea(5, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(logado));

        Competencia c1 = new Competencia();
        c1.setCodigo(1);

        when(funcionarioRepository.findByIdCompleto(5)).thenReturn(Optional.of(logado));
        when(competenciaRepository.findAllById(List.of(1))).thenReturn(List.of(c1));
        when(funcionarioRepository.save(any())).thenReturn(logado);

        assertDoesNotThrow(() -> funcionarioService.associarCompetencias(5, List.of(1)));

        verify(funcionarioRepository).save(logado);
    }

    @Test
    void associarCompetencias_negado_quandoColaboradorTentaEditarOutro() {
        // Colaborador (perfil 3 → ROLE_USER) tenta editar competências de outro
        Funcionario logado = funcionarioComPerfilEArea(5, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(logado));

        Funcionario alvo = funcionarioComPerfilEArea(99, 3, 10);

        when(funcionarioRepository.findByIdCompleto(99)).thenReturn(Optional.of(alvo));

        assertThrows(UnauthorizedActionException.class,
                () -> funcionarioService.associarCompetencias(99, List.of(1)));
    }

    @Test
    void associarCompetencias_permitido_quandoGestorEditaColaboradorDaMesmaArea() {
        // Gestor (perfil 2) da área 10 edita colaborador (perfil 3) da área 10
        Funcionario gestor = funcionarioComPerfilEArea(2, 2, 10);
        setarUsuarioLogado(new CustomUserDetails(gestor));

        Funcionario alvo = funcionarioComPerfilEArea(50, 3, 10);

        Competencia c = new Competencia();
        c.setCodigo(1);

        when(funcionarioRepository.findByIdCompleto(50)).thenReturn(Optional.of(alvo));
        when(competenciaRepository.findAllById(List.of(1))).thenReturn(List.of(c));
        when(funcionarioRepository.save(any())).thenReturn(alvo);

        assertDoesNotThrow(() -> funcionarioService.associarCompetencias(50, List.of(1)));
    }

    @Test
    void associarCompetencias_negado_quandoGestorEditaColaboradorDeOutraArea() {
        Funcionario gestor = funcionarioComPerfilEArea(2, 2, 10);
        setarUsuarioLogado(new CustomUserDetails(gestor));

        Funcionario alvo = funcionarioComPerfilEArea(50, 3, 99); // Área diferente

        when(funcionarioRepository.findByIdCompleto(50)).thenReturn(Optional.of(alvo));

        assertThrows(UnauthorizedActionException.class,
                () -> funcionarioService.associarCompetencias(50, List.of(1)));
    }

    @Test
    void associarCompetencias_lancaResourceNotFound_quandoCompetenciaInvalida() {
        Funcionario logado = funcionarioComPerfilEArea(5, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(logado));

        when(funcionarioRepository.findByIdCompleto(5)).thenReturn(Optional.of(logado));
        // Retorna lista menor → competência 999 não existe
        when(competenciaRepository.findAllById(List.of(999))).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> funcionarioService.associarCompetencias(5, List.of(999)));
    }


    @Test
    void usuarioPodeRemoverCertificado_retornaTrue_quandoDono() {
        Funcionario dono = funcionarioSimples(1);
        FuncionarioCertificado cert = new FuncionarioCertificado();
        cert.setCodigo(10);
        cert.setFuncionario(dono);

        when(certificadoRepository.findById(10)).thenReturn(Optional.of(cert));

        assertTrue(funcionarioService.usuarioPodeRemoverCertificado(10, 1));
    }

    @Test
    void usuarioPodeRemoverCertificado_retornaFalse_quandoNaoDono() {
        Funcionario dono = funcionarioSimples(1);
        FuncionarioCertificado cert = new FuncionarioCertificado();
        cert.setCodigo(10);
        cert.setFuncionario(dono);

        when(certificadoRepository.findById(10)).thenReturn(Optional.of(cert));

        assertFalse(funcionarioService.usuarioPodeRemoverCertificado(10, 99));
    }

    @Test
    void usuarioPodeEditarExperiencia_retornaTrue_quandoDono() {
        Funcionario dono = funcionarioSimples(1);
        Experiencia exp = new Experiencia();
        exp.setCodigo(5);
        exp.setFuncionario(dono);

        when(experienciaRepository.findById(5)).thenReturn(Optional.of(exp));

        assertTrue(funcionarioService.usuarioPodeEditarExperiencia(5, 1));
    }

    @Test
    void usuarioPodeEditarExperiencia_retornaFalse_quandoNaoDono() {
        Funcionario dono = funcionarioSimples(1);
        Experiencia exp = new Experiencia();
        exp.setCodigo(5);
        exp.setFuncionario(dono);

        when(experienciaRepository.findById(5)).thenReturn(Optional.of(exp));

        assertFalse(funcionarioService.usuarioPodeEditarExperiencia(5, 42));
    }


    @Test
    void removerCertificado_deletaQuandoExiste() {
        when(certificadoRepository.existsById(1)).thenReturn(true);

        assertDoesNotThrow(() -> funcionarioService.removerCertificado(1));

        verify(certificadoRepository).deleteById(1);
    }

    @Test
    void removerCertificado_lancaResourceNotFound_quandoNaoExiste() {
        when(certificadoRepository.existsById(99)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> funcionarioService.removerCertificado(99));
    }


    @Test
    void adicionarExperiencia_salvaERetornaDTO() {
        Funcionario f = funcionarioSimples(1);
        ExperienciaRequestDTO dto = new ExperienciaRequestDTO(
                "Dev", "Empresa X", LocalDate.of(2022, 1, 1), null, "Descrição");

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(f));
        when(funcionarioRepository.save(any())).thenReturn(f);

        // O novo ExperienciaDTO é construído a partir da Experiencia; o serviço não
        // chama experienciaRepository.save() diretamente — salva via cascade.
        // Verificamos que o repositório foi chamado e retornou sem exceção.
        assertDoesNotThrow(() -> funcionarioService.adicionarExperiencia(1, dto));

        verify(funcionarioRepository).save(f);
    }

    @Test
    void adicionarExperiencia_lancaResourceNotFound_quandoFuncionarioNaoExiste() {
        ExperienciaRequestDTO dto = new ExperienciaRequestDTO(
                "Dev", "Empresa", LocalDate.of(2022, 1, 1), null, null);

        when(funcionarioRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> funcionarioService.adicionarExperiencia(99, dto));
    }
}