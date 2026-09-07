# Guía práctica — API Spring Boot + JWT Entra ID (pública y privada)

**Asignatura:** DSY1107 — Desarrollo Cloud Native I  
**EA1 · IDaaS / OAuth / resource server**  
**Carpeta de referencia:** `material complementario/msal-api`  
**Va después de:** `msal-front` (React + MSAL)

Esta guía es **autoexplicativa**. Síguela en orden (Paso 0 → 7).

En esta clase Spring solo pregunta: **“¿este JWT lo firmó mi tenant?”**  
**Expose an API** (`api://…` + scope propio) queda para la **siguiente clase**.

---

## Idea de fondo (léelo antes de codear)

En el demo React el access token se veía en pantalla, pero no se enviaba a nadie. Ahora Spring es el **resource server**: recibe el **pase** (access token).

| Rol OAuth | En este demo |
|---|---|
| Resource owner | Tú / el estudiante |
| Client | `msal-front` (React) o Postman |
| Auth server | Microsoft Entra ID |
| Resource server | Esta API Spring (`msal-api`) |

Spring **no hace login**. No pide usuario ni contraseña.

El front ya pide `User.Read`. Ese access token lo mandas a Spring. En esta clase **no** creamos un scope `api://…`: igual que en muchos proyectos reales, basta con que Entra del **mismo directorio** haya firmado el JWT.

| Ruta | Token | Resultado esperado |
|---|---|---|
| `GET /public/hola` | no | **200** — cualquiera entra |
| `GET /api/me` | no | **401** |
| `GET /api/me` | `Authorization: Bearer <access token>` | **200** + claims |

---

## Qué vas a construir al final

Una API Spring Boot 3 (Java 21) que:

1. Deja pasar `/public/**` sin JWT.
2. Exige JWT de Entra en `/api/**`.
3. En `/api/me` muestra `sub`, `aud`, `scp` y el usuario.

**Cómo usar esta carpeta si ya está generada:**

```bash
cd msal-api
# Completa el tenant ID (Paso 0 + Paso 3)
./mvnw spring-boot:run
```

---

## Paso 0 — Prerrequisito: la App Registration que ya tienes

**Para qué:** Spring necesita saber **qué tenant** firma los tokens. Eso ya lo sacaste en `msal-front` (Paso 8 de esa guía). **No** hace falta Expose an API todavía.

