package cl.duoc.msalapi.controller;

import cl.duoc.msalapi.dto.SolicitudFichaClinicaDTO;
import cl.duoc.msalapi.dto.SolicitudPdfResponse;
import cl.duoc.msalapi.entity.Paciente;
import cl.duoc.msalapi.repository.PacienteRepository;
import cl.duoc.msalapi.service.FichaClinicaService;
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
public class FichaClinicaController {

    private final PacienteRepository pacienteRepository;
    private final FichaClinicaService fichaClinicaService;

    public FichaClinicaController(
            PacienteRepository pacienteRepository,
            FichaClinicaService fichaClinicaService) {
        this.pacienteRepository = pacienteRepository;
        this.fichaClinicaService = fichaClinicaService;
    }

    @PostMapping("/{id}/ficha-clinica")
    public ResponseEntity<SolicitudPdfResponse> solicitarFichaClinica(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody SolicitudFichaClinicaDTO solicitud) {
        verificarAdministrador(jwt);

        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (paciente.getFechaNacimiento() == null
                || paciente.getFechaNacimiento().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El paciente no tiene fecha de nacimiento registrada");
        }

        SolicitudPdfResponse respuesta = fichaClinicaService.solicitar(
                paciente, solicitud.getCorreoDestino());
        return ResponseEntity.accepted().body(respuesta);
    }

    @GetMapping("/fichas-clinicas/{requestId}")
    public ResponseEntity<byte[]> descargarFichaClinica(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String requestId) {
        verificarAdministrador(jwt);
        return fichaClinicaService.descargar(requestId);
    }

    @GetMapping("/{id}/ficha-clinica/descarga")
    public ResponseEntity<byte[]> descargarFichaClinicaDirecta(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        verificarAdministrador(jwt);

        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (paciente.getFechaNacimiento() == null
                || paciente.getFechaNacimiento().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El paciente no tiene fecha de nacimiento registrada");
        }

        return fichaClinicaService.descargarDirecto(paciente);
    }

    private void verificarAdministrador(Jwt jwt) {
        List<String> roles = jwt == null ? null : jwt.getClaimAsStringList("roles");

        if (roles == null || !roles.contains("0")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
