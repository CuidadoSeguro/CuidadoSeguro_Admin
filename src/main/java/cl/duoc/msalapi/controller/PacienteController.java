package cl.duoc.msalapi.controller;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.msalapi.entity.Paciente;
import cl.duoc.msalapi.dto.CrearPacienteRequest;
import cl.duoc.msalapi.service.CognitoPacienteService;
import cl.duoc.msalapi.repository.PacienteRepository;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteRepository repository;
    private final CognitoPacienteService cognitoPacienteService;

    public PacienteController(PacienteRepository repository, CognitoPacienteService cognitoPacienteService) {
        this.repository = repository;
        this.cognitoPacienteService = cognitoPacienteService;
    }

    @GetMapping
    public ResponseEntity<List<Paciente>> listar(
            @AuthenticationPrincipal Jwt jwt) {

        if (!esAdmin(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Paciente> obtener(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        if (!esAdmin(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Paciente> crear(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CrearPacienteRequest solicitud) {

        if (!esAdmin(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String rut = solicitud.rut().trim();
        if (repository.existsByRutIgnoreCase(rut)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un paciente registrado con ese RUT.");
        }
        if (repository.existsByEmailIgnoreCase(solicitud.email().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un paciente registrado con ese correo.");
        }

        Paciente paciente = solicitud.toPaciente();
        // La cuenta se crea primero. Si falla la persistencia SQL, se elimina la cuenta Cognito.
        cognitoPacienteService.crearCuenta(
                paciente.getEmail(), paciente.getNombreCompleto(), solicitud.password());
        try {
            paciente.setRut(rut);
            Paciente guardado = repository.save(paciente);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
        } catch (RuntimeException ex) {
            cognitoPacienteService.eliminarCuenta(paciente.getEmail());
            throw ex;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Paciente> actualizar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody Paciente datos) {

        if (!esAdmin(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String rut = datos.getRut().trim();

        return repository.findById(id)
                .map(paciente -> {
                    boolean rutDuplicado = repository.existsByRutIgnoreCase(rut)
                            && !paciente.getRut().equalsIgnoreCase(rut);

                    if (rutDuplicado) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Ya existe un paciente con ese RUT");
                    }

                    paciente.setNombreCompleto(datos.getNombreCompleto());
                    paciente.setRut(rut);
                    paciente.setEmail(datos.getEmail());
                    paciente.setTelefono(datos.getTelefono());
                    paciente.setDiagnostico(datos.getDiagnostico());
                    paciente.setEstado(datos.getEstado());
                    paciente.setFechaNacimiento(datos.getFechaNacimiento());

                    return ResponseEntity.ok(repository.save(paciente));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        if (!esAdmin(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private boolean esAdmin(Jwt jwt) {
        if (jwt == null) {
            return false;
        }

        List<String> roles = jwt.getClaimAsStringList("roles");

        return roles != null && roles.contains("0");
    }
}
