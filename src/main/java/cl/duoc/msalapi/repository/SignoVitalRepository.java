package cl.duoc.msalapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.msalapi.entity.SignoVital;

import java.util.List;

public interface SignoVitalRepository extends JpaRepository<SignoVital, Long> {

    List<SignoVital> findByPacienteIdOrderByIdDesc(Long pacienteId);
}
