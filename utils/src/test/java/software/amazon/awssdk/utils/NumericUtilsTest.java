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

package software.amazon.awssdk.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.utils.NumericUtils.max;
import static software.amazon.awssdk.utils.NumericUtils.min;

import java.time.Duration;
import org.junit.jupiter.api.Test;

public class NumericUtilsTest {

    private final Duration SHORT_DURATION = Duration.ofMillis(10);
    private final Duration SHORT_SAME_DURATION = Duration.ofMillis(10);
    private final Duration LONG_DURATION = Duration.ofMillis(100);
    private final Duration NEGATIVE_SHORT_DURATION = Duration.ofMillis(-10);
    private final Duration NEGATIVE_SHORT_SAME_DURATION = Duration.ofMillis(-10);
    private final Duration NEGATIVE_LONG_DURATION = Duration.ofMillis(-100);

    @Test
    public void minTestDifferentDurations() {
        assertThat(min(SHORT_DURATION, LONG_DURATION)).isEqualTo(SHORT_DURATION);
    }

    @Test
    public void minTestDifferentDurationsReverse() {
        assertThat(min(LONG_DURATION, SHORT_DURATION)).isEqualTo(SHORT_DURATION);
    }

    @Test
    public void minTestSameDurations() {
        assertThat(min(SHORT_DURATION, SHORT_SAME_DURATION)).isEqualTo(SHORT_SAME_DURATION);
    }

    @Test
    public void minTestDifferentNegativeDurations() {
        assertThat(min(NEGATIVE_SHORT_DURATION, NEGATIVE_LONG_DURATION)).isEqualTo(NEGATIVE_LONG_DURATION);
    }

    @Test
    public void minTestNegativeSameDurations() {
        assertThat(min(NEGATIVE_SHORT_DURATION, NEGATIVE_SHORT_SAME_DURATION)).isEqualTo(NEGATIVE_SHORT_DURATION);
    }

    @Test
    public void maxTestDifferentDurations() {
        assertThat(max(LONG_DURATION, SHORT_DURATION)).isEqualTo(LONG_DURATION);
    }

    @Test
    public void maxTestDifferentDurationsReverse() {
        assertThat(max(SHORT_DURATION, LONG_DURATION)).isEqualTo(LONG_DURATION);
    }

    @Test
    public void maxTestSameDurations() {
        assertThat(max(SHORT_DURATION, SHORT_SAME_DURATION)).isEqualTo(SHORT_SAME_DURATION);
    }

    @Test
    public void maxTestDifferentNegativeDurations() {
        assertThat(max(NEGATIVE_SHORT_DURATION, NEGATIVE_LONG_DURATION)).isEqualTo(NEGATIVE_SHORT_DURATION);
    }

    @Test
    public void maxTestNegativeSameDurations() {
        assertThat(max(NEGATIVE_SHORT_DURATION, NEGATIVE_SHORT_SAME_DURATION)).isEqualTo(NEGATIVE_SHORT_DURATION);
    }
    
    @Test
    public void longToByte() {
        long input = 12345678l;
        byte[] bytes = NumericUtils.longToByte(input);
        assertThat(bytes).encodedAsBase64().isEqualTo(BinaryUtils.toBase64(bytes));
    }
}