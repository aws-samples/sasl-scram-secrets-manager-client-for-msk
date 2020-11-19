This is a library intended to be used with Apache Kafka producers and consumers against an Amazon MSK Apache Kafka cluster 
utilizing SASL/SCRAM authentication, and administrative applications that manage secrets for SASL/SCRAM authentication. 
As explained in the [Amazon MSK documentation](https://docs.aws.amazon.com/msk/latest/developerguide/msk-password.html), 
Amazon MSK support for SASL/SCRAM authentication uses [AWS Secrets Manager](https://aws.amazon.com/secrets-manager/) to store 
usernames and passwords in secrets in AWS Secrets Manager and provides the ability to secure those secrets by encrypting 
them with Customer Master Keys (CMKs) from AWS Key Management Service (KMS) and attaching a Resource Policy to control 
who has access to the secret. 

For Apache Kafka producers and consumers to be able use the library to read the secrets from AWS Secrets Manager and 
configure the SASL/SCRAM properties, just the getSecretsManagerClient and the getSecret methods need to be used. All 
other methods are for managing secrets. Here is an example of code required to get a secret for Amazon MSK from 
AWS Secrets Manager that could be used in a producer or consumer.

```
String secretNamePrefix = "AmazonMSK_";
String saslscramUser = "nancy";
String region = "us-east-1"
String secret = Secrets.getSecret(secretNamePrefix + saslscramUser, Secrets.getSecretsManagerClient(region));
```

## Install

### Install the jar file.  

    mvn clean install -f pom.xml
