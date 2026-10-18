package cl.duoc.msalapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

	@SpringBootTest(properties = {
		"spring.security.oauth2.resourceserver.jwt.issuer-uri=",
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
		"spring.datasource.url=jdbc:h2:mem:cuidadoseguro;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ApiSecurityTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private JwtDecoder jwtDecoder;

	@Test
	void publicHolaWithoutTokenIsOk() throws Exception {
		mockMvc.perform(get("/public/isALive"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mensaje").value("API pública: El microservicio esta vivo."));
	}

	@Test
	void privateMeWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/pacientes")).andExpect(status().isUnauthorized());
	}

	@Test
	void privateMeWithJwtIsOk() throws Exception {
		mockMvc.perform(get("/api/pacientes").with(
				SecurityMockMvcRequestPostProcessors.jwt()
						.jwt(jwt -> jwt.claim("roles", List.of("0")))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}
}
