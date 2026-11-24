package cl.duoc.msalapi.controller;

import cl.duoc.msalapi.dto.SignoVitalDTO;
import cl.duoc.msalapi.entity.SignoVital;
import cl.duoc.msalapi.repository.PacienteRepository;
import cl.duoc.msalapi.service.SignoVitalService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/pacientes")
public class SignoVitalController {

    private final PacienteRepository pacienteRepository;
    private final SignoVitalService signoVitalService;

    public SignoVitalController(PacienteRepository pacienteRepository,
                                SignoVitalService signoVitalService) {
        this.pacienteRepository = pacienteRepository;
        this.signoVitalService = signoVitalService;
    }

    @PostMapping("/{id}/signos-vitales")
    public ResponseEntity<SignoVital> registrar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody SignoVitalDTO dto) {
        if (!puedeRegistrar(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!pacienteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no encontrado");
        }

        dto.setPacienteId(id);
        SignoVital guardado = signoVitalService.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @GetMapping("/{id}/signos-vitales")
    public ResponseEntity<List<SignoVital>> listar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        if (!puedeRegistrar(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!pacienteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no encontrado");
        }

        return ResponseEntity.ok(signoVitalService.listarPorPaciente(id));
    }

    private boolean puedeRegistrar(Jwt jwt) {
        // Cualquier usuario autenticado (paciente o profesional) puede
        // consultar/registrar signos vitales. El rol se resuelve desde el
        // cliente al iniciar sesión con Cognito.
        return jwt != null;
    }
}
