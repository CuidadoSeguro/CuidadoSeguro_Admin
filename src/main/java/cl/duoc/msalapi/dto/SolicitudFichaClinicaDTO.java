package cl.duoc.msalapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class SolicitudFichaClinicaDTO {

    @Email(message = "El correo de destino no es válido")
    @Size(max = 254, message = "El correo no puede superar 254 caracteres")
    private String correoDestino;

    public String getCorreoDestino() {
        return correoDestino;
    }

    public void setCorreoDestino(String correoDestino) {
        this.correoDestino = correoDestino;
    }
}
