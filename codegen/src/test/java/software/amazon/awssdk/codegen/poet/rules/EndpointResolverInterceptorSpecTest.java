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

package software.amazon.awssdk.codegen.poet.rules;

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

public class EndpointResolverInterceptorSpecTest {
    @Test
    public void endpointResolverInterceptorClass() {
        ClassSpec endpointProviderInterceptor = new EndpointResolverInterceptorSpec(getModel(true));
        assertThat(endpointProviderInterceptor, generatesTo("endpoint-resolve-interceptor.java"));
    }

    // TODO(post-sra-identity-auth): This can be deleted when useSraAuth is removed
    @Test
    public void endpointResolverInterceptorClass_preSra() {
        ClassSpec endpointProviderInterceptor = new EndpointResolverInterceptorSpec(getModel(false));
        assertThat(endpointProviderInterceptor, generatesTo("endpoint-resolve-interceptor-preSra.java"));
    }

    private static IntermediateModel getModel(boolean useSraAuth) {
        IntermediateModel model = ClientTestModels.queryServiceModels();
        model.getCustomizationConfig().setUseSraAuth(useSraAuth);
        return model;
    }

    @Test
    void endpointResolverInterceptorClassWithSigv4aMultiAuth() {
        ClassSpec endpointProviderInterceptor = new EndpointResolverInterceptorSpec(ClientTestModels.opsWithSigv4a());
        assertThat(endpointProviderInterceptor, generatesTo("endpoint-resolve-interceptor-with-multiauthsigv4a.java"));
    }

    @Test
    void endpointResolverInterceptorClassWithEndpointBasedAuth() {
        ClassSpec endpointProviderInterceptor = new EndpointResolverInterceptorSpec(ClientTestModels.queryServiceModelsEndpointAuthParamsWithoutAllowList());
        assertThat(endpointProviderInterceptor, generatesTo("endpoint-resolve-interceptor-with-endpointsbasedauth.java"));
    }
}
