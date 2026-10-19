package cl.duoc.msalapi.service;

import cl.duoc.msalapi.dto.SolicitudPdfResponse;
import cl.duoc.msalapi.entity.Paciente;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FichaClinicaService {

    private final RestClient queuesClient;

    public FichaClinicaService(
            RestClient.Builder builder,
            @Value("${queues.service.base-url:http://cuidadoseguro-queue-cf502d50d1881b7f.elb.us-east-1.amazonaws.com:8086}") String queuesServiceBaseUrl) {
        this.queuesClient = builder.baseUrl(queuesServiceBaseUrl).build();
    }

    public SolicitudPdfResponse solicitar(Paciente paciente, String correoDestino) {
        boolean enviarCorreo = correoDestino != null && !correoDestino.isBlank();

        try {
            return queuesClient.post()
                    .uri("/api/descargar")
                    .body(new DatosPdfRequest(
                            paciente.getRut(),
                            paciente.getNombreCompleto(),
                            paciente.getFechaNacimiento(),
                            "Ficha clinica",
                            correoDestino,
                            enviarCorreo))
                    .retrieve()
                    .body(SolicitudPdfResponse.class);
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(ex.getStatusCode().value()),
                    "El servicio de documentos no aceptó la solicitud: " + ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de documentos no está disponible", ex);
        }
    }

    public ResponseEntity<byte[]> descargar(String requestId) {
        try {
            return queuesClient.get()
                    .uri("/api/descargar/{requestId}", requestId)
                    .retrieve()
                    .toEntity(byte[].class);
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .headers(ex.getResponseHeaders())
                    .body(ex.getResponseBodyAsByteArray());
        }
    }

    public ResponseEntity<byte[]> descargarDirecto(Paciente paciente) {
        try {
            byte[] pdf = queuesClient.post()
                    .uri("/api/pdf_direct")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DatosPdfRequest(
                            paciente.getRut(),
                            paciente.getNombreCompleto(),
                            paciente.getFechaNacimiento(),
                            "Ficha clinica",
                            null,
                            false))
                    .retrieve()
                    .body(byte[].class);

            if (pdf == null || pdf.length == 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "El servicio de documentos devolvió un archivo vacío");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=\"ficha-clinica.pdf\"")
                    .body(pdf);
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(ex.getStatusCode().value()),
                    "El servicio de documentos no pudo generar la ficha: "
                            + ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de documentos no está disponible", ex);
        }
    }

    private record DatosPdfRequest(
            String rut,
            String nombre,
            @com.fasterxml.jackson.annotation.JsonProperty("fecha_nacimiento")
            String fechaNacimiento,
            @com.fasterxml.jackson.annotation.JsonProperty("tipo_documento")
            String tipoDocumento,
            @com.fasterxml.jackson.annotation.JsonProperty("correo_destino")
            String correoDestino,
            @com.fasterxml.jackson.annotation.JsonProperty("enviar_correo")
            boolean enviarCorreo) {
    }
}
