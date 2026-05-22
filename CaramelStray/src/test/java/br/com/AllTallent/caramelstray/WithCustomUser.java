package br.com.AllTallent.caramelstray;

import br.com.AllTallent.config.CustomUserDetails;
import br.com.AllTallent.model.Funcionario;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithCustomUser.Factory.class)
public @interface WithCustomUser {

    int codigo() default 1;
    String email() default "user@test.com";
    String[] roles() default {};

    class Factory implements WithSecurityContextFactory<WithCustomUser> {

        @Override
        public SecurityContext createSecurityContext(WithCustomUser annotation) {
            Funcionario funcionario = new Funcionario();
            funcionario.setCodigo(annotation.codigo());
            funcionario.setEmail(annotation.email());
            funcionario.setSenhaHash("senha-hash");
            funcionario.setPerfil(null);

            CustomUserDetails principal = new CustomUserDetails(funcionario);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            return context;
        }
    }
}