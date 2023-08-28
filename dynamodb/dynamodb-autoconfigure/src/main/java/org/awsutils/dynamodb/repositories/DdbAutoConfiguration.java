package org.awsutils.dynamodb.repositories;

import org.apache.commons.lang3.StringUtils;
import org.awsutils.common.config.AwsEnvironmentProperties;
import org.awsutils.dynamodb.config.DynamoDbProperties;
import org.awsutils.dynamodb.config.EntityValidationConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@SuppressWarnings("rawtypes")
@Configuration
@ConditionalOnClass({DynamoDbRepository.class})
@EnableConfigurationProperties({AwsEnvironmentProperties.class, DynamoDbProperties.class})
@Import(DataMapperConfig.class)
public class DdbAutoConfiguration {
    @Bean(name = "dynamoDbAsyncClientBuilder")
    @ConditionalOnBean(SdkAsyncHttpClient.class)
    public DynamoDbAsyncClientBuilder dynamoDbAsyncClientBuilder(final SdkAsyncHttpClient selectedSdkAsyncHttpClient,
                                                                 final AwsEnvironmentProperties dynamoDbProperties) throws URISyntaxException {

        final var builder = DynamoDbAsyncClient
                .builder()
                .region(Region.of(dynamoDbProperties.getRegion()))
                .httpClient(selectedSdkAsyncHttpClient);

        if(dynamoDbProperties.isLocalAwsMode() && !StringUtils.isEmpty(dynamoDbProperties.getLocalAwsEndpoint())) {
            return builder.endpointOverride(new URI(dynamoDbProperties.getLocalAwsEndpoint()));
        }

        return builder;
    }

    @Bean(name = "dynamoDbAsyncClientBuilder")
    @ConditionalOnMissingBean(SdkAsyncHttpClient.class)
    public DynamoDbAsyncClientBuilder dynamoDbAsyncClientBuilder2(final AwsEnvironmentProperties dynamoDbProperties) throws URISyntaxException {

        final var builder = DynamoDbAsyncClient
                .builder()
                .region(Region.of(dynamoDbProperties.getRegion()));

        if(dynamoDbProperties.isLocalAwsMode() && !StringUtils.isEmpty(dynamoDbProperties.getLocalAwsEndpoint())) {
            return builder.endpointOverride(new URI(dynamoDbProperties.getLocalAwsEndpoint()));
        }

        return builder;
    }

    @Bean
    @ConditionalOnBean(name = "staticCredentialsProvider")
    @ConditionalOnProperty(prefix = "org.awsutils.aws", value = {"region"})
    public DynamoDbAsyncClient amazonDynamoDB(final AwsCredentialsProvider staticCredentialsProvider,
                                              final DynamoDbAsyncClientBuilder dynamoDbAsyncClientBuilder,
                                              final AwsEnvironmentProperties dynamoDbProperties) throws URISyntaxException {

        return dynamoDbAsyncClientBuilder
                .credentialsProvider(staticCredentialsProvider)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "staticCredentialsProvider")
    @ConditionalOnProperty(prefix = "org.awsutils.aws", value = {"region"})
    public DynamoDbAsyncClient amazonDynamoDBEnv(final DynamoDbAsyncClientBuilder dynamoDbAsyncClientBuilder) {
        return dynamoDbAsyncClientBuilder.build();
    }

    @Bean(name = "entityValidationConfigMain")
    @ConditionalOnProperty(prefix = "org.awsutils.aws.ddb", value = "entity-base-package")
    public EntityValidationConfig entityValidationConfigMain(final DynamoDbProperties dynamoDbProperties) {
        return new EntityValidationConfig(dynamoDbProperties.getEntityBasePackage());
    }

    @Bean(name = "dataMapperConfigCleanUpMain")
    @ConditionalOnProperty(prefix = "org.awsutils.aws.ddb", value = "entity-base-package")
    public DataMapperConfigCleanUp dataMapperConfigCleanUpMain(final DynamoDbProperties dynamoDbProperties, final Map<Class, DataMapper> dataMapperMap, final Environment environment) {
        return new DataMapperConfigCleanUp(dynamoDbProperties.getEntityBasePackage(), dataMapperMap, environment);
    }
}