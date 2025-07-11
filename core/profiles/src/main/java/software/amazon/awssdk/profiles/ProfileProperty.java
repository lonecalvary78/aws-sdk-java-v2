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

package software.amazon.awssdk.profiles;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * The properties used by the Java SDK from the credentials and config files.
 *
 * @see ProfileFile
 */
@SdkPublicApi
public final class ProfileProperty {
    /**
     * Property name for specifying the Amazon AWS Access Key
     */
    public static final String AWS_ACCESS_KEY_ID = "aws_access_key_id";

    /**
     * Property name for specifying the Amazon AWS Secret Access Key
     */
    public static final String AWS_SECRET_ACCESS_KEY = "aws_secret_access_key";

    /**
     * Property name for specifying the Amazon AWS Session Token
     */
    public static final String AWS_SESSION_TOKEN = "aws_session_token";

    /**
     * Property name for specifying the Amazon AWS Account ID associated with credentials
     */
    public static final String AWS_ACCOUNT_ID = "aws_account_id";

    /**
     * Property name for specifying the IAM role to assume
     */
    public static final String ROLE_ARN = "role_arn";

    /**
     * Property name for specifying the IAM role session name
     */
    public static final String ROLE_SESSION_NAME = "role_session_name";

    /**
     * Property name for specifying how long in seconds to assume the role
     */
    public static final String DURATION_SECONDS = "duration_seconds";

    /**
     * Property name for specifying the IAM role external id
     */
    public static final String EXTERNAL_ID = "external_id";

    /**
     * Property name for specifying the profile credentials to use when assuming a role
     */
    public static final String SOURCE_PROFILE = "source_profile";

    /**
     * Property name for specifying the credential source to use when assuming a role
     */
    public static final String CREDENTIAL_SOURCE = "credential_source";

    /**
     * AWS Region to use when creating clients.
     */
    public static final String REGION = "region";

    /**
     * Property name for specifying the identification number of the MFA device
     */
    public static final String MFA_SERIAL = "mfa_serial";

    /**
     * Property name for specifying whether or not endpoint discovery is enabled.
     */
    public static final String ENDPOINT_DISCOVERY_ENABLED = "aws_endpoint_discovery_enabled";

    /**
     * An external process that should be invoked to load credentials.
     */
    public static final String CREDENTIAL_PROCESS = "credential_process";

    public static final String WEB_IDENTITY_TOKEN_FILE = "web_identity_token_file";

    /**
     * The S3 regional endpoint setting for the {@code us-east-1} region. Setting the value to {@code regional} causes
     * the SDK to use the {@code s3.us-east-1.amazonaws.com} endpoint when using the {@code US_EAST_1} region instead of
     * the global {@code s3.amazonaws.com}. Using the regional endpoint is disabled by default.
     */
    public static final String S3_US_EAST_1_REGIONAL_ENDPOINT = "s3_us_east_1_regional_endpoint";

    public static final String DISABLE_S3_EXPRESS_AUTH = "s3_disable_express_session_auth";

    /**
     * The "retry mode" to be used for clients created using the currently-configured profile. Values supported by all SDKs are
     * "legacy" and "standard". See the {@code RetryMode} class JavaDoc for more information.
     */
    public static final String RETRY_MODE = "retry_mode";

    /**
     * The "defaults mode" to be used for clients created using the currently-configured profile. Defaults mode determins how SDK
     * default configuration should be resolved. See the {@code DefaultsMode} class JavaDoc for more
     * information.
     */
    public static final String DEFAULTS_MODE = "defaults_mode";

    /**
     * The "account id endpoint mode" to be used for clients created using the currently-configured profile.
     * This setting can only be used by services that route user requests to account specific endpoints, and determines
     * how endpoints should be resolved depending on the availability of an accountId for a request.
     *
     * See the {@code AccountIdEndpointMode} class javadoc for more information.
     */
    public static final String ACCOUNT_ID_ENDPOINT_MODE = "account_id_endpoint_mode";

    /**
     * Aws region where the SSO directory for the given 'sso_start_url' is hosted. This is independent of the general 'region'.
     */
    public static final String SSO_REGION = "sso_region";

    /**
     * The corresponding IAM role in the AWS account that temporary AWS credentials will be resolved for.
     */
    public static final String SSO_ROLE_NAME = "sso_role_name";

    /**
     * AWS account ID that temporary AWS credentials will be resolved for.
     */
    public static final String SSO_ACCOUNT_ID = "sso_account_id";

    /**
     * Start url provided by the SSO service via the console. It's the main URL used for login to the SSO directory.
     * This is also referred to as the "User Portal URL" and can also be used to login to the SSO web interface for AWS
     * console access.
     */
    public static final String SSO_START_URL = "sso_start_url";

    public static final String USE_DUALSTACK_ENDPOINT = "use_dualstack_endpoint";

    public static final String AUTH_SCHEME_PREFERENCE = "auth_scheme_preference";

    public static final String USE_FIPS_ENDPOINT = "use_fips_endpoint";

    public static final String EC2_METADATA_SERVICE_ENDPOINT_MODE = "ec2_metadata_service_endpoint_mode";

    public static final String EC2_METADATA_SERVICE_ENDPOINT = "ec2_metadata_service_endpoint";

    public static final String EC2_METADATA_V1_DISABLED = "ec2_metadata_v1_disabled";

    public static final String METADATA_SERVICE_TIMEOUT = "metadata_service_timeout";

    /**
     * Whether request compression is disabled for operations marked with the RequestCompression trait. The default value is
     * false, i.e., request compression is enabled.
     */
    public static final String DISABLE_REQUEST_COMPRESSION = "disable_request_compression";

    /**
     * The minimum compression size in bytes, inclusive, for a request to be compressed. The default value is 10_240.
     * The value must be non-negative and no greater than 10_485_760.
     */
    public static final String REQUEST_MIN_COMPRESSION_SIZE_BYTES = "request_min_compression_size_bytes";

    /**
     * The endpoint override to use. This may also be specified under a service-specific parent property to override the
     * endpoint just for that one service.
     */
    public static final String ENDPOINT_URL = "endpoint_url";

    /**
     * The request checksum calculation setting. The default value is WHEN_SUPPORTED.
     */
    public static final String REQUEST_CHECKSUM_CALCULATION = "request_checksum_calculation";

    /**
     * The response checksum validation setting. The default value is WHEN_SUPPORTED.
     */
    public static final String RESPONSE_CHECKSUM_VALIDATION = "response_checksum_validation";

    public static final String SDK_UA_APP_ID = "sdk_ua_app_id";

    /**
     * Property name for specifying the SIGV4A signing region set configuration.
     * This optional property is a non-empty, comma-delimited list of non-empty strings, which can be configured
     * via the environment variable {@code AWS_SIGV4A_SIGNING_REGION_SET},
     * or the configuration file property {@code sigv4a_signing_region_set}, following standard precedence rules.
     */
    public static final String SIGV4A_SIGNING_REGION_SET = "sigv4a_signing_region_set";

    private ProfileProperty() {
    }
}
