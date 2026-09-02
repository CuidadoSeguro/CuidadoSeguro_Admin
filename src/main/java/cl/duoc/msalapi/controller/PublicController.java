package cl.duoc.msalapi.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicController {

	@GetMapping("/hola")
	public Map<String, String> hola() {
		return Map.of(
				"mensaje", "API pública: no se pidió token.",
				"recurso", "/public/hola");
	}
}
