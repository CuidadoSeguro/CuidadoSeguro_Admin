package cl.duoc.msalapi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.msalapi.dto.SolicitudPdfResponse;
import cl.duoc.msalapi.entity.Paciente;
import cl.duoc.msalapi.repository.PacienteRepository;
import cl.duoc.msalapi.service.FichaClinicaService;

@SpringBootTest(properties = {
		"spring.security.oauth2.resourceserver.jwt.issuer-uri=",
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
		"spring.datasource.url=jdbc:h2:mem:cuidadoseguro-ficha;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class FichaClinicaControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PacienteRepository pacienteRepository;

	@MockBean
	private JwtDecoder jwtDecoder;

	@MockBean
	private FichaClinicaService fichaClinicaService;

	private Paciente pacienteConFecha() {
		Paciente paciente = new Paciente();
		paciente.setNombreCompleto("Paciente de Prueba");
		paciente.setRut("12.345.678-9");
		paciente.setEmail("paciente@example.com");
		paciente.setTelefono("912345678");
		paciente.setDiagnostico("Control general");
		paciente.setEstado("Activo");
		paciente.setFechaNacimiento("2000-01-01");
		return pacienteRepository.save(paciente);
	}

	@Test
	void fichaClinicaWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/pacientes/1/ficha-clinica")
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void fichaClinicaWithoutAdminRoleIsForbidden() throws Exception {
		mockMvc.perform(post("/api/pacientes/1/ficha-clinica")
						.with(SecurityMockMvcRequestPostProcessors.jwt()
								.jwt(jwt -> jwt.claim("roles", List.of("1"))))
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void fichaClinicaForUnknownPatientIsNotFound() throws Exception {
		mockMvc.perform(post("/api/pacientes/999/ficha-clinica")
						.with(SecurityMockMvcRequestPostProcessors.jwt()
								.jwt(jwt -> jwt.claim("roles", List.of("0"))))
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void fichaClinicaWithoutBirthDateIsBadRequest() throws Exception {
		Paciente paciente = pacienteConFecha();
		paciente.setFechaNacimiento("  ");
		pacienteRepository.save(paciente);

		mockMvc.perform(post("/api/pacientes/" + paciente.getId() + "/ficha-clinica")
						.with(SecurityMockMvcRequestPostProcessors.jwt()
								.jwt(jwt -> jwt.claim("roles", List.of("0"))))
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void fichaClinicaWithAdminIsAccepted() throws Exception {
		Paciente paciente = pacienteConFecha();

		org.mockito.Mockito.when(fichaClinicaService.solicitar(any(Paciente.class), isNull()))
				.thenReturn(new SolicitudPdfResponse("req-123", "PROCESANDO"));

		mockMvc.perform(post("/api/pacientes/" + paciente.getId() + "/ficha-clinica")
						.with(SecurityMockMvcRequestPostProcessors.jwt()
								.jwt(jwt -> jwt.claim("roles", List.of("0"))))
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.requestId").value("req-123"))
				.andExpect(jsonPath("$.estado").value("PROCESANDO"));
	}

	@Test
	void fichaClinicaWithMailForwardsCorreoDestino() throws Exception {
		Paciente paciente = pacienteConFecha();

		org.mockito.Mockito.when(fichaClinicaService.solicitar(any(Paciente.class), eq("destino@example.com")))
				.thenReturn(new SolicitudPdfResponse("req-456", "PROCESANDO"));

		mockMvc.perform(post("/api/pacientes/" + paciente.getId() + "/ficha-clinica")
						.with(SecurityMockMvcRequestPostProcessors.jwt()
								.jwt(jwt -> jwt.claim("roles", List.of("0"))))
						.contentType("application/json")
						.content("{\"correoDestino\":\"destino@example.com\"}"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.requestId").value("req-456"));
	}

	@Test
	void descargarFichaWithoutAdminRoleIsForbidden() throws Exception {
		mockMvc.perform(get("/api/pacientes/fichas-clinicas/req-1")
						.with(SecurityMockMvcRequestPostProcessors.jwt()
								.jwt(jwt -> jwt.claim("roles", List.of("1")))))
				.andExpect(status().isForbidden());
	}

	@Test
	void descargarFichaProxiesServiceResponse() throws Exception {
		org.mockito.Mockito.when(fichaClinicaService.descargar("req-2"))
				.thenReturn(ResponseEntity.status(HttpStatus.OK).build());

		mockMvc.perform(get("/api/pacientes/fichas-clinicas/req-2")
						.with(SecurityMockMvcRequestPostProcessors.jwt()
								.jwt(jwt -> jwt.claim("roles", List.of("0")))))
				.andExpect(status().isOk());
	}
}
