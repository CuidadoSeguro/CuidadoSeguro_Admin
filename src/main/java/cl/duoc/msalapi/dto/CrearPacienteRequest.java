package cl.duoc.msalapi.dto;

import cl.duoc.msalapi.entity.Paciente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Solicitud de alta de paciente y creación de sus credenciales en Cognito. */
public record CrearPacienteRequest(
        @NotBlank String nombreCompleto,
        @NotBlank String rut,
        @NotBlank @Email String email,
        String telefono,
        String diagnostico,
        String estado,
        String fechaNacimiento,
        @NotBlank @Size(min = 8, max = 256) String password) {

    public Paciente toPaciente() {
        Paciente paciente = new Paciente();
        paciente.setNombreCompleto(nombreCompleto.trim());
        paciente.setRut(rut.trim());
        paciente.setEmail(email.trim().toLowerCase());
        paciente.setTelefono(telefono);
        paciente.setDiagnostico(diagnostico);
        paciente.setEstado(estado == null || estado.isBlank() ? "Activo" : estado);
        paciente.setFechaNacimiento(fechaNacimiento);
        return paciente;
    }
}
