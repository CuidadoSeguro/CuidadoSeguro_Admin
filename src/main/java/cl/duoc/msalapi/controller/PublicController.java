package cl.duoc.msalapi.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.msalapi.entity.Paciente;
import cl.duoc.msalapi.repository.PacienteRepository;
import cl.duoc.msalapi.repository.ProfesionalRepository;

@RestController
@RequestMapping("/public")
public class PublicController {
	private final PacienteRepository repository;
	private final ProfesionalRepository profesionalRepository;

    public PublicController(PacienteRepository repository, ProfesionalRepository profesionalRepository) {
        this.repository = repository;
        this.profesionalRepository = profesionalRepository;
    }

	@GetMapping("/isALive")
	public Map<String, String> hola() {
		return Map.of(
				"mensaje", "API pública: El microservicio esta vivo.",
				"recurso", "/public/isALive");
	}

	@GetMapping("/countPacientes")
    public ResponseEntity<Integer> countPacientes() {
        return ResponseEntity.ok((int)repository.count());
    }
	
	@GetMapping("/countProfesionales")
	public ResponseEntity<Integer> countProfesionales() {
		return ResponseEntity.ok((int) profesionalRepository.count());
	}
}
