package cl.duoc.msalapi.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {

	@GetMapping("/login")
	public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("mensaje", "API privada: el access token es válido.");
		body.put("sub", jwt.getSubject());
		body.put("aud", jwt.getAudience());
		body.put("scp", jwt.getClaimAsString("scp"));
		body.put("preferred_username", jwt.getClaimAsString("preferred_username"));
		body.put("oid", jwt.getClaimAsString("oid"));
		body.put("iss", jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
		body.put("roles",jwt.getClaim("roles"));
		return body;
	}
}
