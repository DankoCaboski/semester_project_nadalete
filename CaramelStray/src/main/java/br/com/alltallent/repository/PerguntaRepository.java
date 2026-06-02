package br.com.alltallent.repository; // Ou o pacote onde seus repositórios estão

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.alltallent.model.Pergunta;

@Repository
public interface PerguntaRepository extends JpaRepository<Pergunta, Long> {

    List<Pergunta> findByCompetenciaCodigo(Integer competenciaCodigo);

    boolean existsByEnunciadoIgnoreCase(String textoPergunta);
}