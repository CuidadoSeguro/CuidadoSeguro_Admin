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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.msalapi.entity.Profesional;
import cl.duoc.msalapi.repository.ProfesionalRepository;

@RestController
@RequestMapping("/api/profesionales")
public class ProfesionalController {

    private final ProfesionalRepository repository;

    public ProfesionalController(ProfesionalRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<Profesional>> listar(
            @AuthenticationPrincipal Jwt jwt) {

        if (!esAdmin(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Profesional> obtener(
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
    public ResponseEntity<Profesional> crear(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody Profesional profesional) {

        if (!esAdmin(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String rut = profesional.getRut().trim();

        if (repository.existsByRutIgnoreCase(rut)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        profesional.setRut(rut);
        Profesional guardado = repository.save(profesional);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(guardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Profesional> actualizar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody Profesional datos) {

        if (!esAdmin(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String rut = datos.getRut().trim();

        return repository.findById(id)
                .map(profesional -> {
                    boolean rutDuplicado = repository.existsByRutIgnoreCase(rut)
                            && !profesional.getRut().equalsIgnoreCase(rut);

                    if (rutDuplicado) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Ya existe un profesional con ese RUT");
                    }

                    profesional.setNombreCompleto(datos.getNombreCompleto());
                    profesional.setRut(rut);
                    profesional.setEspecialidad(datos.getEspecialidad());
                    profesional.setEmail(datos.getEmail());
                    profesional.setTelefono(datos.getTelefono());
                    profesional.setActivo(datos.getActivo());

                    return ResponseEntity.ok(repository.save(profesional));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        if (!esAdmin(jwt)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un administrador puede eliminar profesionales");
        }

        repository.deleteById(id);
    }

    private boolean esAdmin(Jwt jwt) {
        if (jwt == null) {
            return false;
        }

        List<String> roles = jwt.getClaimAsStringList("roles");

        return roles != null && roles.contains("0");
    }
}
