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

package software.amazon.awssdk.core.traits;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkField;

/**
 * Marker interface for traits that contain additional metadata about {@link SdkField}s.
 */
@SdkProtectedApi
public interface Trait {

    /**
     * The known trait type. This is correctly implemented and uniquely mapped to the enum values which allow us to create enum
     * maps for trait known trait types.
     */
    default TraitType type() {
        return null;
    }
}
