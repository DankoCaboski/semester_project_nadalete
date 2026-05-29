package br.com.AllTallent.caramelstray;

import br.com.AllTallent.dto.CadastroRequestDTO;
import br.com.AllTallent.model.Area;
import br.com.AllTallent.model.Funcionario;
import br.com.AllTallent.model.Perfil;
import br.com.AllTallent.repository.AreaRepository;
import br.com.AllTallent.repository.FuncionarioRepository;
import br.com.AllTallent.repository.PerfilRepository;
import br.com.AllTallent.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private AreaRepository areaRepository;
    @Mock private PerfilRepository perfilRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /** Cria um DTO de cadastro minimamente preenchido. */
    private CadastroRequestDTO dto(String email, Integer codigoArea, Integer codigoPerfil) {
        CadastroRequestDTO d = new CadastroRequestDTO();
        d.setNomeCompleto("Funcionário Teste");
        d.setEmail(email);
        d.setSenha("senha123");
        d.setCpf("12345678901");
        d.setIdCracha("CRACHA-01");
        d.setCodigoArea(codigoArea);
        d.setCodigoPerfil(codigoPerfil);
        d.setDataAdmissao(LocalDate.of(2024, 1, 15));
        return d;
    }

    private Area area(int codigo) {
        Area a = new Area();
        a.setCodigo(codigo);
        a.setNome("Área " + codigo);
        return a;
    }

    private Perfil perfil(int codigo) {
        Perfil p = new Perfil();
        p.setCodigo(codigo);
        p.setNome("Perfil " + codigo);
        return p;
    }

    // =========================================================================
    // Validação de email duplicado
    // =========================================================================

    @Test
    void register_lancaExcecao_quandoEmailJaExiste() {
        CadastroRequestDTO d = dto("existente@test.com", 1, 3);

        when(funcionarioRepository.findByEmail("existente@test.com"))
                .thenReturn(Optional.of(new Funcionario()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(d));

        assertTrue(ex.getMessage().contains("Email"));
        verify(funcionarioRepository, never()).save(any());
    }

    // =========================================================================
    // Relacionamentos obrigatórios
    // =========================================================================

    @Test
    void register_lancaExcecao_quandoAreaNaoEncontrada() {
        CadastroRequestDTO d = dto("novo@test.com", 99, 3);

        when(funcionarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(areaRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(d));

        assertTrue(ex.getMessage().contains("Área"));
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    void register_lancaExcecao_quandoPerfilNaoEncontrado() {
        CadastroRequestDTO d = dto("novo@test.com", 1, 99);

        when(funcionarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(areaRepository.findById(1)).thenReturn(Optional.of(area(1)));
        when(perfilRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(d));

        assertTrue(ex.getMessage().contains("Perfil"));
        verify(funcionarioRepository, never()).save(any());
    }

    // =========================================================================
    // Cadastro bem-sucedido sem gestor
    // =========================================================================

    @Test
    void register_salvaFuncionario_semGestor() {
        CadastroRequestDTO d = dto("novo@test.com", 1, 3);
        Funcionario esperado = new Funcionario();
        esperado.setCodigo(10);

        when(funcionarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(areaRepository.findById(1)).thenReturn(Optional.of(area(1)));
        when(perfilRepository.findById(3)).thenReturn(Optional.of(perfil(3)));
        when(passwordEncoder.encode("senha123")).thenReturn("hash");
        when(funcionarioRepository.save(any())).thenReturn(esperado);

        Funcionario resultado = authService.register(d);

        assertNotNull(resultado);
        assertEquals(10, resultado.getCodigo());
        verify(funcionarioRepository).save(any());
    }

    @Test
    void register_criptografaSenha() {
        CadastroRequestDTO d = dto("novo@test.com", 1, 3);

        when(funcionarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(areaRepository.findById(1)).thenReturn(Optional.of(area(1)));
        when(perfilRepository.findById(3)).thenReturn(Optional.of(perfil(3)));
        when(passwordEncoder.encode("senha123")).thenReturn("hash-criptografado");
        when(funcionarioRepository.save(any())).thenReturn(new Funcionario());

        authService.register(d);

        verify(passwordEncoder).encode("senha123");

        ArgumentCaptor<Funcionario> captor = ArgumentCaptor.forClass(Funcionario.class);
        verify(funcionarioRepository).save(captor.capture());
        assertEquals("hash-criptografado", captor.getValue().getSenhaHash());
    }

    @Test
    void register_mapeiaDataDeCadastroAutomaticamente() {
        CadastroRequestDTO d = dto("novo@test.com", 1, 3);

        when(funcionarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(areaRepository.findById(1)).thenReturn(Optional.of(area(1)));
        when(perfilRepository.findById(3)).thenReturn(Optional.of(perfil(3)));
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(funcionarioRepository.save(any())).thenReturn(new Funcionario());

        authService.register(d);

        ArgumentCaptor<Funcionario> captor = ArgumentCaptor.forClass(Funcionario.class);
        verify(funcionarioRepository).save(captor.capture());
        assertNotNull(captor.getValue().getDataCadastro());
    }

    @Test
    void register_mapeiaAreaEPerfilNaEntidade() {
        CadastroRequestDTO d = dto("novo@test.com", 5, 2);
        Area area = area(5);
        Perfil p = perfil(2);

        when(funcionarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(areaRepository.findById(5)).thenReturn(Optional.of(area));
        when(perfilRepository.findById(2)).thenReturn(Optional.of(p));
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(funcionarioRepository.save(any())).thenReturn(new Funcionario());

        authService.register(d);

        ArgumentCaptor<Funcionario> captor = ArgumentCaptor.forClass(Funcionario.class);
        verify(funcionarioRepository).save(captor.capture());
        assertEquals(area, captor.getValue().getArea());
        assertEquals(p, captor.getValue().getPerfil());
    }

    // =========================================================================
    // Cadastro com gestor
    // =========================================================================

    @Test
    void register_salvaFuncionario_comGestor() {
        CadastroRequestDTO d = dto("novo@test.com", 1, 3);
        d.setCodigoGestor(50);

        Funcionario gestor = new Funcionario();
        gestor.setCodigo(50);

        when(funcionarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(areaRepository.findById(1)).thenReturn(Optional.of(area(1)));
        when(perfilRepository.findById(3)).thenReturn(Optional.of(perfil(3)));
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(funcionarioRepository.findById(50)).thenReturn(Optional.of(gestor));
        when(funcionarioRepository.save(any())).thenReturn(new Funcionario());

        authService.register(d);

        ArgumentCaptor<Funcionario> captor = ArgumentCaptor.forClass(Funcionario.class);
        verify(funcionarioRepository).save(captor.capture());
        assertEquals(gestor, captor.getValue().getGestor());
    }

    @Test
    void register_lancaExcecao_quandoGestorNaoEncontrado() {
        CadastroRequestDTO d = dto("novo@test.com", 1, 3);
        d.setCodigoGestor(999);

        when(funcionarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(areaRepository.findById(1)).thenReturn(Optional.of(area(1)));
        when(perfilRepository.findById(3)).thenReturn(Optional.of(perfil(3)));
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(funcionarioRepository.findById(999)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(d));

        assertTrue(ex.getMessage().contains("Gestor"));
        verify(funcionarioRepository, never()).save(any());
    }
}
