package br.com.alltallent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.alltallent.model.Area;

@Repository
public interface AreaRepository extends JpaRepository<Area, Integer> {
    
}