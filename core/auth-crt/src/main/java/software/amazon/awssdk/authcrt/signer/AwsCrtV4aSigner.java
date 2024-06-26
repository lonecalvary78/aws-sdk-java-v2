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

package software.amazon.awssdk.authcrt.signer;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.authcrt.signer.internal.DefaultAwsCrtV4aSigner;
import software.amazon.awssdk.core.signer.Presigner;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.regions.RegionScope;

/**
 * Enables signing and presigning using Sigv4a (Asymmetric Sigv4) through an external API call to the AWS CRT
 *  (Common RunTime) library.
 *  <p/>
 * In CRT signing, payload signing is the default unless an override value is specified.
 *
 * @deprecated Use {@code software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner} from the 'http-auth-aws' module.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
@Deprecated
public interface AwsCrtV4aSigner extends Signer, Presigner {

    /**
     * Create a default Aws4aSigner.
     */
    static AwsCrtV4aSigner create() {
        return DefaultAwsCrtV4aSigner.create();
    }

    static Builder builder() {
        return DefaultAwsCrtV4aSigner.builder();
    }

    interface Builder {
        /**
         * The region scope that this signer will default to if not provided explicitly when the signer is invoked.
         *
         * @param defaultRegionScope The default region scope.
         * @return This builder for method chaining.
         */
        Builder defaultRegionScope(RegionScope defaultRegionScope);

        AwsCrtV4aSigner build();
    }
}
