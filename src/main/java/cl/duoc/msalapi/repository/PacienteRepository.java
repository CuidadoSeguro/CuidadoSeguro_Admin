package cl.duoc.msalapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.msalapi.entity.Paciente;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
}
