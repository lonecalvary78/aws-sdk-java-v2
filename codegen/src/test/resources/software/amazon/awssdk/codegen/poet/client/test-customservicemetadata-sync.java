package software.amazon.awssdk.services.protocolrestjsonwithcustomcontenttype;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.protocolrestjsonwithcustomcontenttype.internal.ProtocolRestJsonWithCustomContentTypeServiceClientConfigurationBuilder;
import software.amazon.awssdk.services.protocolrestjsonwithcustomcontenttype.internal.ServiceVersionInfo;
import software.amazon.awssdk.services.protocolrestjsonwithcustomcontenttype.model.OneOperationRequest;
import software.amazon.awssdk.services.protocolrestjsonwithcustomcontenttype.model.OneOperationResponse;
import software.amazon.awssdk.services.protocolrestjsonwithcustomcontenttype.model.ProtocolRestJsonWithCustomContentTypeException;
import software.amazon.awssdk.services.protocolrestjsonwithcustomcontenttype.transform.OneOperationRequestMarshaller;
import software.amazon.awssdk.utils.Logger;

/**
 * Internal implementation of {@link ProtocolRestJsonWithCustomContentTypeClient}.
 *
 * @see ProtocolRestJsonWithCustomContentTypeClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultProtocolRestJsonWithCustomContentTypeClient implements ProtocolRestJsonWithCustomContentTypeClient {
    private static final Logger log = Logger.loggerFor(DefaultProtocolRestJsonWithCustomContentTypeClient.class);

    private static final AwsProtocolMetadata protocolMetadata = AwsProtocolMetadata.builder()
                                                                                   .serviceProtocol(AwsServiceProtocol.REST_JSON).build();

    private final SyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    protected DefaultProtocolRestJsonWithCustomContentTypeClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsSyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration
            .toBuilder()
            .option(SdkClientOption.SDK_CLIENT, this)
            .option(SdkClientOption.API_METADATA,
                    "AmazonProtocolRestJsonWithCustomContentType" + "#" + ServiceVersionInfo.VERSION).build();
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
    }

    /**
     * Invokes the OneOperation operation.
     *
     * @param oneOperationRequest
     * @return Result of the OneOperation operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws ProtocolRestJsonWithCustomContentTypeException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample ProtocolRestJsonWithCustomContentTypeClient.OneOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OneOperation" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public OneOperationResponse oneOperation(OneOperationRequest oneOperationRequest) throws AwsServiceException,
                                                                                             SdkClientException, ProtocolRestJsonWithCustomContentTypeException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<OneOperationResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                          OneOperationResponse::builder);
        Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
            if (errorCode == null) {
                return Optional.empty();
            }
            switch (errorCode) {
                default:
                    return Optional.empty();
            }
        };
        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata, exceptionMetadataMapper);
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(oneOperationRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, oneOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "AmazonProtocolRestJsonWithCustomContentType");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OneOperation");

            return clientHandler.execute(new ClientExecutionParams<OneOperationRequest, OneOperationResponse>()
                                             .withOperationName("OneOperation").withProtocolMetadata(protocolMetadata)
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .withRequestConfiguration(clientConfiguration).withInput(oneOperationRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new OneOperationRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
    }

    private static List<MetricPublisher> resolveMetricPublishers(SdkClientConfiguration clientConfiguration,
                                                                 RequestOverrideConfiguration requestOverrideConfiguration) {
        List<MetricPublisher> publishers = null;
        if (requestOverrideConfiguration != null) {
            publishers = requestOverrideConfiguration.metricPublishers();
        }
        if (publishers == null || publishers.isEmpty()) {
            publishers = clientConfiguration.option(SdkClientOption.METRIC_PUBLISHERS);
        }
        if (publishers == null) {
            publishers = Collections.emptyList();
        }
        return publishers;
    }

    private HttpResponseHandler<AwsServiceException> createErrorResponseHandler(BaseAwsJsonProtocolFactory protocolFactory,
                                                                                JsonOperationMetadata operationMetadata, Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper) {
        return protocolFactory.createErrorResponseHandler(operationMetadata, exceptionMetadataMapper);
    }

    private void updateRetryStrategyClientConfiguration(SdkClientConfiguration.Builder configuration) {
        ClientOverrideConfiguration.Builder builder = configuration.asOverrideConfigurationBuilder();
        RetryMode retryMode = builder.retryMode();
        if (retryMode != null) {
            configuration.option(SdkClientOption.RETRY_STRATEGY, AwsRetryStrategy.forRetryMode(retryMode));
        } else {
            Consumer<RetryStrategy.Builder<?, ?>> configurator = builder.retryStrategyConfigurator();
            if (configurator != null) {
                RetryStrategy.Builder<?, ?> defaultBuilder = AwsRetryStrategy.defaultRetryStrategy().toBuilder();
                configurator.accept(defaultBuilder);
                configuration.option(SdkClientOption.RETRY_STRATEGY, defaultBuilder.build());
            } else {
                RetryStrategy retryStrategy = builder.retryStrategy();
                if (retryStrategy != null) {
                    configuration.option(SdkClientOption.RETRY_STRATEGY, retryStrategy);
                }
            }
        }
        configuration.option(SdkClientOption.CONFIGURED_RETRY_MODE, null);
        configuration.option(SdkClientOption.CONFIGURED_RETRY_STRATEGY, null);
        configuration.option(SdkClientOption.CONFIGURED_RETRY_CONFIGURATOR, null);
    }

    private SdkClientConfiguration updateSdkClientConfiguration(SdkRequest request, SdkClientConfiguration clientConfiguration) {
        List<SdkPlugin> plugins = request.overrideConfiguration().map(c -> c.plugins()).orElse(Collections.emptyList());
        if (plugins.isEmpty()) {
            return clientConfiguration;
        }
        SdkClientConfiguration.Builder configuration = clientConfiguration.toBuilder();
        ProtocolRestJsonWithCustomContentTypeServiceClientConfigurationBuilder serviceConfigBuilder = new ProtocolRestJsonWithCustomContentTypeServiceClientConfigurationBuilder(
            configuration);
        for (SdkPlugin plugin : plugins) {
            plugin.configureClient(serviceConfigBuilder);
        }
        updateRetryStrategyClientConfiguration(configuration);
        return configuration.build();
    }

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder.clientConfiguration(clientConfiguration)
                      .defaultServiceExceptionSupplier(ProtocolRestJsonWithCustomContentTypeException::builder)
                      .protocol(AwsJsonProtocol.REST_JSON).protocolVersion("1.1").contentType("application/json");
    }

    @Override
    public final ProtocolRestJsonWithCustomContentTypeServiceClientConfiguration serviceClientConfiguration() {
        return new ProtocolRestJsonWithCustomContentTypeServiceClientConfigurationBuilder(this.clientConfiguration.toBuilder())
            .build();
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}
