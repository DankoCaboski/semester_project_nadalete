package br.com.alltallent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.alltallent.model.FuncionarioCertificado;

@Repository
public interface CertificadoRepository extends JpaRepository<FuncionarioCertificado, Integer> {
}