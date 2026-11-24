package cl.duoc.msalapi.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupExistsException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

@Service
public class CognitoPacienteService {

    private final CognitoIdentityProviderClient cognito;
    private final String userPoolId;
    private final String patientGroup;

    public CognitoPacienteService(
            CognitoIdentityProviderClient cognito,
            @Value("${app.cognito.user-pool-id}") String userPoolId,
            @Value("${app.cognito.patient-group:PACIENTE}") String patientGroup) {
        this.cognito = cognito;
        this.userPoolId = userPoolId;
        this.patientGroup = patientGroup;
    }

    public void crearCuenta(String email, String nombreCompleto, String password) {
        String username = email.trim().toLowerCase();
        boolean creado = false;
        try {
            asegurarGrupoPaciente();
            cognito.adminCreateUser(AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .messageAction(MessageActionType.SUPPRESS)
                    .userAttributes(
                            AttributeType.builder().name("email").value(username).build(),
                            AttributeType.builder().name("email_verified").value("true").build(),
                            AttributeType.builder().name("name").value(nombreCompleto).build())
                    .build());
            creado = true;

            // La contraseña indicada por el administrador queda como permanente.
            cognito.adminSetUserPassword(AdminSetUserPasswordRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .password(password)
                    .permanent(true)
                    .build());

            cognito.adminAddUserToGroup(AdminAddUserToGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .groupName(patientGroup)
                    .build());
        } catch (UsernameExistsException ex) {
            throw new CuentaCognitoExistenteException("Ya existe una cuenta Cognito para el correo " + username, ex);
        } catch (CognitoIdentityProviderException ex) {
            if (creado) eliminarCuenta(username);
            String codigo = ex.awsErrorDetails() == null ? "" : ex.awsErrorDetails().errorCode();
            String detalle = ex.awsErrorDetails() == null ? "" : ex.awsErrorDetails().errorMessage();
            if ("InvalidPasswordException".equals(codigo)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La contraseña no cumple la política configurada en Cognito: " + detalle, ex);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Amazon Cognito no pudo completar el registro del usuario.", ex);
        } catch (RuntimeException ex) {
            if (creado) eliminarCuenta(username);
            throw ex;
        }
    }

    public void eliminarCuenta(String email) {
        try {
            cognito.adminDeleteUser(AdminDeleteUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email.trim().toLowerCase())
                    .build());
        } catch (CognitoIdentityProviderException ignored) {
            // La compensación es de mejor esfuerzo; el error original se conserva.
        }
    }

    private void asegurarGrupoPaciente() {
        try {
            cognito.createGroup(CreateGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .groupName(patientGroup)
                    .description("Usuarios pacientes de Cuidado Seguro")
                    .build());
        } catch (GroupExistsException ignored) {
            // El grupo ya está creado: comportamiento idempotente.
        }
    }

    public static class CuentaCognitoExistenteException extends RuntimeException {
        public CuentaCognitoExistenteException(String message, Throwable cause) { super(message, cause); }
    }
}
