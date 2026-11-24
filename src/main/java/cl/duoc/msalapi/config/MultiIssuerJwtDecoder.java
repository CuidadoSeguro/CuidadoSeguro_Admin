package cl.duoc.msalapi.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Acepta exclusivamente JWT firmados por los emisores configurados:
 * Microsoft Entra ID y Amazon Cognito.
 *
 * El claim iss se usa únicamente para seleccionar el decoder. La firma,
 * issuer, timestamps y claims específicos se validan criptográficamente
 * después con el decoder correspondiente.
 */
public final class MultiIssuerJwtDecoder implements JwtDecoder {

    private final String azureIssuer;
    private final String azureClientId;
    private final String cognitoIssuer;
    private final String cognitoClientId;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentMap<String, JwtDecoder> decoders = new ConcurrentHashMap<>();

    public MultiIssuerJwtDecoder(
            String azureIssuer,
            String azureClientId,
            String cognitoIssuer,
            String cognitoClientId) {

        this.azureIssuer = normalize(azureIssuer);
        this.azureClientId = normalize(azureClientId);
        this.cognitoIssuer = normalize(cognitoIssuer);
        this.cognitoClientId = normalize(cognitoClientId);

        if (this.azureIssuer.isBlank() || this.azureClientId.isBlank()) {
            throw new IllegalArgumentException(
                "Debe configurar AZURE_ISSUER_URI y AZURE_CLIENT_ID."
            );
        }
        if (this.cognitoIssuer.isBlank() || this.cognitoClientId.isBlank()
                || this.cognitoClientId.startsWith("REEMPLAZAR_")) {
            throw new IllegalArgumentException(
                "Debe configurar COGNITO_ISSUER_URI y COGNITO_CLIENT_ID con el App Client público de Cognito (sin secret)."
            );
        }
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        String issuer = readIssuerWithoutTrustingIt(token);

        if (azureIssuer.equals(issuer)) {
            return decoders.computeIfAbsent(azureIssuer, this::buildAzureDecoder).decode(token);
        }

        if (cognitoIssuer.equals(issuer)) {
            return decoders.computeIfAbsent(cognitoIssuer, this::buildCognitoDecoder).decode(token);
        }

        throw new JwtException("JWT issuer no permitido: " + issuer);
    }

    private JwtDecoder buildAzureDecoder(String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            List<String> audiences = jwt.getAudience();
            String appUri = "api://" + azureClientId;

            boolean validAudience = audiences != null
                    && (audiences.contains(azureClientId) || audiences.contains(appUri));

            return validAudience
                    ? OAuth2TokenValidatorResult.success()
                    : failure("La audiencia del token Microsoft no corresponde a esta API.");
        };

        OAuth2TokenValidator<Jwt> scopeValidator = jwt -> {
            String scopes = jwt.getClaimAsString("scp");
            boolean validScope = scopes != null
                    && List.of(scopes.split("\\s+")).contains("access_as_user");

            return validScope
                    ? OAuth2TokenValidatorResult.success()
                    : failure("El access token Microsoft no contiene el scope access_as_user.");
        };

        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                audienceValidator,
                scopeValidator
        ));

        return decoder;
    }

    private JwtDecoder buildCognitoDecoder(String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();

        OAuth2TokenValidator<Jwt> tokenUseValidator = jwt -> {
            String tokenUse = jwt.getClaimAsString("token_use");
            return "access".equals(tokenUse)
                    ? OAuth2TokenValidatorResult.success()
                    : failure("Cognito requiere un Access Token (token_use=access), no un ID Token.");
        };

        OAuth2TokenValidator<Jwt> clientIdValidator = jwt -> {
            String clientId = jwt.getClaimAsString("client_id");
            return cognitoClientId.equals(clientId)
                    ? OAuth2TokenValidatorResult.success()
                    : failure("El Access Token de Cognito no pertenece al App Client configurado.");
        };

        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                tokenUseValidator,
                clientIdValidator
        ));

        return decoder;
    }

    private String readIssuerWithoutTrustingIt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new JwtException("Formato JWT inválido.");
            }

            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode claims = objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));
            JsonNode issuerNode = claims.get("iss");

            if (issuerNode == null || !issuerNode.isTextual()) {
                throw new JwtException("El JWT no contiene un issuer válido.");
            }

            return issuerNode.asText();
        } catch (JwtException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtException("No fue posible leer el issuer del JWT.", ex);
        }
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", description, null)
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("/+$", "");
    }
}
