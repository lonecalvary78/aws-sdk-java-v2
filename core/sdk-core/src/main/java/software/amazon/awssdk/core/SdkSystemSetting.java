/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.core;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * System properties to configure the SDK runtime.
 */
@SdkProtectedApi
public enum SdkSystemSetting implements SystemSetting {
    /**
     * Configure the AWS access key ID.
     *
     * This value will not be ignored if the {@link #AWS_SECRET_ACCESS_KEY} is not specified.
     */
    AWS_ACCESS_KEY_ID("aws.accessKeyId", null),

    /**
     * Configure the AWS secret access key.
     *
     * This value will not be ignored if the {@link #AWS_ACCESS_KEY_ID} is not specified.
     */
    AWS_SECRET_ACCESS_KEY("aws.secretAccessKey", null),

    /**
     * Configure the AWS session token.
     */
    AWS_SESSION_TOKEN("aws.sessionToken", null),

    /**
     * Configure the AWS account id associated with credentials supplied through system properties.
     */
    AWS_ACCOUNT_ID("aws.accountId", null),

    /**
     * Configure the AWS web identity token file path.
     */
    AWS_WEB_IDENTITY_TOKEN_FILE("aws.webIdentityTokenFile", null),

    /**
     * Configure the AWS role arn.
     */
    AWS_ROLE_ARN("aws.roleArn", null),

    /**
     * Configure the session name for a role.
     */
    AWS_ROLE_SESSION_NAME("aws.roleSessionName", null),

    /**
     * Configure the default region.
     */
    AWS_REGION("aws.region", null),

    /**
     * Whether to load information such as credentials, regions from EC2 Metadata instance service.
     */
    AWS_EC2_METADATA_DISABLED("aws.disableEc2Metadata", "false"),

    /**
     * Whether to disable fallback to insecure EC2 Metadata instance service v1 on errors or timeouts.
     */
    AWS_EC2_METADATA_V1_DISABLED("aws.disableEc2MetadataV1", null),

    /**
     * The EC2 instance metadata service endpoint.
     *
     * This allows a service running in EC2 to automatically load its credentials and region without needing to configure them
     * in the SdkClientBuilder.
     */
    AWS_EC2_METADATA_SERVICE_ENDPOINT("aws.ec2MetadataServiceEndpoint", "http://169.254.169.254"),

    AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE("aws.ec2MetadataServiceEndpointMode", "IPv4"),

    /**
     * The number of seconds (either as an integer or double) before a connection to the instance
     * metadata service should time out. This value is applied to both the socket connect and read timeouts.
     *
     * The timeout can be configured using the system property "aws.ec2MetadataServiceTimeout". If not set,
     * a default timeout is used. This setting is crucial for ensuring timely responses from the instance
     * metadata service in environments with varying network conditions.
     */
    AWS_METADATA_SERVICE_TIMEOUT("aws.ec2MetadataServiceTimeout", "1"),

    /**
     * The elastic container metadata service endpoint that should be called by the ContainerCredentialsProvider
     * when loading data from the container metadata service.
     *
     * This allows a service running in an elastic container to automatically load its credentials without needing to configure
     * them in the SdkClientBuilder.
     *
     * This is not used if the {@link #AWS_CONTAINER_CREDENTIALS_RELATIVE_URI} is not specified.
     */
    AWS_CONTAINER_SERVICE_ENDPOINT("aws.containerServiceEndpoint", "http://169.254.170.2"),

    /**
     * The elastic container metadata service path that should be called by the ContainerCredentialsProvider when
     * loading credentials form the container metadata service. If this is not specified, credentials will not be automatically
     * loaded from the container metadata service.
     *
     * @see #AWS_CONTAINER_SERVICE_ENDPOINT
     */
    AWS_CONTAINER_CREDENTIALS_RELATIVE_URI("aws.containerCredentialsPath", null),

    /**
     * The full URI path to a localhost metadata service to be used.
     */
    AWS_CONTAINER_CREDENTIALS_FULL_URI("aws.containerCredentialsFullUri", null),

    /**
     * An authorization token to pass to a container metadata service, only used when {@link #AWS_CONTAINER_CREDENTIALS_FULL_URI}
     * is specified.
     *
     * @see #AWS_CONTAINER_CREDENTIALS_FULL_URI
     */
    AWS_CONTAINER_AUTHORIZATION_TOKEN("aws.containerAuthorizationToken", null),

    /**
     * The absolute file path containing the authorization token in plain text to pass to a container metadata
     * service, only used when {@link #AWS_CONTAINER_CREDENTIALS_FULL_URI} is specified.
     * @see #AWS_CONTAINER_CREDENTIALS_FULL_URI
     */
    AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE("aws.containerAuthorizationTokenFile", null),

    /**
     * Explicitly identify the default synchronous HTTP implementation the SDK will use. Useful
     * when there are multiple implementations on the classpath or as a performance optimization
     * since implementation discovery requires classpath scanning.
     */
    SYNC_HTTP_SERVICE_IMPL("software.amazon.awssdk.http.service.impl", null),

    /**
     * Explicitly identify the default Async HTTP implementation the SDK will use. Useful
     * when there are multiple implementations on the classpath or as a performance optimization
     * since implementation discovery requires classpath scanning.
     */
    ASYNC_HTTP_SERVICE_IMPL("software.amazon.awssdk.http.async.service.impl", null),

