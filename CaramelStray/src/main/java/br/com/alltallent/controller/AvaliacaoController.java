package br.com.alltallent.controller;

import br.com.alltallent.dto.*; 
import br.com.alltallent.exception.ResourceNotFoundException; 
import br.com.alltallent.service.AvaliacaoService; 
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid; 
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder; 

import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    private static final Logger log = LoggerFactory.getLogger(AvaliacaoController.class);

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public ResponseEntity<AvaliacaoResponseDTO> criarAvaliacao(
            @Valid @RequestBody AvaliacaoRequestDTO dto) {
        try {
            AvaliacaoResponseDTO avaliacaoCriada = avaliacaoService.criarAvaliacaoCompleta(dto);

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest() 
                    .path("/{id}") 
                    .buildAndExpand(avaliacaoCriada.codigo()) 
                    .toUri();

            return ResponseEntity.created(location).body(avaliacaoCriada);

        } catch (EntityNotFoundException e) {
             return ResponseEntity.badRequest().body(null); 
        } catch (Exception e) {
            log.error("Erro ao criar avaliação: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public ResponseEntity<List<AvaliacaoResponseDTO>> listarTodasAvaliacoes() {
        List<AvaliacaoResponseDTO> lista = avaliacaoService.listarTodasAvaliacoes();
        return ResponseEntity.ok(lista);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public ResponseEntity<AvaliacaoDetalhadaDTO> buscarAvaliacaoDetalhada(@PathVariable Integer id) {
        try {
            AvaliacaoDetalhadaDTO detalhadaDTO = avaliacaoService.buscarAvaliacaoDetalhada(id);
            return ResponseEntity.ok(detalhadaDTO);
        } catch (ResourceNotFoundException e) { 
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{id}/instancias")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public ResponseEntity<List<AvaliacaoFuncionarioResponseDTO>> buscarInstanciasPorAvaliacao(@PathVariable Integer id) {
         try {
             List<AvaliacaoFuncionarioResponseDTO> instancias = avaliacaoService.buscarInstanciasPorAvaliacao(id);
             return ResponseEntity.ok(instancias);
         } catch (EntityNotFoundException e) { 
             return ResponseEntity.notFound().build();
         }
    }
    
    @PostMapping("/respostas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> salvarResposta(
            @Valid @RequestBody RespostaColaboradorRequestDTO respostaDTO) {
        try {
            RespostaColaboradorResponseDTO respostaSalva = avaliacaoService.salvarOuAtualizarResposta(respostaDTO);
            return ResponseEntity.ok(respostaSalva); 
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Erro ao salvar resposta: " + e.getMessage());
        } catch (Exception e) {
            log.error("Erro interno ao salvar resposta: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno ao salvar resposta.");
        }
    }

    @GetMapping("/instancias/{instanciaId}/respostas")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public ResponseEntity<List<RespostaColaboradorResponseDTO>> buscarRespostasPorInstancia(@PathVariable Long instanciaId) {
        try {
            List<RespostaColaboradorResponseDTO> respostas = avaliacaoService.buscarRespostasPorInstancia(instanciaId);
            return ResponseEntity.ok(respostas);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().<List<RespostaColaboradorResponseDTO>>build();
        }
    }

    // --- NOVO ENDPOINT PARA SUPERVISOR VISUALIZAR AVALIAÇÃO CONCLUÍDA (Task 4) ---
    @GetMapping("/revisao/{codigoAvaliacaoFuncionario}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public ResponseEntity<List<RevisaoDetalhadaDTO>> getDadosParaRevisao(@PathVariable Long codigoAvaliacaoFuncionario) {
        try {
            List<RevisaoDetalhadaDTO> revisao = avaliacaoService.buscarDadosRevisao(codigoAvaliacaoFuncionario);
            return ResponseEntity.ok(revisao);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().<List<RevisaoDetalhadaDTO>>build();
        }
    }
    // -----------------------------------------------------------------------------

    @PutMapping("/instancias/{instanciaId}/revisar") 
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public ResponseEntity<Object> salvarRevisaoSupervisor(
            @PathVariable Long instanciaId,
            @Valid @RequestBody RevisaoSupervisorRequestDTO revisaoDTO) {
        try {
            AvaliacaoFuncionarioResponseDTO instanciaAtualizada = avaliacaoService.salvarRevisaoSupervisor(instanciaId, revisaoDTO);
            return ResponseEntity.ok(instanciaAtualizada);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().<Object>build();
        } catch (Exception e) {
            log.error("Erro interno ao salvar revisão: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno ao salvar revisão.");
        }
    }

    @GetMapping("/pendentes/{funcionarioId}")
    @PreAuthorize("principal.codigo == #funcionarioId")
    public ResponseEntity<List<AvaliacaoFuncionarioResponseDTO>> buscarAvaliacoesPendentes(@PathVariable Integer funcionarioId) {
        List<AvaliacaoFuncionarioResponseDTO> pendentes = avaliacaoService.buscarPendentesPorFuncionario(funcionarioId);
        return ResponseEntity.ok(pendentes);
    }

    @GetMapping("/instancias/{instanciaId}/responder")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AvaliacaoParaResponderDTO> buscarAvaliacaoParaResponder(@PathVariable Long instanciaId) {
        try {
            AvaliacaoParaResponderDTO dto = avaliacaoService.buscarParaResponder(instanciaId);
            return ResponseEntity.ok(dto);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().<AvaliacaoParaResponderDTO>build();
        }
    }
    
    @PutMapping("/instancias/{instanciaId}/finalizar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> finalizarAvaliacaoColaborador(@PathVariable Long instanciaId) {
        try {
             avaliacaoService.finalizarPeloColaborador(instanciaId);
             return ResponseEntity.noContent().build(); 
        } catch (EntityNotFoundException e) {
             return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) { 
             return ResponseEntity.status(HttpStatus.CONFLICT).build(); 
        }
    }
}