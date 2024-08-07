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

package software.amazon.awssdk.protocols.json.internal.unmarshall;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.json.ErrorCodeParser;
import software.amazon.awssdk.protocols.json.JsonContent;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public class JsonErrorCodeParser implements ErrorCodeParser {

    /**
     * Services using AWS JSON 1.1 protocol with HTTP binding send the error code information in the
     * response headers, instead of the content. Package private for tests.
     */
    public static final String X_AMZN_ERROR_TYPE = "x-amzn-ErrorType";

    static final String ERROR_CODE_HEADER = ":error-code";

    static final String EXCEPTION_TYPE_HEADER = ":exception-type";

    /**
     * List of header keys that represent the error code sent by service.
     * Response should only contain one of these headers
     */
    private final List<String> errorCodeHeaders;
    private final String errorCodeFieldName;

    public JsonErrorCodeParser(String errorCodeFieldName) {
        this.errorCodeFieldName = errorCodeFieldName == null ? "__type" : errorCodeFieldName;
        this.errorCodeHeaders = Arrays.asList(X_AMZN_ERROR_TYPE, ERROR_CODE_HEADER, EXCEPTION_TYPE_HEADER);
    }

    /**
     * Parse the error code from the response.
     *
     * @return Error Code of exceptional response or null if it can't be determined
     */
    @Override
    public String parseErrorCode(SdkHttpFullResponse response, JsonContent jsonContent) {
        String errorCodeFromHeader = parseErrorCodeFromHeader(response);
        if (errorCodeFromHeader != null) {
            return errorCodeFromHeader;
        } else if (jsonContent != null) {
            return parseErrorCodeFromContents(jsonContent.getJsonNode());
        } else {
            return null;
        }
    }

    /**
     * Attempt to parse the error code from the response headers. Returns null if information is not
     * present in the header.
     */
    private String parseErrorCodeFromHeader(SdkHttpFullResponse response) {
        for (String errorCodeHeader : errorCodeHeaders) {
            Optional<String> errorCode = response.firstMatchingHeader(errorCodeHeader);

            if (errorCode.isPresent()) {
                if (X_AMZN_ERROR_TYPE.equals(errorCodeHeader)) {
                    return parseErrorCodeFromXAmzErrorType(errorCode.get());
                }

                return errorCode.get();
            }
        }

        return null;
    }

    private String parseErrorCodeFromXAmzErrorType(String headerValue) {
        if (headerValue != null) {
            int separator = headerValue.indexOf(':');
            if (separator != -1) {
                headerValue = headerValue.substring(0, separator);
            }
        }
        return headerValue;
    }

    /**
     * Attempt to parse the error code from the response content. Returns null if information is not
     * present in the content. Codes are expected to be in the form <b>"typeName"</b> or
     * <b>"prefix#typeName"</b> Examples : "AccessDeniedException",
     * "software.amazon.awssdk.dynamodb.v20111205#ProvisionedThroughputExceededException"
     */
    private String parseErrorCodeFromContents(JsonNode jsonContents) {
        if (jsonContents == null) {
            return null;
        }
        JsonNode errorCodeField = jsonContents.field(errorCodeFieldName).orElse(null);
        if (errorCodeField == null) {
            return null;
        }
        String code = errorCodeField.text();
        int separator = code.lastIndexOf('#');
        return code.substring(separator + 1);
    }
}
