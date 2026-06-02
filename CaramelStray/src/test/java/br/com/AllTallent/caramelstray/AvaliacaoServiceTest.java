package br.com.AllTallent.caramelstray;

import br.com.AllTallent.config.CustomUserDetails;
import br.com.AllTallent.dto.*;
import br.com.AllTallent.exception.ResourceNotFoundException;
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

import java.util.HashSet;
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
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(ctx);

        assertThrows(UnauthorizedActionException.class,
                () -> avaliacaoService.listarTodasAvaliacoes());
    }

 
    @Test
    void listarTodasAvaliacoes_adminIgnoraAvaliacaoComCriadorSemArea() {
        Funcionario admin = funcionarioComPerfilEArea(1, 1, 10);
        setarUsuarioLogado(new CustomUserDetails(admin));

        Funcionario criadorSemArea = new Funcionario();
        criadorSemArea.setCodigo(2);
        criadorSemArea.setArea(null);

        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);
        aval.setTitulo("Sem área");
        aval.setCriador(criadorSemArea);

        when(avaliacaoRepository.findAll()).thenReturn(List.of(aval));

        List<AvaliacaoResponseDTO> resultado = avaliacaoService.listarTodasAvaliacoes();

        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarTodasAvaliacoes_gestorIgnoraAvaliacoesSemCriador() {
        Funcionario gestor = funcionarioComPerfilEArea(5, 2, 10);
        setarUsuarioLogado(new CustomUserDetails(gestor));

        Avaliacao semCriador = new Avaliacao();
        semCriador.setCodigo(1);
        semCriador.setCriador(null);

        when(avaliacaoRepository.findAll()).thenReturn(List.of(semCriador));

        List<AvaliacaoResponseDTO> resultado = avaliacaoService.listarTodasAvaliacoes();

        assertTrue(resultado.isEmpty());
    }

    @Test
    void criarAvaliacaoCompleta_salvaAvaliacaoEInstancias_quandoGestorTemPermissao() {
        Funcionario gestor = funcionarioComPerfilEArea(2, 2, 10);
        setarUsuarioLogado(new CustomUserDetails(gestor));

        Funcionario alvo = funcionarioComPerfilEArea(5, 3, 10);

        Pergunta pergunta = new Pergunta();
        pergunta.setCodigo(1L);

        AvaliacaoRequestDTO dto = new AvaliacaoRequestDTO(
                "Avaliação Anual", null, List.of(5), List.of(1L));

        Avaliacao avaliacaoSalva = new Avaliacao();
        avaliacaoSalva.setCodigo(99);
        avaliacaoSalva.setTitulo("Avaliação Anual");
        avaliacaoSalva.setCriador(gestor);

        when(funcionarioRepository.getReferenceById(2)).thenReturn(gestor);
        when(perguntaRepository.findAllById(List.of(1L))).thenReturn(List.of(pergunta));
        when(funcionarioRepository.findAllById(List.of(5))).thenReturn(List.of(alvo));
        when(avaliacaoRepository.save(any())).thenReturn(avaliacaoSalva);
        when(avaliacaoFuncionarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AvaliacaoResponseDTO resultado = avaliacaoService.criarAvaliacaoCompleta(dto);

        assertNotNull(resultado);
        verify(avaliacaoRepository).save(any());
        verify(avaliacaoFuncionarioRepository).save(any());
    }

    @Test
    void criarAvaliacaoCompleta_lancaEntityNotFound_quandoPerguntaNaoExiste() {
        Funcionario gestor = funcionarioComPerfilEArea(2, 2, 10);
        setarUsuarioLogado(new CustomUserDetails(gestor));

        AvaliacaoRequestDTO dto = new AvaliacaoRequestDTO(
                "Título", null, List.of(5), List.of(1L, 2L));

        when(funcionarioRepository.getReferenceById(2)).thenReturn(gestor);
        // findAllById retorna 1 item; dto pede 2 → tamanhos divergem
        when(perguntaRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(new Pergunta()));

        assertThrows(EntityNotFoundException.class,
                () -> avaliacaoService.criarAvaliacaoCompleta(dto));
    }

    @Test
    void criarAvaliacaoCompleta_lancaEntityNotFound_quandoFuncionarioNaoExiste() {
        Funcionario gestor = funcionarioComPerfilEArea(2, 2, 10);
        setarUsuarioLogado(new CustomUserDetails(gestor));

        Pergunta p = new Pergunta();
        p.setCodigo(1L);

        AvaliacaoRequestDTO dto = new AvaliacaoRequestDTO(
                "Título", null, List.of(5, 6), List.of(1L));

        when(funcionarioRepository.getReferenceById(2)).thenReturn(gestor);
        when(perguntaRepository.findAllById(List.of(1L))).thenReturn(List.of(p));
        // Retorna 1 funcionário; dto pede 2 → tamanhos divergem
        when(funcionarioRepository.findAllById(List.of(5, 6)))
                .thenReturn(List.of(funcionarioComPerfilEArea(5, 3, 10)));

        assertThrows(EntityNotFoundException.class,
                () -> avaliacaoService.criarAvaliacaoCompleta(dto));
    }

    @Test
    void criarAvaliacaoCompleta_lancaUnauthorized_quandoGestorAvaliaFuncionarioDeOutraArea() {
        Funcionario gestor = funcionarioComPerfilEArea(2, 2, 10);
        setarUsuarioLogado(new CustomUserDetails(gestor));

        Funcionario alvoOutraArea = funcionarioComPerfilEArea(5, 3, 99);

        Pergunta p = new Pergunta();
        p.setCodigo(1L);

        AvaliacaoRequestDTO dto = new AvaliacaoRequestDTO(
                "Título", null, List.of(5), List.of(1L));

        when(funcionarioRepository.getReferenceById(2)).thenReturn(gestor);
        when(perguntaRepository.findAllById(List.of(1L))).thenReturn(List.of(p));
        when(funcionarioRepository.findAllById(List.of(5))).thenReturn(List.of(alvoOutraArea));

        assertThrows(UnauthorizedActionException.class,
                () -> avaliacaoService.criarAvaliacaoCompleta(dto));
    }

    @Test
    void buscarAvaliacaoDetalhada_retornaDTO_quandoAdminDaMesmaArea() {
        Funcionario admin = funcionarioComPerfilEArea(1, 1, 10);
        setarUsuarioLogado(new CustomUserDetails(admin));

        Funcionario criador = funcionarioComPerfilEArea(2, 2, 10);
        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);
        aval.setTitulo("Avaliação");
        aval.setCriador(criador);
        aval.setPerguntas(new HashSet<>());

        when(avaliacaoRepository.findById(1)).thenReturn(Optional.of(aval));

        AvaliacaoDetalhadaDTO resultado = avaliacaoService.buscarAvaliacaoDetalhada(1);

        assertNotNull(resultado);
        assertEquals("Avaliação", resultado.titulo());
    }

    @Test
    void buscarAvaliacaoDetalhada_lancaResourceNotFound_quandoNaoExiste() {
        Funcionario admin = funcionarioComPerfilEArea(1, 1, 10);
        setarUsuarioLogado(new CustomUserDetails(admin));

        when(avaliacaoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> avaliacaoService.buscarAvaliacaoDetalhada(99));
    }

    @Test
    void buscarAvaliacaoDetalhada_lancaUnauthorized_quandoSemCriador() {
        Funcionario admin = funcionarioComPerfilEArea(1, 1, 10);
        setarUsuarioLogado(new CustomUserDetails(admin));

        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);
        aval.setCriador(null);

        when(avaliacaoRepository.findById(1)).thenReturn(Optional.of(aval));

        assertThrows(UnauthorizedActionException.class,
                () -> avaliacaoService.buscarAvaliacaoDetalhada(1));
    }

    @Test
    void buscarAvaliacaoDetalhada_lancaUnauthorized_quandoAreaDiferente() {
        Funcionario admin = funcionarioComPerfilEArea(1, 1, 10);
        setarUsuarioLogado(new CustomUserDetails(admin));

        Funcionario criador = funcionarioComPerfilEArea(2, 2, 99);
        Avaliacao aval = new Avaliacao();
        aval.setCodigo(2);
        aval.setCriador(criador);

        when(avaliacaoRepository.findById(2)).thenReturn(Optional.of(aval));

        assertThrows(UnauthorizedActionException.class,
                () -> avaliacaoService.buscarAvaliacaoDetalhada(2));
    }

    @Test
    void buscarAvaliacaoDetalhada_lancaUnauthorized_quandoGestorNaoCriadorDaAvaliacao() {
        Funcionario gestor = funcionarioComPerfilEArea(5, 2, 10);
        setarUsuarioLogado(new CustomUserDetails(gestor));

        Funcionario outroCriador = funcionarioComPerfilEArea(6, 2, 10);
        Avaliacao aval = new Avaliacao();
        aval.setCodigo(3);
        aval.setCriador(outroCriador);

        when(avaliacaoRepository.findById(3)).thenReturn(Optional.of(aval));

        assertThrows(UnauthorizedActionException.class,
                () -> avaliacaoService.buscarAvaliacaoDetalhada(3));
    }

    
    @Test
    void buscarInstanciasPorAvaliacao_retornaInstancias_quandoAdminDaMesmaArea() {
        Funcionario admin = funcionarioComPerfilEArea(1, 1, 10);
        setarUsuarioLogado(new CustomUserDetails(admin));

        Funcionario criador = funcionarioComPerfilEArea(2, 2, 10);
        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);
        aval.setCriador(criador);

        when(avaliacaoRepository.findById(1)).thenReturn(Optional.of(aval));
        when(avaliacaoFuncionarioRepository.findByAvaliacaoCodigo(1)).thenReturn(List.of());

        List<AvaliacaoFuncionarioResponseDTO> resultado =
                avaliacaoService.buscarInstanciasPorAvaliacao(1);

        assertNotNull(resultado);
        verify(avaliacaoFuncionarioRepository).findByAvaliacaoCodigo(1);
    }

    @Test
    void buscarInstanciasPorAvaliacao_lancaEntityNotFound_quandoAvaliacaoNaoExiste() {
        Funcionario admin = funcionarioComPerfilEArea(1, 1, 10);
        setarUsuarioLogado(new CustomUserDetails(admin));

        when(avaliacaoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> avaliacaoService.buscarInstanciasPorAvaliacao(99));
    }

    @Test
    void buscarInstanciasPorAvaliacao_lancaUnauthorized_quandoAreaDiferente() {
        Funcionario admin = funcionarioComPerfilEArea(1, 1, 10);
        setarUsuarioLogado(new CustomUserDetails(admin));

        Funcionario criador = funcionarioComPerfilEArea(2, 2, 99);
        Avaliacao aval = new Avaliacao();
        aval.setCodigo(5);
        aval.setCriador(criador);

        when(avaliacaoRepository.findById(5)).thenReturn(Optional.of(aval));

        assertThrows(UnauthorizedActionException.class,
                () -> avaliacaoService.buscarInstanciasPorAvaliacao(5));
    }


    @Test
    void buscarRespostasPorInstancia_retornaRespostas_quandoAdminDaMesmaArea() {
        Funcionario admin = funcionarioComPerfilEArea(1, 1, 10);
        setarUsuarioLogado(new CustomUserDetails(admin));

        Funcionario criador = funcionarioComPerfilEArea(2, 2, 10);
        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);
        aval.setCriador(criador);

        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario();
        instancia.setCodigo(10L);
        instancia.setAvaliacao(aval);

        when(avaliacaoFuncionarioRepository.findById(10L)).thenReturn(Optional.of(instancia));
        when(respostaColaboradorRepository.findByAvaliacaoFuncionarioCodigo(10L))
                .thenReturn(List.of());

        List<RespostaColaboradorResponseDTO> resultado =
                avaliacaoService.buscarRespostasPorInstancia(10L);

        assertNotNull(resultado);
        verify(respostaColaboradorRepository).findByAvaliacaoFuncionarioCodigo(10L);
    }

    @Test
    void buscarRespostasPorInstancia_lancaEntityNotFound_quandoInstanciaNotFound() {
        Funcionario admin = funcionarioComPerfilEArea(1, 1, 10);
        setarUsuarioLogado(new CustomUserDetails(admin));

        when(avaliacaoFuncionarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> avaliacaoService.buscarRespostasPorInstancia(99L));
    }

    @Test
    void buscarRespostasPorInstancia_lancaUnauthorized_quandoAreaDiferente() {
        Funcionario admin = funcionarioComPerfilEArea(1, 1, 10);
        setarUsuarioLogado(new CustomUserDetails(admin));

        Funcionario criador = funcionarioComPerfilEArea(2, 2, 99);
        Avaliacao aval = new Avaliacao();
        aval.setCodigo(7);
        aval.setCriador(criador);

        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario();
        instancia.setCodigo(20L);
        instancia.setAvaliacao(aval);

        when(avaliacaoFuncionarioRepository.findById(20L)).thenReturn(Optional.of(instancia));

        assertThrows(UnauthorizedActionException.class,
                () -> avaliacaoService.buscarRespostasPorInstancia(20L));
    }


    @Test
    void salvarOuAtualizarResposta_lancaEntityNotFound_quandoInstanciaNotFound() {
        Funcionario f = funcionarioComPerfilEArea(5, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(f));

        when(avaliacaoFuncionarioRepository.findById(99L)).thenReturn(Optional.empty());

        RespostaColaboradorRequestDTO dto = new RespostaColaboradorRequestDTO(99L, 1L, "texto", null);

        assertThrows(EntityNotFoundException.class,
                () -> avaliacaoService.salvarOuAtualizarResposta(dto));
    }

    @Test
    void salvarOuAtualizarResposta_lancaEntityNotFound_quandoPerguntaNaoExiste() {
        Funcionario f = funcionarioComPerfilEArea(5, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(f));

        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);
        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario(f, aval);
        instancia.setCodigo(1L);

        when(avaliacaoFuncionarioRepository.findById(1L)).thenReturn(Optional.of(instancia));
        when(perguntaRepository.findById(99L)).thenReturn(Optional.empty());

        RespostaColaboradorRequestDTO dto = new RespostaColaboradorRequestDTO(1L, 99L, "texto", null);

        assertThrows(EntityNotFoundException.class,
                () -> avaliacaoService.salvarOuAtualizarResposta(dto));
    }

    @Test
    void salvarOuAtualizarResposta_comOpcao_salva_quandoOpcaoPertenceAPergunta() {
        Funcionario f = funcionarioComPerfilEArea(5, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(f));

        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);
        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario(f, aval);
        instancia.setCodigo(1L);

        Pergunta pergunta = new Pergunta();
        pergunta.setCodigo(10L);

        PerguntaOpcao opcao = new PerguntaOpcao();
        opcao.setCodigo(20L);
        opcao.setPergunta(pergunta);

        RespostaColaborador respostaSalva = new RespostaColaborador();
        respostaSalva.setCodigo(1L);
        respostaSalva.setAvaliacaoFuncionario(instancia);
        respostaSalva.setPergunta(pergunta);
        respostaSalva.setOpcaoSelecionada(opcao);

        when(avaliacaoFuncionarioRepository.findById(1L)).thenReturn(Optional.of(instancia));
        when(perguntaRepository.findById(10L)).thenReturn(Optional.of(pergunta));
        when(perguntaOpcaoRepository.findById(20L)).thenReturn(Optional.of(opcao));
        when(respostaColaboradorRepository
                .findByFuncionarioAvaliacaoCodigoAndPerguntaCodigo(1L, 10L))
                .thenReturn(Optional.empty());
        when(respostaColaboradorRepository.save(any())).thenReturn(respostaSalva);

        RespostaColaboradorRequestDTO dto = new RespostaColaboradorRequestDTO(1L, 10L, null, 20L);

        RespostaColaboradorResponseDTO resultado = avaliacaoService.salvarOuAtualizarResposta(dto);

        assertNotNull(resultado);
        verify(perguntaOpcaoRepository).findById(20L);
    }

    @Test
    void salvarOuAtualizarResposta_lancaEntityNotFound_quandoOpcaoNaoExiste() {
        Funcionario f = funcionarioComPerfilEArea(5, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(f));

        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);
        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario(f, aval);
        instancia.setCodigo(1L);

        Pergunta pergunta = new Pergunta();
        pergunta.setCodigo(10L);

        when(avaliacaoFuncionarioRepository.findById(1L)).thenReturn(Optional.of(instancia));
        when(perguntaRepository.findById(10L)).thenReturn(Optional.of(pergunta));
        when(perguntaOpcaoRepository.findById(99L)).thenReturn(Optional.empty());

        RespostaColaboradorRequestDTO dto = new RespostaColaboradorRequestDTO(1L, 10L, null, 99L);

        assertThrows(EntityNotFoundException.class,
                () -> avaliacaoService.salvarOuAtualizarResposta(dto));
    }

    @Test
    void salvarOuAtualizarResposta_lancaIllegalArgument_quandoOpcaoPertenceOutraPergunta() {
        Funcionario f = funcionarioComPerfilEArea(5, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(f));

        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);
        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario(f, aval);
        instancia.setCodigo(1L);

        Pergunta perguntaCorreta = new Pergunta();
        perguntaCorreta.setCodigo(10L);

        Pergunta outraPergunta = new Pergunta();
        outraPergunta.setCodigo(99L);

        PerguntaOpcao opcaoDeOutraPergunta = new PerguntaOpcao();
        opcaoDeOutraPergunta.setCodigo(20L);
        opcaoDeOutraPergunta.setPergunta(outraPergunta);

        when(avaliacaoFuncionarioRepository.findById(1L)).thenReturn(Optional.of(instancia));
        when(perguntaRepository.findById(10L)).thenReturn(Optional.of(perguntaCorreta));
        when(perguntaOpcaoRepository.findById(20L)).thenReturn(Optional.of(opcaoDeOutraPergunta));

        RespostaColaboradorRequestDTO dto = new RespostaColaboradorRequestDTO(1L, 10L, null, 20L);

        assertThrows(IllegalArgumentException.class,
                () -> avaliacaoService.salvarOuAtualizarResposta(dto));
    }

    @Test
    void salvarOuAtualizarResposta_atualizaRespostaExistente() {
        Funcionario f = funcionarioComPerfilEArea(5, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(f));

        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);
        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario(f, aval);
        instancia.setCodigo(1L);

        Pergunta pergunta = new Pergunta();
        pergunta.setCodigo(10L);

        RespostaColaborador respostaExistente = new RespostaColaborador();
        respostaExistente.setCodigo(55L);
        respostaExistente.setRespostaTexto("Resposta antiga");

        RespostaColaborador respostaAtualizada = new RespostaColaborador();
        respostaAtualizada.setCodigo(55L);
        respostaAtualizada.setRespostaTexto("Nova resposta");
        respostaAtualizada.setAvaliacaoFuncionario(instancia);
        respostaAtualizada.setPergunta(pergunta);

        when(avaliacaoFuncionarioRepository.findById(1L)).thenReturn(Optional.of(instancia));
        when(perguntaRepository.findById(10L)).thenReturn(Optional.of(pergunta));
        when(respostaColaboradorRepository
                .findByFuncionarioAvaliacaoCodigoAndPerguntaCodigo(1L, 10L))
                .thenReturn(Optional.of(respostaExistente));
        when(respostaColaboradorRepository.save(respostaExistente)).thenReturn(respostaAtualizada);

        RespostaColaboradorRequestDTO dto =
                new RespostaColaboradorRequestDTO(1L, 10L, "Nova resposta", null);

        RespostaColaboradorResponseDTO resultado = avaliacaoService.salvarOuAtualizarResposta(dto);

        assertNotNull(resultado);
        verify(respostaColaboradorRepository).save(respostaExistente);
    }


    @Test
    void salvarRevisaoSupervisor_salva_quandoGestorPodeAvaliarFuncionario() {
        Funcionario gestor = funcionarioComPerfilEArea(2, 2, 10);
        setarUsuarioLogado(new CustomUserDetails(gestor));

        Funcionario colaborador = funcionarioComPerfilEArea(5, 3, 10);
        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);

        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario(colaborador, aval);
        instancia.setCodigo(10L);
        instancia.setResultadoStatus("AGUARDANDO_REVISAO");

        RevisaoSupervisorRequestDTO dto = new RevisaoSupervisorRequestDTO(
                "Bom desempenho", "Parabéns", "APROVADO");

        when(avaliacaoFuncionarioRepository.findById(10L)).thenReturn(Optional.of(instancia));
        when(avaliacaoFuncionarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AvaliacaoFuncionarioResponseDTO resultado =
                avaliacaoService.salvarRevisaoSupervisor(10L, dto);

        assertNotNull(resultado);
        assertEquals("APROVADO", instancia.getResultadoStatus());
        verify(avaliacaoFuncionarioRepository).save(instancia);
    }

    @Test
    void salvarRevisaoSupervisor_lancaEntityNotFound_quandoInstanciaNotFound() {
        Funcionario gestor = funcionarioComPerfilEArea(2, 2, 10);
        setarUsuarioLogado(new CustomUserDetails(gestor));

        when(avaliacaoFuncionarioRepository.findById(99L)).thenReturn(Optional.empty());

        RevisaoSupervisorRequestDTO dto =
                new RevisaoSupervisorRequestDTO("comentario", null, "APROVADO");

        assertThrows(EntityNotFoundException.class,
                () -> avaliacaoService.salvarRevisaoSupervisor(99L, dto));
    }

    @Test
    void salvarRevisaoSupervisor_lancaUnauthorized_quandoGestorNaoPodeAvaliar() {
        Funcionario gestor = funcionarioComPerfilEArea(2, 2, 10);
        setarUsuarioLogado(new CustomUserDetails(gestor));

        Funcionario alvoOutraArea = funcionarioComPerfilEArea(5, 3, 99);
        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);

        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario(alvoOutraArea, aval);
        instancia.setCodigo(10L);

        when(avaliacaoFuncionarioRepository.findById(10L)).thenReturn(Optional.of(instancia));

        RevisaoSupervisorRequestDTO dto =
                new RevisaoSupervisorRequestDTO("comentario", null, "APROVADO");

        assertThrows(UnauthorizedActionException.class,
                () -> avaliacaoService.salvarRevisaoSupervisor(10L, dto));
    }

    @Test
    void buscarParaResponder_retornaDTO_quandoProprioFuncionario() {
        Funcionario f = funcionarioComPerfilEArea(5, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(f));

        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);
        aval.setTitulo("Avaliação X");
        aval.setPerguntas(new HashSet<>());

        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario(f, aval);
        instancia.setCodigo(10L);

        when(avaliacaoFuncionarioRepository.findById(10L)).thenReturn(Optional.of(instancia));

        AvaliacaoParaResponderDTO resultado = avaliacaoService.buscarParaResponder(10L);

        assertNotNull(resultado);
        assertEquals(10L, resultado.avaliacaoFuncionarioCodigo());
    }

    @Test
    void buscarParaResponder_lancaEntityNotFound_quandoInstanciaNotFound() {
        Funcionario f = funcionarioComPerfilEArea(5, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(f));

        when(avaliacaoFuncionarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> avaliacaoService.buscarParaResponder(99L));
    }

    @Test
    void buscarParaResponder_lancaUnauthorized_quandoOutroFuncionario() {
        Funcionario logado = funcionarioComPerfilEArea(5, 3, 10);
        setarUsuarioLogado(new CustomUserDetails(logado));

        Funcionario dono = funcionarioComPerfilEArea(99, 3, 10);
        Avaliacao aval = new Avaliacao();
        aval.setCodigo(1);

        AvaliacaoFuncionario instancia = new AvaliacaoFuncionario(dono, aval);
        instancia.setCodigo(10L);

        when(avaliacaoFuncionarioRepository.findById(10L)).thenReturn(Optional.of(instancia));

        assertThrows(UnauthorizedActionException.class,
                () -> avaliacaoService.buscarParaResponder(10L));
    }

 
    @Test
    void buscarDadosRevisao_retornaDTOs_comOpcaoSelecionadaNula() {
        Pergunta pergunta = new Pergunta();
        pergunta.setCodigo(10L);
        pergunta.setEnunciado("Como foi seu desempenho?");

        RespostaColaborador resposta = new RespostaColaborador();
        resposta.setCodigo(1L);
        resposta.setPergunta(pergunta);
        resposta.setRespostaTexto("Bom");
        resposta.setPerguntaOpcaoSelecionada(null);

        when(avaliacaoFuncionarioRepository.existsById(1L)).thenReturn(true);
        when(respostaColaboradorRepository.findByAvaliacaoFuncionarioCodigo(1L))
                .thenReturn(List.of(resposta));

        List<RevisaoDetalhadaDTO> resultado = avaliacaoService.buscarDadosRevisao(1L);

        assertEquals(1, resultado.size());
        assertEquals("Como foi seu desempenho?", resultado.get(0).getPerguntaTexto());
        assertEquals("Bom", resultado.get(0).getRespostaDada());
        assertNull(resultado.get(0).getOpcaoSelecionadaId());
    }

    @Test
    void buscarDadosRevisao_retornaDTOs_comOpcaoSelecionadaPreenchida() {
        Pergunta pergunta = new Pergunta();
        pergunta.setCodigo(10L);
        pergunta.setEnunciado("Qual opção?");

        PerguntaOpcao opcao = new PerguntaOpcao();
        opcao.setCodigo(5L);

        RespostaColaborador resposta = new RespostaColaborador();
        resposta.setCodigo(1L);
        resposta.setPergunta(pergunta);
        resposta.setRespostaTexto(null);
        resposta.setPerguntaOpcaoSelecionada(opcao);

        when(avaliacaoFuncionarioRepository.existsById(2L)).thenReturn(true);
        when(respostaColaboradorRepository.findByAvaliacaoFuncionarioCodigo(2L))
                .thenReturn(List.of(resposta));

        List<RevisaoDetalhadaDTO> resultado = avaliacaoService.buscarDadosRevisao(2L);

        assertEquals(1, resultado.size());
        assertEquals(5L, resultado.get(0).getOpcaoSelecionadaId());
    }
}