1. Abre [Portal de Azure](https://portal.azure.com) → **Microsoft Entra ID** → **App registrations** → la app del demo React.
2. En **Overview** copia **Directory (tenant) ID**.
3. Confirma que la plataforma es **SPA** y el redirect es `http://localhost:5173`.

Ese GUID es el único dato nuevo para Spring (`issuer-uri`). El **Application (client) ID** ya está en el `.env` del front; aquí no lo pegas.

**Cómo comprobar:** tienes el tenant ID a mano (el mismo `VITE_TENANT_ID` de `msal-front`).

---

## Paso 1 — Máquina: Java 21 y Maven

**Para qué:** poder compilar y arrancar Spring Boot.

```bash
java -version
```

Necesitas **Java 21**. Esta carpeta trae Maven Wrapper (`./mvnw`): no hace falta instalar Maven global.

**Cómo comprobar:** `java -version` muestra 21 (esta guía está pinneada a 21).

---

## Paso 2 — Crear el proyecto Spring Boot

**Para qué:** API REST vacía + librería que valida JWT.

Si partes de esta carpeta, **ya está creado**. Si partes de cero, en [start.spring.io](https://start.spring.io):

| Campo | Valor |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | 3.4.x |
| Group | `cl.duoc` |
| Artifact | `msal-api` |
| Packaging | Jar |
| Java | 21 |
| Dependencies | **Spring Web**, **OAuth2 Resource Server** |

Descomprime y entra a `msal-api`.

**Por qué Resource Server y no OAuth2 Client:** el client es React. Spring solo **valida** el Bearer. No hay client secret.

**Cómo comprobar:** existe `pom.xml` con `spring-boot-starter-web` y `spring-boot-starter-oauth2-resource-server`.

---

## Paso 3 — Configurar el issuer (solo el tenant)

**Para qué:** al arrancar, Spring baja las claves públicas de Entra y acepta JWT cuyo `iss` sea **ese** directorio. En esta clase **no** configuramos `audiences` (eso va con Expose an API, la próxima clase).

Edita `src/main/resources/application.yml` y pega tu tenant (sin la palabra `REEMPLAZAR`):

```yaml
server:
  port: 8080

spring:
  application:
    name: msal-api
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0
```

| Clave | Qué es |
|---|---|
| `issuer-uri` | “Solo acepto tokens firmados por **este** directorio” (`/v2.0`) |

Si arrancas con `REEMPLAZAR_CON_...`, Spring intenta consultar Entra y **falla al subir**.

**Cómo comprobar:** el YAML no contiene la palabra `REEMPLAZAR`.

---

## Paso 4 — Cadena de seguridad (pública vs privada)

**Para qué:** sin token en `/public`; con JWT en `/api`. CORS para React (`5173`) y Angular (`4200`).

Crea `src/main/java/cl/duoc/msalapi/config/SecurityConfig.java`:

```java
package cl.duoc.msalapi.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/public/**").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().denyAll())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration cfg = new CorsConfiguration();
		cfg.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:4200"));
		cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
		cfg.setAllowCredentials(true);
		cfg.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", cfg);
		return source;
	}
}
```

`oauth2ResourceServer().jwt()` usa el `issuer-uri` del Paso 3. CSRF se apaga porque el client manda **Bearer**, no cookie de sesión.

**Cómo comprobar:** `/public/**` está en `permitAll` y `/api/**` en `authenticated`.

---

## Paso 5 — Los dos endpoints

### 5.1 Pública — `PublicController`

```java
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
```

### 5.2 Privada — `MeController`

`@AuthenticationPrincipal Jwt` es el pase ya validado.

```java
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

	@GetMapping("/me")
	public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("mensaje", "API privada: el access token es válido.");
		body.put("sub", jwt.getSubject());
		body.put("aud", jwt.getAudience());
		body.put("scp", jwt.getClaimAsString("scp"));
		body.put("preferred_username", jwt.getClaimAsString("preferred_username"));
		body.put("oid", jwt.getClaimAsString("oid"));
		body.put("iss", jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
		return body;
	}
}
```

**Cómo comprobar:** existen `GET /public/hola` y `GET /api/me`.

---

## Paso 6 — El token sale de `msal-front` (no cambies scopes)

No toques `loginRequest.ts`. Sigue con `openid`, `profile`, `User.Read`.

1. Arranca `msal-front` (`npm run dev`) e inicia sesión.
2. En **Perfil**, copia el **Access Token** (el JWT largo, no el ID Token).
3. Ese es el Bearer para Postman / curl.

En [jwt.io](https://jwt.io) vas a ver `aud` de Graph (`00000003-0000-0000-c000-000000000000`) y `scp` con `User.Read`. En esta clase **eso está bien**: Spring no filtra audience.

**Cómo comprobar:** el access token tiene `iss` con tu tenant. El ID Token **no** lo uses como Bearer (es el carnet).

---

## Paso 7 — Arrancar y probar

```bash
cd msal-api
./mvnw spring-boot:run
```

```bash
# 1) Pública, sin token → 200
curl -i http://localhost:8080/public/hola

# 2) Privada, sin token → 401
curl -i http://localhost:8080/api/me

# 3) Privada, con el access token del front
curl -i http://localhost:8080/api/me \
  -H "Authorization: Bearer PEGA_AQUI_EL_ACCESS_TOKEN"
```

| # | Qué haces | Qué deberías ver |
|---|---|---|
| 1 | `/public/hola` | JSON con el mensaje público |
| 2 | `/api/me` sin header | **401** |
| 3 | `/api/me` + access token de `msal-front` | **200**, `preferred_username`, `scp` |
| 4 | Bearer = **ID Token** | suele fallar o no es el ejercicio: usa el access token |

Detén con `Ctrl+C`.

### Checklist

- [ ] Tengo el tenant ID de la App Registration de `msal-front`
- [ ] Entiendo: React = **client**, Spring = **resource server**, Entra = **auth server**
- [ ] `application.yml` tiene `issuer-uri` real (sin `REEMPLAZAR`)
- [ ] `/public/**` sin token; `/api/**` con JWT
- [ ] El Bearer es el **access token**, no el ID Token
- [ ] `./mvnw test` pasa

**Próxima clase:** Expose an API (`api://…/access_as_user`) y `audiences` en Spring, para que un token de Graph **ya no** sirva: el pase tiene que ser **para tu API**.

**Integración con el front:** cuando el back funcione con Postman/curl, sigue **`../msal-front/GUIA_INTEGRACION.md`** para conectar React con `/public/hola` y `/api/me`.

**Access token real:** después de la integración básica, sigue **`../GUIA_ACCESS_TOKEN.md`** (PDF: `GUIA_ACCESS_TOKEN.pdf`) para Expose an API y `audiences`.

---

## Si algo falla

| Síntoma | Qué significa | Qué hacer |
|---|---|---|
| App no arranca / error al bajar OpenID config | Tenant mal pegado o sin red | Revisa `issuer-uri` (GUID del directory + `/v2.0`) |
| `/api/me` 401 | Falta header, token cortado, o ID Token | Copia el access token entero de Perfil |
| `AADSTS50011` | Redirect del front | No es Spring; URI SPA = `http://localhost:5173` |
| CORS en el navegador | Origen no listado | Front en 5173 o 4200 |

---

## Comandos rápidos

```bash
cd msal-api
./mvnw test
./mvnw spring-boot:run    # http://localhost:8080
```

```bash
curl http://localhost:8080/public/hola
curl -i http://localhost:8080/api/me
```
