package cl.duoc.msalapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
public class CognitoConfig {

    @Bean
    CognitoIdentityProviderClient cognitoIdentityProviderClient(
            @Value("${app.cognito.region:us-east-1}") String region,
            @Value("${AWS_ACCESS_KEY_ID:}") String accessKeyId,
            @Value("${AWS_SECRET_ACCESS_KEY:}") String secretAccessKey,
            @Value("${AWS_SESSION_TOKEN:}") String sessionToken) {

        AwsCredentialsProvider credentialsProvider;

        boolean hasAccessKey = accessKeyId != null && !accessKeyId.isBlank();
        boolean hasSecretKey = secretAccessKey != null && !secretAccessKey.isBlank();

        if (hasAccessKey != hasSecretKey) {
            throw new IllegalStateException(
                    "AWS_ACCESS_KEY_ID y AWS_SECRET_ACCESS_KEY deben configurarse juntos.");
        }

        if (hasAccessKey) {
            if (sessionToken != null && !sessionToken.isBlank()) {
                credentialsProvider = StaticCredentialsProvider.create(
                        AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken));
            } else {
                credentialsProvider = StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey));
            }
        } else {
            // En AWS (EC2/ECS/EKS), usar IAM Role. En desarrollo local también
            // se acepta ~/.aws/credentials o las variables del sistema.
            credentialsProvider = DefaultCredentialsProvider.create();
        }

        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
