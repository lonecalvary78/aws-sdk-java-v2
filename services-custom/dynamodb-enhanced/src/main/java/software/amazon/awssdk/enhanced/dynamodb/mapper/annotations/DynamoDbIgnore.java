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

package software.amazon.awssdk.enhanced.dynamodb.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Opts this attribute out of participating in the table schema. It will be completely ignored by the mapper.
 * <p>
 * Example using {@link DynamoDbIgnore}:
 *
 * {@snippet :
 * @DynamoDbBean
 * public class Bean {
 *      private String internalKey;
 *
 *      @DynamoDbIgnore
 *      public String getInternalKey() {
 *          return this.internalKey;
 *      }
 *
 *      public void setInternalKey(String internalKey) {
 *          this.internalKey = internalKey;
 *      }
 * }
 * }
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@SdkPublicApi
public @interface DynamoDbIgnore {
}
