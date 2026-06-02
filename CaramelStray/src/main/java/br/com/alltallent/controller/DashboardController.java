package br.com.alltallent.controller;

import br.com.alltallent.config.CustomUserDetails;
import br.com.alltallent.dto.DashboardResponseDTO;
import br.com.alltallent.model.Funcionario;
import br.com.alltallent.repository.FuncionarioRepository;
import br.com.alltallent.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;
    private final FuncionarioRepository funcionarioRepository;

    /**
     * Retorna dados do Dashboard.
     * - Se for GESTOR: Retorna apenas dados da sua equipe/área.
     * - Se for ADMIN: Pode ver tudo ou filtrar por ?codigoArea=X
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> getDashboardData(
            @RequestParam(required = false) Integer codigoArea,
            Authentication authentication) {

        log.debug("Endpoint getDashboardData acionado");

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer usuarioLogadoId = userDetails.getCodigo();

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DIRETORIA"));

            boolean isGestor = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR") || a.getAuthority().equals("ROLE_SUPERVISAO"));

            Integer filtroAreaId = codigoArea;

            if (isGestor && !isAdmin) {
                Funcionario gestor = funcionarioRepository.findById(usuarioLogadoId)
                        .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

                if (gestor.getArea() != null) {
                    filtroAreaId = gestor.getArea().getCodigo();
                    log.debug("Filtro de área aplicado para gestor: areaId={}", filtroAreaId);
                }
            }

            DashboardResponseDTO data = dashboardService.getDashboardData(filtroAreaId);
            log.debug("getDashboardData concluído com sucesso");

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            log.error("Erro ao obter dados do dashboard: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Erro interno no servidor: " + e.getMessage());
        }
    }
}