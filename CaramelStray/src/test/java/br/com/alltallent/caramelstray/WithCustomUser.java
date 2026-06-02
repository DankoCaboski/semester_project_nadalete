package br.com.alltallent.caramelstray;

import br.com.alltallent.config.CustomUserDetails;
import br.com.alltallent.model.Funcionario;
import br.com.alltallent.model.Perfil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Anotação de teste que monta um SecurityContext com um CustomUserDetails real.
 *
 * perfilCodigo:
 *   0 (padrão) → sem perfil → ROLE_USER
 *   1           → ROLE_ADMIN + ROLE_GESTOR + ROLE_USER
 *   2           → ROLE_GESTOR + ROLE_USER
 *   3+          → ROLE_USER
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithCustomUser.Factory.class)
public @interface WithCustomUser {

    int codigo() default 1;
    String email() default "user@test.com";

    /**
     * Código do perfil que define as roles geradas por CustomUserDetails.
     * 0 = sem perfil (apenas ROLE_USER).
     */
    int perfilCodigo() default 0;

    class Factory implements WithSecurityContextFactory<WithCustomUser> {

        @Override
        public SecurityContext createSecurityContext(WithCustomUser annotation) {
            Funcionario funcionario = new Funcionario();
            funcionario.setCodigo(annotation.codigo());
            funcionario.setEmail(annotation.email());
            funcionario.setSenhaHash("senha-hash");

            if (annotation.perfilCodigo() > 0) {
                Perfil perfil = new Perfil();
                perfil.setCodigo(annotation.perfilCodigo());
                funcionario.setPerfil(perfil);
            } else {
                funcionario.setPerfil(null);
            }

            CustomUserDetails principal = new CustomUserDetails(funcionario);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            return context;
        }
    }
}