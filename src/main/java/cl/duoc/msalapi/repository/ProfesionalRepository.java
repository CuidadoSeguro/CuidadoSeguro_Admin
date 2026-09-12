package cl.duoc.msalapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.msalapi.entity.Profesional;

public interface ProfesionalRepository extends JpaRepository<Profesional, Long> {

    boolean existsByRutIgnoreCase(String rut);
}
