package br.com.AllTallent.caramelstray;

import br.com.AllTallent.config.CustomUserDetails;
import br.com.AllTallent.dto.*;
import br.com.AllTallent.exception.UnauthorizedActionException;
import br.com.AllTallent.model.*;
import br.com.AllTallent.repository.*;
import br.com.AllTallent.service.AvaliacaoService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTest {

    @Mock private AvaliacaoRepository avaliacaoRepository;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private PerguntaRepository perguntaRepository;
    @Mock private AvaliacaoFuncionarioRepository avaliacaoFuncionarioRepository;
    @Mock private RespostaColaboradorRepository respostaColaboradorRepository;
    @Mock private PerguntaOpcaoRepository perguntaOpcaoRepository;

    @InjectMocks
    private AvaliacaoService avaliacaoService;

    // -------------------------------------------------------------------------
    // Helpers para montar contexto de segurança sem mocks inline
    // -------------------------------------------------------------------------

    /** Monta um Funcionario com perfil e área. */
    private Funcionario funcionarioComPerfilEArea(int codigo, int perfilCodigo, int areaCodigo) {
        Area area = new Area();
        area.setCodigo(areaCodigo);

        Perfil perfil = new Perfil();
        perfil.setCodigo(perfilCodigo);

        Funcionario f = new Funcionario();
        f.setCodigo(codigo);
        f.setEmail("f" + codigo + "@test.com");
        f.setSenhaHash("hash");
        f.setPerfil(perfil);
        f.setArea(area);
        return f;
    }

    /** Injeta um CustomUserDetails no SecurityContextHolder para a duração do teste. */
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
    void listarTodasAvaliacoes_adminVeAvaliacoesDaMesmaArea() {
        // Admin (perfil 1) na área 10
        Funcionario admin = funcionarioComPerfilEArea(1, 1, 10);
        setarUsuarioLogado(new CustomUserDetails(admin));

        // Avaliação criada por alguém da área 10
        Funcionario criador = funcionarioComPerfilEArea(2, 2, 10);
        Avaliacao aval = new Avaliacao();
        aval.setCodigo(100);
        aval.setTitulo("Avaliação X");
        aval.setCriador(criador);

        // Avaliação de outra área — não deve aparecer
        Funcionario outraArea = funcionarioComPerfilEArea(3, 3, 99);
        Avaliacao outra = new Avaliacao();
        outra.setCodigo(200);
        outra.setTitulo("Outra");
        outra.setCriador(outraArea);

        when(avaliacaoRepository.findAll()).thenReturn(List.of(aval, outra));

        List<AvaliacaoResponseDTO> resultado = avaliacaoService.listarTodasAvaliacoes();

        assertEquals(1, resultado.size());
        assertEquals("Avaliação X", resultado.get(0).titulo());
    }

    @Test
    void listarTodasAvaliacoes_gestorVeApenasAsSuasAvaliacoes() {
        // Gestor (perfil 2) na área 10
        Funcionario gestor = funcionarioComPerfilEArea(5, 2, 10);
        setarUsuarioLogado(new CustomUserDetails(gestor));

        Avaliacao dele = new Avaliacao();
        dele.setCodigo(1);
        dele.setTitulo("Do gestor");
        dele.setCriador(gestor);

        // Avaliação criada por outro da mesma área — gestor NÃO vê
        Funcionario outro = funcionarioComPerfilEArea(6, 2, 10);
        Avaliacao deOutro = new Avaliacao();
        deOutro.setCodigo(2);
        deOutro.setTitulo("De outro");
        deOutro.setCriador(outro);

        when(avaliacaoRepository.findAll()).thenReturn(List.of(dele, deOutro));

        List<AvaliacaoResponseDTO> resultado = avaliacaoService.listarTodasAvaliacoes();

        assertEquals(1, resultado.size());
        assertEquals("Do gestor", resultado.get(0).titulo());
    }

    @Test
    void listarTodasAvaliacoes_usuarioComumRetornaListaVazia() {
        // Perfil 3 → ROLE_USER somente
        Funcionario user = funcionarioComPerfilEArea(7, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(user));

        when(avaliacaoRepository.findAll()).thenReturn(List.of());

        List<AvaliacaoResponseDTO> resultado = avaliacaoService.listarTodasAvaliacoes();

        assertTrue(resultado.isEmpty());
        verify(avaliacaoRepository).findAll(); // foi chamado, mas retorno filtrado
    }

    @Test
    void listarTodasAvaliacoes_ignoraAvaliacoesSemCriador() {
        Funcionario admin = funcionarioComPerfilEArea(1, 1, 10);
        setarUsuarioLogado(new CustomUserDetails(admin));

        Avaliacao semCriador = new Avaliacao();
        semCriador.setCodigo(99);
        semCriador.setTitulo("Sem criador");
        semCriador.setCriador(null);

        when(avaliacaoRepository.findAll()).thenReturn(List.of(semCriador));

        List<AvaliacaoResponseDTO> resultado = avaliacaoService.listarTodasAvaliacoes();

        assertTrue(resultado.isEmpty());
    }


    @Test
    void buscarPendentesPorFuncionario_retornaApenasComStatusPendente() {
        Funcionario f = new Funcionario();
        f.setCodigo(1);

        Avaliacao aval = new Avaliacao();
        aval.setCodigo(10);
        aval.setTitulo("Teste");

        AvaliacaoFuncionario pendente = new AvaliacaoFuncionario(f, aval);
        pendente.setCodigo(1L);
        pendente.setResultadoStatus("PENDENTE");

        AvaliacaoFuncionario concluido = new AvaliacaoFuncionario(f, aval);
        concluido.setCodigo(2L);
        concluido.setResultadoStatus("CONCLUIDO");

        when(avaliacaoFuncionarioRepository.findByFuncionarioCodigo(1))
                .thenReturn(List.of(pendente, concluido));

        List<AvaliacaoFuncionarioResponseDTO> resultado =
                avaliacaoService.buscarPendentesPorFuncionario(1);

        assertEquals(1, resultado.size());
    }

    @Test
    void buscarPendentesPorFuncionario_retornaVazioSeNaoHaPendentes() {
        when(avaliacaoFuncionarioRepository.findByFuncionarioCodigo(99))
                .thenReturn(List.of());

        List<AvaliacaoFuncionarioResponseDTO> resultado =
                avaliacaoService.buscarPendentesPorFuncionario(99);

        assertTrue(resultado.isEmpty());
    }


    @Test
    void finalizarPeloColaborador_alteraStatusParaAguardandoRevisao_quandoPendente() {
        Funcionario f = funcionarioComPerfilEArea(3, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(f));

        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);

        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario(f, aval);
        instancia.setCodigo(10L);
        instancia.setResultadoStatus("PENDENTE");

        when(avaliacaoFuncionarioRepository.findById(10L)).thenReturn(Optional.of(instancia));
        when(avaliacaoFuncionarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> avaliacaoService.finalizarPeloColaborador(10L));

        assertEquals("AGUARDANDO_REVISAO", instancia.getResultadoStatus());
        verify(avaliacaoFuncionarioRepository).save(instancia);
    }

    @Test
    void finalizarPeloColaborador_lancaIllegalState_quandoNaoPendente() {
        Funcionario f = funcionarioComPerfilEArea(3, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(f));

        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);

        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario(f, aval);
        instancia.setCodigo(20L);
        instancia.setResultadoStatus("CONCLUIDO");

        when(avaliacaoFuncionarioRepository.findById(20L)).thenReturn(Optional.of(instancia));

        assertThrows(IllegalStateException.class,
                () -> avaliacaoService.finalizarPeloColaborador(20L));
    }

    @Test
    void finalizarPeloColaborador_lancaUnauthorized_quandoOutroFuncionario() {
        // Usuário logado é o funcionário 3, mas a instância pertence ao funcionário 99
        Funcionario logado = funcionarioComPerfilEArea(3, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(logado));

        Funcionario dono = funcionarioComPerfilEArea(99, 3, 10);
        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);

        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario(dono, aval);
        instancia.setCodigo(30L);
        instancia.setResultadoStatus("PENDENTE");

        when(avaliacaoFuncionarioRepository.findById(30L)).thenReturn(Optional.of(instancia));

        assertThrows(UnauthorizedActionException.class,
                () -> avaliacaoService.finalizarPeloColaborador(30L));
    }

    @Test
    void finalizarPeloColaborador_lancaEntityNotFound_quandoInstanciaNaoExiste() {
        Funcionario f = funcionarioComPerfilEArea(3, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(f));

        when(avaliacaoFuncionarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> avaliacaoService.finalizarPeloColaborador(999L));
    }


    @Test
    void salvarOuAtualizarResposta_salvaNovamente_quandoNaoExisteResposta() {
        Funcionario f = funcionarioComPerfilEArea(5, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(f));

        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);

        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario(f, aval);
        instancia.setCodigo(1L);

        Pergunta pergunta = new Pergunta();
        pergunta.setCodigo(10L);

        RespostaColaborador resposta = new RespostaColaborador();
        resposta.setCodigo(100L);
        resposta.setAvaliacaoFuncionario(instancia);
        resposta.setPergunta(pergunta);
        resposta.setRespostaTexto("Minha resposta");

        RespostaColaboradorRequestDTO dto =
                new RespostaColaboradorRequestDTO(1L, 10L, "Minha resposta", null);

        when(avaliacaoFuncionarioRepository.findById(1L)).thenReturn(Optional.of(instancia));
        when(perguntaRepository.findById(10L)).thenReturn(Optional.of(pergunta));
        when(respostaColaboradorRepository
                .findByFuncionarioAvaliacaoCodigoAndPerguntaCodigo(1L, 10L))
                .thenReturn(Optional.empty());
        when(respostaColaboradorRepository.save(any())).thenReturn(resposta);

        RespostaColaboradorResponseDTO resultado =
                avaliacaoService.salvarOuAtualizarResposta(dto);

        assertNotNull(resultado);
        verify(respostaColaboradorRepository).save(any());
    }

    @Test
    void salvarOuAtualizarResposta_lancaUnauthorized_quandoInstanciaDeOutroFuncionario() {
        Funcionario logado = funcionarioComPerfilEArea(5, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(logado));

        Funcionario dono = funcionarioComPerfilEArea(99, 3, 10);
        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);

        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario(dono, aval);
        instancia.setCodigo(1L);

        when(avaliacaoFuncionarioRepository.findById(1L)).thenReturn(Optional.of(instancia));

        RespostaColaboradorRequestDTO dto =
                new RespostaColaboradorRequestDTO(1L, 10L, "resposta", null);

        assertThrows(UnauthorizedActionException.class,
                () -> avaliacaoService.salvarOuAtualizarResposta(dto));
    }


    @Test
    void buscarDadosRevisao_lancaEntityNotFound_quandoInstanciaNaoExiste() {
        when(avaliacaoFuncionarioRepository.existsById(99L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class,
                () -> avaliacaoService.buscarDadosRevisao(99L));
    }

    @Test
    void buscarDadosRevisao_retornaListaVazia_quandoSemRespostas() {
        when(avaliacaoFuncionarioRepository.existsById(1L)).thenReturn(true);
        when(respostaColaboradorRepository.findByAvaliacaoFuncionarioCodigo(1L))
                .thenReturn(List.of());

        List<RevisaoDetalhadaDTO> resultado = avaliacaoService.buscarDadosRevisao(1L);

        assertTrue(resultado.isEmpty());
    }


    @Test
    void listarTodasAvaliacoes_lancaUnauthorized_quandoSemAutenticacao() {
        // SecurityContext vazio (sem Authentication): getUsuarioLogado() lança antes de
        // chegar em findAll(), portanto nenhum stub de repositório é necessário.
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(ctx);

        assertThrows(UnauthorizedActionException.class,
                () -> avaliacaoService.listarTodasAvaliacoes());
    }
}