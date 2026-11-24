# Corrección de credenciales AWS para Cognito

## Causa
El backend construía `CognitoIdentityProviderClient` con la cadena predeterminada del SDK. El archivo `.env` contiene propiedades para Spring, pero las credenciales no estaban definidas; por eso el SDK no encontraba ninguna identidad IAM.

## Desarrollo local con `.env`
1. Copia `.env.example` como `.env` (o conserva tu `.env` actual y añade las variables faltantes).
2. Completa `AWS_ACCESS_KEY_ID` y `AWS_SECRET_ACCESS_KEY` con una access key IAM de permisos mínimos para el User Pool configurado.
3. Si utilizas credenciales temporales STS, completa también `AWS_SESSION_TOKEN`.
4. Confirma `AWS_REGION=us-east-1` y `COGNITO_USER_POOL_ID=us-east-1_RP2GidqrX`.
5. Reinicia Spring Boot. Si usas Docker Compose: `docker compose up --build -d`.

El `CognitoConfig` actualizado lee las credenciales desde el entorno Spring (incluido spring-dotenv). Si ambas claves están configuradas, usa credenciales estáticas; si no están, recurre a `DefaultCredentialsProvider` (perfil AWS, variables del sistema o IAM Role). Si solo una de las dos claves está presente, el backend falla con un mensaje claro.

## Seguridad
- No compartas ni subas `.env`.
- No incluyas credenciales en `environment.ts`, Angular, repositorios ni capturas.
- No uses permisos `AdministratorAccess`.
- Limita IAM a acciones Cognito necesarias y al ARN de este User Pool:
  `arn:aws:cognito-idp:us-east-1:487929298141:userpool/us-east-1_RP2GidqrX`
- En AWS desplegado, es preferible asignar un IAM Role a la tarea/instancia y no guardar access keys.

## Variables requeridas
`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `COGNITO_USER_POOL_ID`.
`AWS_SESSION_TOKEN` solo si las credenciales son temporales.
