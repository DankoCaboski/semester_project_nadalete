package br.com.alltallent.repository;

import br.com.alltallent.model.Competencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetenciaRepository extends JpaRepository<Competencia, Integer> {
    boolean existsByNomeIgnoreCase(String nome);
}
