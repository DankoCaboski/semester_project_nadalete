package br.com.alltallent.caramelstray;

import br.com.alltallent.config.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Gera uma chave HS256 válida para uso nos testes e injeta via reflection
        String base64Key = Encoders.BASE64.encode(
                Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded());
        ReflectionTestUtils.setField(jwtService, "secretKey", base64Key);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private UserDetails userDetails(String username) {
        return User.withUsername(username)
                .password("irrelevante")
                .authorities(Collections.emptyList())
                .build();
    }


    @Test
    void generateToken_retornaTokenNaoNuloNemVazio() {
        UserDetails ud = userDetails("joao@test.com");

        String token = jwtService.generateToken(ud);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateToken_comExtraClaims_retornaTokenValido() {
        UserDetails ud = userDetails("maria@test.com");
        Map<String, Object> extra = Map.of("role", "ADMIN");

        String token = jwtService.generateToken(extra, ud);

        assertNotNull(token);
        // token JWT tem exatamente três segmentos separados por ponto
        assertEquals(3, token.split("\\.").length);
    }


    @Test
    void extractUsername_retornaEmailCorreto() {
        UserDetails ud = userDetails("usuario@test.com");
        String token = jwtService.generateToken(ud);

        String username = jwtService.extractUsername(token);

        assertEquals("usuario@test.com", username);
    }


    @Test
    void isTokenValid_retornaTrue_quandoTokenDoMesmoUsuario() {
        UserDetails ud = userDetails("alice@test.com");
        String token = jwtService.generateToken(ud);

        assertTrue(jwtService.isTokenValid(token, ud));
    }

    @Test
    void isTokenValid_retornaFalse_quandoTokenDeOutroUsuario() {
        UserDetails dono   = userDetails("dono@test.com");
        UserDetails outro  = userDetails("outro@test.com");
        String token = jwtService.generateToken(dono);

        assertFalse(jwtService.isTokenValid(token, outro));
    }


    @Test
    void extractClaim_retornaSubjectCorreto() {
        UserDetails ud = userDetails("sub@test.com");
        String token = jwtService.generateToken(ud);

        // Claims::getSubject é o mesmo que extractUsername; testa a API genérica
        String subject = jwtService.extractClaim(token, Claims::getSubject);

        assertEquals("sub@test.com", subject);
    }

    @Test
    void extractClaim_retornaExpirationNaoNula() {
        UserDetails ud = userDetails("exp@test.com");
        String token = jwtService.generateToken(ud);

        var expiration = jwtService.extractClaim(token, Claims::getExpiration);

        assertNotNull(expiration);
    }
}
