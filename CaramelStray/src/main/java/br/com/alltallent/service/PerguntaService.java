package br.com.alltallent.service; 

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.alltallent.dto.PerguntaRequestDTO;
import br.com.alltallent.dto.PerguntaResponseDTO;
import br.com.alltallent.dto.OpcaoRequest;
import br.com.alltallent.model.Competencia;
import br.com.alltallent.model.Pergunta; 
import br.com.alltallent.model.PerguntaOpcao;
import br.com.alltallent.repository.CompetenciaRepository;
import br.com.alltallent.repository.PerguntaRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class PerguntaService {

    private static final Logger log = LoggerFactory.getLogger(PerguntaService.class);

    private final PerguntaRepository perguntaRepository;
    private final CompetenciaRepository competenciaRepository;

    public PerguntaService(PerguntaRepository perguntaRepository, CompetenciaRepository competenciaRepository) {
        this.perguntaRepository = perguntaRepository;
        this.competenciaRepository = competenciaRepository;
    }

    @Transactional 
    public PerguntaResponseDTO criarPergunta(PerguntaRequestDTO dto) {
        Competencia competencia = competenciaRepository.findById(dto.competenciaCodigo())
                .orElseThrow(() -> new EntityNotFoundException("Competência não encontrada: " + dto.competenciaCodigo()));

        Pergunta novaPergunta = new Pergunta();
        novaPergunta.setEnunciado(dto.pergunta());
        novaPergunta.setCompetencia(competencia);
        novaPergunta.setTipoPergunta(dto.tipoPergunta()); 

        // CORREÇÃO DO BUG: Verificação mais robusta do tipo (com ou sem acento)
        String tipo = dto.tipoPergunta() != null ? dto.tipoPergunta().toLowerCase() : "";
        boolean isMultipla = tipo.contains("múltipla") || tipo.contains("multipla");

        if (isMultipla && dto.opcoes() != null && !dto.opcoes().isEmpty()) {
            log.debug("Processando {} opções recebidas", dto.opcoes().size());

            Set<PerguntaOpcao> opcoesSet = new HashSet<>();

            for (OpcaoRequest opRequest : dto.opcoes()) {
                if (opRequest.descricao() != null && !opRequest.descricao().trim().isEmpty()) {
                    PerguntaOpcao opcao = new PerguntaOpcao();
                    opcao.setDescricaoOpcao(opRequest.descricao().trim());
                    opcao.setIsCorreta(opRequest.isCorreta());
                    opcao.setPergunta(novaPergunta);
                    opcoesSet.add(opcao);
                }
            }
            novaPergunta.setOpcoes(opcoesSet);
        } else {
            log.debug("Pergunta não é múltipla escolha ou não há opções válidas. Tipo recebido: {}", tipo);
        }

        Pergunta perguntaSalva = perguntaRepository.save(novaPergunta);
        log.debug("Pergunta salva com código: {}", perguntaSalva.getCodigo());

        return new PerguntaResponseDTO(perguntaSalva);
    }

    @Transactional(readOnly = true) 
    public List<PerguntaResponseDTO> listarTodas() {
        return perguntaRepository.findAll().stream()
                .map(PerguntaResponseDTO::new)
                .toList();
    }

     
    @Transactional(readOnly = true)
    public PerguntaResponseDTO buscarPorId(Long id) {
        return perguntaRepository.findById(id)
                .map(PerguntaResponseDTO::new)
                .orElseThrow(() -> new EntityNotFoundException("Pergunta não encontrada: " + id));
    }

    @Transactional
    public void deletarPergunta(Long id) {
        if (!perguntaRepository.existsById(id)) {
            throw new EntityNotFoundException("Pergunta não encontrada: " + id);
        }
        perguntaRepository.deleteById(id);
    }
}