package cl.duoc.msalapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration"
})
@AutoConfigureMockMvc
class ApiSecurityTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private JwtDecoder jwtDecoder;

	@Test
	void publicHolaWithoutTokenIsOk() throws Exception {
		mockMvc.perform(get("/public/hola"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mensaje").value("API pública: no se pidió token."));
	}

	@Test
	void privateMeWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void privateMeWithJwtIsOk() throws Exception {
		mockMvc.perform(get("/api/me").with(SecurityMockMvcRequestPostProcessors.jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mensaje").value("API privada: el access token es válido."));
	}
}
