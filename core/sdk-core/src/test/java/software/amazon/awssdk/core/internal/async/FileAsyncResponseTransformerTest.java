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

package software.amazon.awssdk.core.internal.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.core.FileTransformerConfiguration.FailureBehavior.DELETE;
import static software.amazon.awssdk.core.FileTransformerConfiguration.FailureBehavior.LEAVE;

import com.google.common.jimfs.Jimfs;
import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.FileTransformerConfiguration;
import software.amazon.awssdk.core.FileTransformerConfiguration.FileWriteOption;
import software.amazon.awssdk.core.FileTransformerConfiguration.FailureBehavior;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.internal.util.NoopSubscription;

/**
 * Tests for {@link FileAsyncResponseTransformer}.
 */
class FileAsyncResponseTransformerTest {
    private FileSystem testFs;

    @BeforeEach
    public void setup() {
        testFs = Jimfs.newFileSystem();
    }

    @AfterEach
    public void teardown() throws IOException {
        testFs.close();
    }

    @Test
    public void errorInStream_completesFuture() {
        Path testPath = testFs.getPath("test_file.txt");
        FileAsyncResponseTransformer xformer = new FileAsyncResponseTransformer(testPath);

        CompletableFuture prepareFuture = xformer.prepare();

        xformer.onResponse(new Object());
        xformer.onStream(subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long l) {
                }

                @Override
                public void cancel() {
                }
            });

            subscriber.onError(new RuntimeException("Something went wrong"));
        });

        assertThat(prepareFuture.isCompletedExceptionally()).isTrue();
    }

    @Test
    public void synchronousPublisher_shouldNotHang() throws Exception {
        List<CompletableFuture> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Path testPath = testFs.getPath(i + "test_file.txt");
            FileAsyncResponseTransformer transformer = new FileAsyncResponseTransformer(testPath);

            CompletableFuture prepareFuture = transformer.prepare();

            transformer.onResponse(new Object());

            transformer.onStream(testPublisher(RandomStringUtils.randomAlphanumeric(30000)));
            futures.add(prepareFuture);
        }

        CompletableFuture<Void> future = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        future.get(10, TimeUnit.SECONDS);
        assertThat(future.isCompletedExceptionally()).isFalse();
    }

    @Test
    void noConfiguration_fileAlreadyExists_shouldThrowException() throws Exception {
        Path testPath = testFs.getPath("test_file.txt");
        Files.write(testPath, RandomStringUtils.random(1000).getBytes(StandardCharsets.UTF_8));
        assertThat(testPath).exists();

        String content = RandomStringUtils.randomAlphanumeric(30000);
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(testPath);

        CompletableFuture<String> future = transformer.prepare();
        transformer.onResponse("foobar");
        transformer.onStream(testPublisher(content));
        assertThatThrownBy(() -> future.join()).hasRootCauseInstanceOf(FileAlreadyExistsException.class);
    }

    @Test
    void createOrReplaceExisting_fileDoesNotExist_shouldCreateNewFile() throws Exception {
        Path testPath = testFs.getPath("test_file.txt");
        assertThat(testPath).doesNotExist();
        String newContent = RandomStringUtils.randomAlphanumeric(2000);
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(testPath,
                                                                                              FileTransformerConfiguration.defaultCreateOrReplaceExisting());

        stubSuccessfulStreaming(newContent, transformer);
        assertThat(testPath).hasContent(newContent);
    }

    @Test
    void createOrReplaceExisting_fileAlreadyExists_shouldReplaceExisting() throws Exception {
        Path testPath = testFs.getPath("test_file.txt");

        int existingBytesLength = 20;
        String existingContent = RandomStringUtils.randomAlphanumeric(existingBytesLength);
        byte[] existingContentBytes = existingContent.getBytes(StandardCharsets.UTF_8);
        Files.write(testPath, existingContentBytes);

        int newBytesLength = 10;
        String newContent = RandomStringUtils.randomAlphanumeric(newBytesLength);
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(testPath,
                                                                                              FileTransformerConfiguration.defaultCreateOrReplaceExisting());
        stubSuccessfulStreaming(newContent, transformer);
        assertThat(testPath).hasContent(newContent);
    }

    @Test
    void createOrAppendExisting_fileDoesNotExist_shouldCreateNewFile() throws Exception {
        Path testPath = testFs.getPath("test_file.txt");
        assertThat(testPath).doesNotExist();
        String newContent = RandomStringUtils.randomAlphanumeric(500);
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(testPath,
                                                                                              FileTransformerConfiguration.defaultCreateOrAppend());
        stubSuccessfulStreaming(newContent, transformer);
        assertThat(testPath).hasContent(newContent);
    }

    @Test
    void createOrAppendExisting_fileExists_shouldAppend() throws Exception {
        Path testPath = testFs.getPath("test_file.txt");
        String existingString = RandomStringUtils.randomAlphanumeric(10);
        byte[] existingBytes = existingString.getBytes(StandardCharsets.UTF_8);
        Files.write(testPath, existingBytes);
        String content = RandomStringUtils.randomAlphanumeric(20);
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(testPath,
                                                                                              FileTransformerConfiguration.defaultCreateOrAppend());
        stubSuccessfulStreaming(content, transformer);
        assertThat(testPath).hasContent(existingString + content);
    }

    @ParameterizedTest
    @MethodSource("configurations")
    void exceptionOccurred_deleteFileBehavior(FileTransformerConfiguration configuration) throws Exception {
        Path testPath = testFs.getPath("test_file.txt");
        if (configuration.fileWriteOption() == FileWriteOption.WRITE_TO_POSITION) {
            // file must exist for WRITE_TO_POSITION
            Files.write(testPath, "foobar".getBytes(StandardCharsets.UTF_8));
        }
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(testPath, configuration);
        stubException(transformer);
        if (configuration.failureBehavior() == LEAVE) {
            assertThat(testPath).exists();
        } else {
            assertThat(testPath).doesNotExist();
        }
    }

    private static List<FileTransformerConfiguration> configurations() {
        List<FileTransformerConfiguration> conf = new ArrayList<>();
        conf.add(FileTransformerConfiguration.defaultCreateNew());
        conf.add(FileTransformerConfiguration.defaultCreateOrAppend());
        conf.add(FileTransformerConfiguration.defaultCreateOrReplaceExisting());
        for (FailureBehavior failureBehavior : FailureBehavior.values()) {
            for (FileWriteOption fileWriteOption : FileWriteOption.values()) {
                conf.add(FileTransformerConfiguration.builder()
                                                     .fileWriteOption(fileWriteOption)
                                                     .failureBehavior(failureBehavior)
                                                     .build());
            }
        }
        return conf;
    }

    @Test
    void explicitExecutor_shouldUseExecutor() throws Exception {
        Path testPath = testFs.getPath("test_file.txt");
        assertThat(testPath).doesNotExist();
        String newContent = RandomStringUtils.randomAlphanumeric(2000);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            SpyingExecutorService spyingExecutorService = new SpyingExecutorService(executor);
            FileTransformerConfiguration configuration = FileTransformerConfiguration
                .builder()
                .fileWriteOption(FileWriteOption.CREATE_NEW)
                .failureBehavior(DELETE)
                .executorService(spyingExecutorService)
                .build();
            FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(testPath, configuration);

            stubSuccessfulStreaming(newContent, transformer);
            assertThat(testPath).hasContent(newContent);
            assertThat(spyingExecutorService.hasReceivedTasks()).isTrue();
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(1, TimeUnit.MINUTES)).isTrue();
        }
    }

    @Test
    void writeToPosition_fileExists_shouldAppendFromPosition() throws Exception {
        int totalSize = 100;
        long prefixSize = 80L;
        int newContentLength = 20;

        Path testPath = testFs.getPath("test_file.txt");
        String contentBeforeRewrite = RandomStringUtils.randomAlphanumeric(totalSize);
        byte[] existingBytes = contentBeforeRewrite.getBytes(StandardCharsets.UTF_8);
        Files.write(testPath, existingBytes);
        String newContent = RandomStringUtils.randomAlphanumeric(newContentLength);
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(
            testPath,
            FileTransformerConfiguration.builder()
                                        .position(prefixSize)
                                        .failureBehavior(DELETE)
                                        .fileWriteOption(FileWriteOption.WRITE_TO_POSITION)
                                        .build());

        stubSuccessfulStreaming(newContent, transformer);

        String expectedContent = contentBeforeRewrite.substring(0, 80) + newContent;
        assertThat(testPath).hasContent(expectedContent);
    }

    @Test
    void writeToPosition_fileDoesNotExists_shouldThrowException() throws Exception {
        Path path = testFs.getPath("this/file/does/not/exists");
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(
            path,
            FileTransformerConfiguration.builder()
                                        .position(0L)
                                        .failureBehavior(DELETE)
                                        .fileWriteOption(FileWriteOption.WRITE_TO_POSITION)
                                        .build());
        CompletableFuture<?> future = transformer.prepare();
        transformer.onResponse("foobar");
        assertThatThrownBy(() -> {
            transformer.onStream(testPublisher("foo-bar-content"));
            future.get(10, TimeUnit.SECONDS);
        }).hasRootCauseInstanceOf(NoSuchFileException.class);
    }

    @Test
    void writeToPosition_fileExists_positionNotDefined_shouldRewriteFromStart() throws Exception {
        int totalSize = 100;
        Path testPath = testFs.getPath("test_file.txt");
        String contentBeforeRewrite = RandomStringUtils.randomAlphanumeric(totalSize);
        byte[] existingBytes = contentBeforeRewrite.getBytes(StandardCharsets.UTF_8);
        Files.write(testPath, existingBytes);
        String newContent = RandomStringUtils.randomAlphanumeric(totalSize);
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(
            testPath,
            FileTransformerConfiguration.builder()
                                        .failureBehavior(DELETE)
                                        .fileWriteOption(FileWriteOption.WRITE_TO_POSITION)
                                        .build());

        stubSuccessfulStreaming(newContent, transformer);

        assertThat(testPath).hasContent(newContent);

    }

    @Test
    void onStreamFailed_shouldCompleteFutureExceptionally() {
        Path testPath = testFs.getPath("test_file.txt");
        FileAsyncResponseTransformer<String> transformer = new FileAsyncResponseTransformer<>(testPath);
        CompletableFuture<String> future = transformer.prepare();
        transformer.onStream(null);
        assertThat(future).isCompletedExceptionally();
    }

    private static void stubSuccessfulStreaming(String newContent, FileAsyncResponseTransformer<String> transformer) throws Exception {
        CompletableFuture<String> future = transformer.prepare();
        transformer.onResponse("foobar");

        transformer.onStream(testPublisher(newContent));

        future.get(10, TimeUnit.SECONDS);
        assertThat(future.isCompletedExceptionally()).isFalse();
    }

    private static void stubException(FileAsyncResponseTransformer<String> transformer) throws Exception {
        CompletableFuture<String> future = transformer.prepare();
        transformer.onResponse("foobar");

        RuntimeException runtimeException = new RuntimeException("oops");
        transformer.onStream(s -> s.onSubscribe(new NoopSubscription(s)));
        transformer.exceptionOccurred(runtimeException);

        assertThat(future).failsWithin(1, TimeUnit.SECONDS)
                          .withThrowableOfType(ExecutionException.class)
                          .withCause(runtimeException);
    }

    private static SdkPublisher<ByteBuffer> testPublisher(String content) {
        return SdkPublisher.adapt(Flowable.just(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8))));
    }

    private static final class SpyingExecutorService implements ExecutorService {
        private final ExecutorService executorService;
        private boolean receivedTasks = false;

        private SpyingExecutorService(ExecutorService executorService) {
            this.executorService = executorService;
        }

        public boolean hasReceivedTasks() {
            return receivedTasks;
        }

        @Override
        public void shutdown() {
            executorService.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return executorService.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return executorService.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return executorService.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return executorService.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            receivedTasks = true;
            return executorService.submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            receivedTasks = true;
            return executorService.submit(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            receivedTasks = true;
            return executorService.submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            receivedTasks = true;
            return executorService.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            receivedTasks = true;
            return executorService.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            receivedTasks = true;
            return executorService.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            receivedTasks = true;
            return executorService.invokeAny(tasks, timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            receivedTasks = true;
            executorService.execute(command);
        }
    }
}