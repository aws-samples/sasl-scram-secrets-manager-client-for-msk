package com.amazonaws.kafka.samples.saslscram;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;

class Clients {

    static SecretsManagerAsyncClient createSecretsManagerClient(Region region) {

        return SecretsManagerAsyncClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.builder()
                        .asyncCredentialUpdateEnabled(true)
                        .build())
                .region(region).build();
    }

}