    /**
     * Whether CBOR optimization should automatically be used if its support is found on the classpath and the service supports
     * CBOR-formatted JSON.
     */
    CBOR_ENABLED("aws.cborEnabled", "true"),

    /**
     * Whether binary ION representation optimization should automatically be used if the service supports ION.
     */
    BINARY_ION_ENABLED("aws.binaryIonEnabled", "true"),

    /**
     * The execution environment of the SDK user. This is automatically set in certain environments by the underlying AWS service.
     * For example, AWS Lambda will automatically specify a runtime indicating that the SDK is being used within Lambda.
     */
    AWS_EXECUTION_ENV("aws.executionEnvironment", null),

    /**
     * Whether endpoint discovery should be enabled.
     */
    AWS_ENDPOINT_DISCOVERY_ENABLED("aws.endpointDiscoveryEnabled", null),

    /**
     * The S3 regional endpoint setting for the {@code us-east-1} region. Setting the value to {@code regional} causes
     * the SDK to use the {@code s3.us-east-1.amazonaws.com} endpoint when using the {@code US_EAST_1} region instead of
     * the global {@code s3.amazonaws.com}. Using the regional endpoint is disabled by default.
     */
    AWS_S3_US_EAST_1_REGIONAL_ENDPOINT("aws.s3UseUsEast1RegionalEndpoint", null),

    AWS_S3_DISABLE_EXPRESS_SESSION_AUTH("aws.disableS3ExpressAuth", null),

    /**
     * Which {@link RetryMode} to use for the default {@link RetryPolicy}, when one is not specified at the client level.
     */
    AWS_RETRY_MODE("aws.retryMode", null),

    /**
     * Defines the default value for {@link RetryPolicy.Builder#numRetries(Integer)}, if the retry count is not overridden in the
     * retry policy configured via {@link ClientOverrideConfiguration.Builder#retryPolicy(RetryPolicy)}. This is one more than
     * the number of retries, so aws.maxAttempts = 1 is 0 retries.
     */
    AWS_MAX_ATTEMPTS("aws.maxAttempts", null),

    /**
     * Which {@code DefaultsMode} to use, case insensitive
     */
    AWS_DEFAULTS_MODE("aws.defaultsMode", null),

    /**
     * Which {@code AccountIdEndpointMode} to use, case insensitive
     */
    AWS_ACCOUNT_ID_ENDPOINT_MODE("aws.accountIdEndpointMode", null),

    /**
     * Defines whether dualstack endpoints should be resolved during default endpoint resolution instead of non-dualstack
     * endpoints.
     */
    AWS_USE_DUALSTACK_ENDPOINT("aws.useDualstackEndpoint", null),

    /**
     * Defines whether fips endpoints should be resolved during default endpoint resolution instead of non-fips endpoints.
     */
    AWS_USE_FIPS_ENDPOINT("aws.useFipsEndpoint", null),

    /**
     * Whether request compression is disabled for operations marked with the RequestCompression trait. The default value is
     * false, i.e., request compression is enabled.
     */
    AWS_DISABLE_REQUEST_COMPRESSION("aws.disableRequestCompression", null),

    /**
     * Defines the minimum compression size in bytes, inclusive, for a request to be compressed. The default value is 10_240.
     * The value must be non-negative and no greater than 10_485_760.
     */
    AWS_REQUEST_MIN_COMPRESSION_SIZE_BYTES("aws.requestMinCompressionSizeBytes", null),

    /**
     * Defines a file path from which partition metadata should be loaded. If this isn't specified, the partition
     * metadata deployed with the SDK client will be used instead.
     */
    AWS_PARTITIONS_FILE("aws.partitionsFile", null),

    /**
     * The request checksum calculation setting. The default value is WHEN_SUPPORTED.
     */
    AWS_REQUEST_CHECKSUM_CALCULATION("aws.requestChecksumCalculation", null),

    /**
     * The response checksum validation setting. The default value is WHEN_SUPPORTED.
     */
    AWS_RESPONSE_CHECKSUM_VALIDATION("aws.responseChecksumValidation", null),

    /**
     * Configure an optional identification value to be appended to the user agent header.
     * The value should be less than 50 characters in length and is null by default.
     */
    AWS_SDK_UA_APP_ID("sdk.ua.appId", null),

    /**
     * Configure the SIGV4A signing region set.
     * This is a non-empty, comma-delimited list of AWS region names used during signing.
     */
    AWS_SIGV4A_SIGNING_REGION_SET("aws.sigv4a.signing.region.set", null),


    /**
     * Configure the preferred auth scheme to use.
     * This is a comma-delimited list of AWS auth scheme names used during signing.
     */
    AWS_AUTH_SCHEME_PREFERENCE("aws.authSchemePreference", null);

    private final String systemProperty;
    private final String defaultValue;

    SdkSystemSetting(String systemProperty, String defaultValue) {
        this.systemProperty = systemProperty;
        this.defaultValue = defaultValue;
    }

    @Override
    public String property() {
        return systemProperty;
    }

    @Override
    public String environmentVariable() {
        return name();
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }
}
