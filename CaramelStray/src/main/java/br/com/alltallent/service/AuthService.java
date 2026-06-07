package br.com.alltallent.service;

import java.time.OffsetDateTime; // Importante para a data de cadastro
import java.time.ZoneId;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.alltallent.dto.CadastroRequestDTO;
import br.com.alltallent.exception.ResourceNotFoundException;
import br.com.alltallent.model.Area;
import br.com.alltallent.model.Funcionario;
import br.com.alltallent.model.Perfil;
import br.com.alltallent.repository.AreaRepository;
import br.com.alltallent.repository.FuncionarioRepository;
import br.com.alltallent.repository.PerfilRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final FuncionarioRepository funcionarioRepository;
    private final AreaRepository areaRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder; 

    @Transactional
    public Funcionario register(CadastroRequestDTO request) {
        
        // 1. Validar se o email já existe
        if (funcionarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Erro: Email já está em uso!");
        }

        // 2. Buscar relacionamentos obrigatórios
        Area area = areaRepository.findById(request.getCodigoArea())
                .orElseThrow(() -> new ResourceNotFoundException("Erro: Área (Departamento) não encontrada."));

        Perfil perfil = perfilRepository.findById(request.getCodigoPerfil())
                .orElseThrow(() -> new ResourceNotFoundException("Erro: Perfil (Cargo) não encontrado."));

        // 3. Criptografia
        String senhaCriptografada = passwordEncoder.encode(request.getSenha());

        // 4. Criar e popular o Funcionário
        Funcionario novoFuncionario = new Funcionario();
        novoFuncionario.setNomeCompleto(request.getNomeCompleto());
        novoFuncionario.setEmail(request.getEmail());
        novoFuncionario.setSenhaHash(senhaCriptografada);
        novoFuncionario.setTelefone(request.getTelefone());
        novoFuncionario.setResumo(request.getResumo());
        novoFuncionario.setArea(area);
        novoFuncionario.setPerfil(perfil);
        
        // Novos campos
        novoFuncionario.setCpf(request.getCpf());
        novoFuncionario.setLocalizacao(request.getLocalizacao());
        novoFuncionario.setTituloProfissional(request.getTituloProfissional());
        novoFuncionario.setIdCracha(request.getIdCracha());
        novoFuncionario.setDataAdmissao(request.getDataAdmissao());
        
        // --- DATA DE CADASTRO AUTOMÁTICA ---
        novoFuncionario.setDataCadastro(OffsetDateTime.now(ZoneId.of("America/Sao_Paulo"))); 

        // Lógica do Gestor (Opcional)
        if (request.getCodigoGestor() != null) {
            Funcionario gestor = funcionarioRepository.findById(request.getCodigoGestor())
                .orElseThrow(() -> new ResourceNotFoundException("Erro: Gestor informado não encontrado."));
            novoFuncionario.setGestor(gestor);
        }
        
        // 5. Salvar
        return funcionarioRepository.save(novoFuncionario);
    }
}