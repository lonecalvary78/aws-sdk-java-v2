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

package software.amazon.awssdk.services.waiters;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.restjsonwithwaiters.jmespath.internal.JmesPathRuntime;
import software.amazon.awssdk.services.restjsonwithwaiters.model.AllTypesResponse;

public class WaitersPathMatcherTest {
    @Test
    void testNumericValueComparison() {
        AllTypesResponse response = AllTypesResponse.builder()
                                                    .doubleMember(42.5)
                                                    .build();

        JmesPathRuntime.Value input = new JmesPathRuntime.Value(response);
        assertThat(input.field("DoubleMember").compare("==", input.constant(new BigDecimal("42.5"))).value())
            .isEqualTo(true);

        response = AllTypesResponse.builder()
                                   .doubleMember(42.4)
                                   .build();

        input = new JmesPathRuntime.Value(response);
        assertThat(input.field("DoubleMember").compare("<", input.constant(new BigDecimal("42.5"))).value())
            .isEqualTo(true);

        response = AllTypesResponse.builder()
                                   .bigDecimalMember(new BigDecimal("123.456789012345678901"))
                                   .build();

        input = new JmesPathRuntime.Value(response);
        assertThat(input.field("BigDecimalMember").compare(">", input.constant(new BigDecimal("123.45678901234567890"))).value())
            .isEqualTo(true);
    }


}
