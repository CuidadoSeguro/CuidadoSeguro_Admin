# Alta de pacientes con cuenta Amazon Cognito

## Flujo

- El panel Angular solicita contraseña y confirmación al crear un paciente.
- El backend Spring Boot valida la solicitud, crea el usuario de Cognito usando el correo como username y define la contraseña como permanente.
- Se agrega al grupo `PACIENTE` (el backend lo crea si aún no existe).
- Después se guarda el paciente en MySQL. Si falla el guardado SQL, el backend intenta eliminar la cuenta recién creada en Cognito.
- La contraseña nunca se almacena en MySQL ni se devuelve en la respuesta.

## Configuración

En el `.env` del backend configura:

```dotenv
AWS_REGION=us-east-1
COGNITO_USER_POOL_ID=us-east-1_RP2GidqrX
COGNITO_PATIENT_GROUP=PACIENTE
```

Configura credenciales AWS para el entorno local mediante `AWS_ACCESS_KEY_ID` y `AWS_SECRET_ACCESS_KEY`, o mediante un perfil AWS local. En despliegues, usa un IAM Role y evita credenciales estáticas.

El principal/rol IAM necesita, como mínimo, permisos sobre el User Pool configurado para:

- `cognito-idp:AdminCreateUser`
- `cognito-idp:AdminSetUserPassword`
- `cognito-idp:AdminAddUserToGroup`
- `cognito-idp:AdminCreateGroup`
- `cognito-idp:AdminDeleteUser`

Restringe los permisos al ARN del User Pool. No entregues estas credenciales al frontend.

El App Client de Cognito que usa el paciente para iniciar sesión debe ser público (sin client secret) y tener habilitado un flujo de autenticación compatible con el login implementado en Angular (por ejemplo `ALLOW_USER_PASSWORD_AUTH`, si ese es el flujo que usa el frontend). El `COGNITO_CLIENT_ID` de validación de tokens debe seguir siendo el ID de ese App Client.

## Contraseña

La contraseña ingresada queda como permanente. Cognito aplicará la política de contraseñas definida en el User Pool. Verifica que la política admita los requisitos que muestra el formulario (mínimo 8 caracteres); si tu política exige mayúsculas, números o símbolos, ajusta el validador del frontend para reflejarla.

## Endpoint

`POST /api/pacientes` ahora recibe los datos del paciente más `password`. Solo un administrador autenticado puede invocarlo. La respuesta contiene el paciente guardado, nunca la contraseña.

## Consideraciones

La creación de Cognito y MySQL no comparte una transacción distribuida. El backend aplica una compensación intentando borrar el usuario Cognito si falla la escritura SQL; monitorea los errores de compensación en CloudWatch/logs.
