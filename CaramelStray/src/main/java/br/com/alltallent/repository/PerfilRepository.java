package br.com.alltallent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.alltallent.model.Perfil;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Integer> {
   
}