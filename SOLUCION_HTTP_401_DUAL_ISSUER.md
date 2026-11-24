# Solución HTTP 401 — validación dual Entra ID + Amazon Cognito

## Qué se encontró en el backend recibido

El `application.yml` original configuraba Spring Security como Resource Server de un único emisor Cognito mediante `spring.security.oauth2.resourceserver.jwt.issuer-uri`. En cambio, el frontend también envía Access Tokens de Microsoft Entra ID. Un token Entra no puede superar la validación si el backend solo confía en el issuer de Cognito.

Además, `MeController` solo devolvía el claim `roles`, mientras que Cognito suele entregar grupos en `cognito:groups`.

## Cambios incluidos

- `MultiIssuerJwtDecoder.java`: selecciona entre los dos emisores permitidos y valida firma, issuer, timestamps y claims de proveedor.
- `SecurityConfig.java`: registra el decoder multi-issuer; conserva `/public/**` público y exige JWT válido en `/api/**`.
- `application.yml`: configura issuer y client ID de Entra y Cognito mediante variables de entorno.
- `MeController.java`: devuelve `roles` de Entra o `cognito:groups` de Cognito en una propiedad `roles` uniforme.
- `docker-compose.yml`: pasa las variables de ambos proveedores al contenedor.
- `.env.example`: ejemplo sin secretos.

## Configuración local obligatoria

1. Copia `.env.example` a `.env`.
2. Confirma estos valores de Entra:
   - `AZURE_ISSUER_URI=https://login.microsoftonline.com/95cd823a-c239-4ece-b6ea-932724964971/v2.0`
   - `AZURE_CLIENT_ID=3912eb25-8b20-4725-9b9d-18a99c419ead`
3. En Cognito, utiliza el ID del App Client público nuevo, sin secret:
   - `COGNITO_ISSUER_URI=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_RP2GidqrX`
   - `COGNITO_CLIENT_ID=<ID_DEL_APP_CLIENT_PUBLICO>`
4. No reutilices el App Client antiguo `t68vnb4vklnodqcpsep1s4vik` si conserva Client Secret. El frontend SPA no puede firmar `SECRET_HASH`; crea/usa uno público sin secret y habilita `USER_PASSWORD_AUTH`.
5. Ajusta `DB_*`, Kafka y URLs de servicios según tu ambiente.

## Requisitos del token Microsoft

El frontend pide el scope:

`api://3912eb25-8b20-4725-9b9d-18a99c419ead/access_as_user`

El Access Token enviado a Spring debe incluir:
- `iss` igual a `AZURE_ISSUER_URI`.
- `aud` igual al GUID `AZURE_CLIENT_ID` o `api://` + ese GUID.
- `scp` que contenga `access_as_user`.

Si el access token es para Microsoft Graph (`aud=00000003-0000-0000-c000-000000000000`) o es un ID Token, será rechazado deliberadamente con 401. Entra debe emitir un Access Token destinado a esta API y la App Registration debe exponer el scope indicado.

## Requisitos del token Cognito

El frontend debe enviar el `AccessToken` devuelto por `InitiateAuth`, no el `IdToken`. El decoder valida:
- `iss` igual al issuer del User Pool.
- `token_use=access`.
- `client_id` igual a `COGNITO_CLIENT_ID`.

El Access Token de Cognito normalmente identifica al App Client con `client_id`, no con `aud`.

## Ejecutar y probar

Desde la carpeta del backend:

```bash
# Windows
copy .env.example .env

# Revisar y editar .env, especialmente COGNITO_CLIENT_ID
mvnw.cmd clean test
mvnw.cmd spring-boot:run
```

Pruebas:
- `GET http://localhost:8080/public/isALive` sin token: debe seguir respondiendo 200.
- `GET http://localhost:8080/api/login` sin token: debe responder 401.
- `GET http://localhost:8080/api/login` con Access Token API de Entra: debe responder 200.
- `GET http://localhost:8080/api/login` con Access Token de Cognito del App Client público: debe responder 200.
- ID Token, token Graph, token de otro tenant o client ID deben ser rechazados.

Ejemplo PowerShell:

```powershell
$token = "<PEGAR_ACCESS_TOKEN>"
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/login" `
  -Headers @{ Authorization = "Bearer $token" }
```

## Seguridad

No se elimina la autenticación de `/api/login`, no se aceptan JWT sin verificar y no se concede rol por defecto. La firma y los claims se validan antes de que el controlador reciba el JWT.

El archivo `.env` original y el historial `.git` no se incluyen en este ZIP para evitar distribuir credenciales o información privada.
