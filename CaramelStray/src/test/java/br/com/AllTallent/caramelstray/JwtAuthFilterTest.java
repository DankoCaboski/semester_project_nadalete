package br.com.AllTallent.caramelstray;

import br.com.AllTallent.config.JwtAuthFilter;
import br.com.AllTallent.config.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import io.jsonwebtoken.JwtException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtService, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private UserDetails userDetails(String email) {
        return User.withUsername(email)
                .password("irrelevante")
                .authorities(Collections.emptyList())
                .build();
    }

    // =========================================================================
    // Sem header Authorization — não deve interferir no fluxo
    // =========================================================================

    @Test
    void semHeaderAuthorization_encaminhaRequisicaoAdiante() throws Exception {
        MockHttpServletRequest request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void headerSemPrefixoBearer_encaminhaRequisicaoAdiante() throws Exception {
        MockHttpServletRequest request  = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // =========================================================================
    // Token JWT válido — deve setar autenticação no contexto
    // =========================================================================

    @Test
    void tokenValido_setaAutenticacaoNoContexto() throws Exception {
        UserDetails ud = userDetails("alice@test.com");
        String fakeToken = "header.payload.signature";

        MockHttpServletRequest request  = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + fakeToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername(fakeToken)).thenReturn("alice@test.com");
        when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(ud);
        when(jwtService.isTokenValid(fakeToken, ud)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("alice@test.com",
                SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void tokenComIsTokenValidFalso_naoSetaAutenticacao() throws Exception {
        UserDetails ud = userDetails("bob@test.com");
        String fakeToken = "header.payload.signature";

        MockHttpServletRequest request  = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + fakeToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername(fakeToken)).thenReturn("bob@test.com");
        when(userDetailsService.loadUserByUsername("bob@test.com")).thenReturn(ud);
        when(jwtService.isTokenValid(fakeToken, ud)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void tokenComEmailNulo_naoCarregaUsuario() throws Exception {
        String fakeToken = "header.payload.signature";

        MockHttpServletRequest request  = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + fakeToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername(fakeToken)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(userDetailsService, never()).loadUserByUsername(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // =========================================================================
    // Exceções — o filtro trata internamente sem propagar
    // =========================================================================

    @Test
    void jwtInvalido_retorna401_semPropagar() throws Exception {
        String tokenInvalido = "token.invalido.assinatura";

        MockHttpServletRequest request  = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenInvalido);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername(tokenInvalido))
                .thenThrow(new JwtException("Token inválido"));

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void excecaoGenerica_retorna500_semPropagar() throws Exception {
        String fakeToken = "header.payload.signature";

        MockHttpServletRequest request  = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + fakeToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername(fakeToken))
                .thenThrow(new RuntimeException("Erro inesperado"));

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    // =========================================================================
    // Token válido mas autenticação já presente no contexto — não reprocessa
    // =========================================================================

    @Test
    void autenticacaoJaExistente_naoReprocessaToken() throws Exception {
        // Simula que o SecurityContext já tem uma autenticação setada antes do filtro
        UserDetails existente = userDetails("ja-logado@test.com");
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken authExistente =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        existente, null, existente.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authExistente);

        String fakeToken = "header.payload.signature";
        MockHttpServletRequest request  = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + fakeToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername(fakeToken)).thenReturn("ja-logado@test.com");

        filter.doFilter(request, response, filterChain);

        // userDetailsService NÃO deve ser chamado quando auth já existe
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(request, response);
    }
}
