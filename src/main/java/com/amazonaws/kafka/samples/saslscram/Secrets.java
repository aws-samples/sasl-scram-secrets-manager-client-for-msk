package com.amazonaws.kafka.samples.saslscram;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.model.*;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

public class Secrets {

    private static final Logger logger = LogManager.getLogger(Secrets.class);

    public static SecretsManagerAsyncClient getSecretsManagerClient(String region) {
        return Clients.createSecretsManagerClient(Region.of(region));
    }

    public static String getSecret(String secretId, SecretsManagerAsyncClient secretsManagerClient) {
        String secret;
        ByteBuffer binarySecretData;
        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                .secretId(secretId)
                .versionStage("AWSCURRENT")
                .build();

        GetSecretValueResponse getSecretValueResponse = null;
        try {
            logger.info("Retrieving secret for secret id {} \n", secretId);
            getSecretValueResponse = secretsManagerClient.getSecretValue(getSecretValueRequest).get();
        } catch (InterruptedException | ExecutionException e) {
            if (e.getCause() instanceof ResourceNotFoundException) {
                logger.error("The requested secret {} was not found. Error: {} \n", secretId, Util.stackTrace(e));
                throw new RuntimeException(String.format("Error retrieving secret %s. Cannot proceed.\n", secretId));
            }
            if (e.getCause() instanceof InvalidRequestException) {
                logger.error("The request was invalid. Error: {} \n", Util.stackTrace(e));
                throw new RuntimeException("The request was invalid. Cannot proceed.\n");
            }
            if (e.getCause() instanceof InvalidParameterException) {
                logger.error("The request had invalid params. Error: {} \n", Util.stackTrace(e));
                throw new RuntimeException("The request had invalid params. Cannot proceed.\n");
            }
        }

        if (getSecretValueResponse == null) {
            logger.error("Retrieved secret value is null. Cannot proceed. \n");
            throw new RuntimeException("Retrieved secret vale is null. Cannot proceed. \n");
        }

        // Depending on whether the secret was a string or binary, one of these fields will be populated
        logger.info("Successfully retrieved secret for secret id {} \n", secretId);
        if (getSecretValueResponse.secretString() != null) {
            secret = getSecretValueResponse.secretString();
            return secret;
        } else {
            binarySecretData = getSecretValueResponse.secretBinary().asByteBuffer();
            return binarySecretData.toString();
        }
    }

    private static String buildResourcePolicy(String secretArn) {
        JsonObjectBuilder jsonPrincipal = Json.createObjectBuilder()
                .add("Service", "kafka.amazonaws.com");
        JsonObjectBuilder jsonStatement = Json.createObjectBuilder()
                .add("Effect", "Allow")
                .add("Principal", jsonPrincipal)
                .add("Action", "secretsmanager:getSecretValue")
                .add("Resource", secretArn);

        return Json.createObjectBuilder()
                .add("Version", "2012-10-17")
                .add("Statement", Json.createArrayBuilder()
                        .add(jsonStatement))
                .build()
                .toString();
    }

    public static void putResourcePolicyOnSecret(String resourcePolicy, String secretId, SecretsManagerAsyncClient secretsManagerClient) throws ExecutionException, InterruptedException {

        String secretResourcePolicy;
        if (resourcePolicy == null) {
            secretResourcePolicy = buildResourcePolicy(secretId);
        } else
            secretResourcePolicy = resourcePolicy;
        PutResourcePolicyRequest putResourcePolicyRequest = PutResourcePolicyRequest.builder()
                .secretId(secretId)
                .resourcePolicy(secretResourcePolicy)
                .build();
        logger.info("Adding resource policy to secret {}. \n", secretId);
        secretsManagerClient.putResourcePolicy(putResourcePolicyRequest).get();
    }

    public static String createSecret(String userName, String password, String kmsKeyId, SecretsManagerAsyncClient secretsManagerClient) throws ExecutionException, InterruptedException {
        String namePrefix = "AmazonMSK_";
        String jsonSecretString = Json.createObjectBuilder()
                .add("username", userName)
                .add("password", password)
                .build()
                .toString();
        CreateSecretRequest createSecretRequest = CreateSecretRequest.builder()
                .name(namePrefix + userName)
                .description("Amazon MSK secret for user " + userName)
                .kmsKeyId(kmsKeyId)
                .secretString(jsonSecretString)
                .build();
        logger.info("Creating secret with name {} for user {}. \n", namePrefix + userName, userName);
        String secretArn = secretsManagerClient.createSecret(createSecretRequest).get().arn();

        logger.info("Adding resource policy on secret to allow Amazon MSK access to the secret. \n");
        putResourcePolicyOnSecret(null, secretArn, secretsManagerClient);
        return secretArn;
    }

    public static Instant deleteSecret(String secretId, boolean immediateDelete, SecretsManagerAsyncClient secretsManagerClient) throws ExecutionException, InterruptedException {
        long defaultRecoveryWindowInDays = 7L;

        DeleteSecretRequest deleteSecretRequest;
        if (immediateDelete) {
            deleteSecretRequest = DeleteSecretRequest.builder()
                    .secretId(secretId)
                    .forceDeleteWithoutRecovery(immediateDelete)
                    .build();
        } else {
            deleteSecretRequest = DeleteSecretRequest.builder()
                    .secretId(secretId)
                    .recoveryWindowInDays(defaultRecoveryWindowInDays)
                    .build();
        }
        logger.info("Deleting secret {} with Immediate delete set to {}. \n", secretId, immediateDelete);
        return secretsManagerClient.deleteSecret(deleteSecretRequest).get().deletionDate();
    }

    public static void updateSecretValue(String userName, String password, String secretId, SecretsManagerAsyncClient secretsManagerClient) throws ExecutionException, InterruptedException {
        String jsonSecretString = Json.createObjectBuilder()
                .add("username", userName)
                .add("password", password)
                .build()
                .toString();
        PutSecretValueRequest putSecretValueRequest = PutSecretValueRequest.builder()
                .secretId(secretId)
                .secretString(jsonSecretString)
                .build();
        logger.info("Updating secret value for secret {}. \n", secretId);
        secretsManagerClient.putSecretValue(putSecretValueRequest).get();
    }
}
