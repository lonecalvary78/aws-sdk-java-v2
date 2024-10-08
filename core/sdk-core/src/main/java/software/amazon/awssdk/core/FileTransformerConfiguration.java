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

package software.amazon.awssdk.core;

import java.nio.channels.AsynchronousChannelGroup;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration options for {@link AsyncResponseTransformer#toFile(Path, FileTransformerConfiguration)} to configure how the SDK
 * should write the file and if the SDK should delete the file when an exception occurs.
 *
 * @see #builder()
 * @see FileWriteOption
 * @see FailureBehavior
 */
@SdkPublicApi
public final class FileTransformerConfiguration implements ToCopyableBuilder<FileTransformerConfiguration.Builder,
    FileTransformerConfiguration> {
    private final FileWriteOption fileWriteOption;
    private final FailureBehavior failureBehavior;
    private final ExecutorService executorService;
    private final Long position;

    private FileTransformerConfiguration(DefaultBuilder builder) {
        this.fileWriteOption = Validate.paramNotNull(builder.fileWriteOption, "fileWriteOption");
        this.failureBehavior = Validate.paramNotNull(builder.failureBehavior, "failureBehavior");
        this.executorService = builder.executorService;
        this.position = Validate.isNotNegativeOrNull(builder.position, "position");
        if (fileWriteOption != FileWriteOption.WRITE_TO_POSITION && position != null) {
            throw new IllegalArgumentException(String.format(
                "'position' can only be used with 'WRITE_TO_POSITION' file write option, but was used with '%s'",
                fileWriteOption
            ));
        }
    }

    /**
     * The configured {@link FileWriteOption}
     */
    public FileWriteOption fileWriteOption() {
        return fileWriteOption;
    }

    /**
     * The configured {@link FailureBehavior}
     */
    public FailureBehavior failureBehavior() {
        return failureBehavior;
    }

    /**
     * The configured {@link ExecutorService} the writes should be executed on.
     * <p>
     * If not set, the default thread pool defined by the underlying {@link java.nio.file.spi.FileSystemProvider} will be used.
     * This will typically be the thread pool defined by the {@link AsynchronousChannelGroup}.
     */
    public Optional<ExecutorService> executorService() {
        return Optional.ofNullable(executorService);
    }

    /**
     * Exclusively used with {@link FileWriteOption#WRITE_TO_POSITION}. Configures the position, where to start writing to the
     * existing file. The location correspond to the first byte where new data will be written. For example, if {@code 128} is
     * configured, bytes 0-127 of the existing file will remain untouched and data will be written starting at byte 128. If not
     * specified, defaults to 0.
     *
     * @return The offset at which to start overwriting data in the file.
     */
    public Long position() {
        return position;
    }

    /**
     * Create a {@link Builder}, used to create a {@link FileTransformerConfiguration}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Returns the default {@link FileTransformerConfiguration} for {@link FileWriteOption#CREATE_NEW}
     * <p>
     * Always create a new file. If the file already exists, {@link FileAlreadyExistsException} will be thrown.
     * In the event of an error, the SDK will attempt to delete the file (whatever has been written to it so far).
     */
    public static FileTransformerConfiguration defaultCreateNew() {
        return builder().fileWriteOption(FileWriteOption.CREATE_NEW)
                        .failureBehavior(FailureBehavior.DELETE)
                        .build();
    }

    /**
     * Returns the default {@link FileTransformerConfiguration} for {@link FileWriteOption#CREATE_OR_REPLACE_EXISTING}
     * <p>
     * Create a new file if it doesn't exist, otherwise replace the existing file.
     * In the event of an error, the SDK will NOT attempt to delete the file, leaving it as-is
     */
    public static FileTransformerConfiguration defaultCreateOrReplaceExisting() {
        return builder().fileWriteOption(FileWriteOption.CREATE_OR_REPLACE_EXISTING)
                        .failureBehavior(FailureBehavior.LEAVE)
                        .build();
    }

    /**
     * Returns the default {@link FileTransformerConfiguration} for {@link FileWriteOption#CREATE_OR_APPEND_TO_EXISTING}
     * <p>
     * Create a new file if it doesn't exist, otherwise append to the existing file.
     * In the event of an error, the SDK will NOT attempt to delete the file, leaving it as-is
     */
    public static FileTransformerConfiguration defaultCreateOrAppend() {
        return builder().fileWriteOption(FileWriteOption.CREATE_OR_APPEND_TO_EXISTING)
                        .failureBehavior(FailureBehavior.LEAVE)
                        .build();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileTransformerConfiguration that = (FileTransformerConfiguration) o;

        if (fileWriteOption != that.fileWriteOption) {
            return false;
        }
        if (failureBehavior != that.failureBehavior) {
            return false;
        }
        if (!Objects.equals(position, that.position)) {
            return false;
        }
        return Objects.equals(executorService, that.executorService);
    }

    @Override
    public int hashCode() {
        int result = fileWriteOption != null ? fileWriteOption.hashCode() : 0;
        result = 31 * result + (failureBehavior != null ? failureBehavior.hashCode() : 0);
        result = 31 * result + (executorService != null ? executorService.hashCode() : 0);
        result = 31 * result + (position != null ? position.hashCode() : 0);
        return result;
    }

    /**
     * Defines how the SDK should write the file
     */
    public enum FileWriteOption {
        /**
         * Always create a new file. If the file already exists, {@link FileAlreadyExistsException} will be thrown.
         */
        CREATE_NEW,

        /**
         * Create a new file if it doesn't exist, otherwise replace the existing file.
         */
        CREATE_OR_REPLACE_EXISTING,

        /**
         * Create a new file if it doesn't exist, otherwise append to the existing file.
         */
        CREATE_OR_APPEND_TO_EXISTING,

        /**
         * Write to an existing file at the specified position, defined by the {@link FileTransformerConfiguration#position()}. If
         * the file does not exist, a {@link java.nio.file.NoSuchFileException} will be thrown. If
         * {@link FileTransformerConfiguration#position()} is not configured, start overwriting data at the beginning of the file
         * (byte 0).
         */
        WRITE_TO_POSITION
    }

    /**
     * Defines how the SDK should handle the file if there is an exception
     */
    public enum FailureBehavior {
        /**
         * In the event of an error, the SDK will attempt to delete the file and whatever has been written to it so far.
         */
        DELETE,

        /**
         * In the event of an error, the SDK will NOT attempt to delete the file and leave the file as-is (with whatever has been
         * written to it so far)
         */
        LEAVE
    }

    public interface Builder extends CopyableBuilder<Builder, FileTransformerConfiguration> {

        /**
         * Configures how to write the file
         *
         * @param fileWriteOption the file write option
         * @return This object for method chaining.
         */
        Builder fileWriteOption(FileWriteOption fileWriteOption);

        /**
         * Configures the {@link FailureBehavior} in the event of an error
         *
         * @param failureBehavior the failure behavior
         * @return This object for method chaining.
         */
        Builder failureBehavior(FailureBehavior failureBehavior);

        /**
         * Configures the {@link ExecutorService} the writes should be executed on.
         *
         * @param executorService the executor service to use, or null if using the default thread pool.
         * @return This object for method chaining.
         */
        Builder executorService(ExecutorService executorService);

        /**
         * Exclusively used with {@link FileWriteOption#WRITE_TO_POSITION}. Configures the position, where to start writing to the
         * existing file. The location correspond to the first byte where new data will be written. For example, if {@code 128} is
         * configured, bytes 0-127 of the existing file will remain untouched and data will be written starting at byte 128. If
         * not specified, defaults to 0.
         *
         * @param writePosition the position at where to start writing data to the file.
         * @return This object for method chaining.
         */
        Builder position(Long writePosition);
    }

    private static final class DefaultBuilder implements Builder {
        private FileWriteOption fileWriteOption;
        private FailureBehavior failureBehavior;
        private ExecutorService executorService;
        private Long position;

        private DefaultBuilder() {
        }

        private DefaultBuilder(FileTransformerConfiguration fileTransformerConfiguration) {
            this.fileWriteOption = fileTransformerConfiguration.fileWriteOption;
            this.failureBehavior = fileTransformerConfiguration.failureBehavior;
            this.executorService = fileTransformerConfiguration.executorService;
            this.position = fileTransformerConfiguration.position;
        }

        @Override
        public Builder fileWriteOption(FileWriteOption fileWriteOption) {
            this.fileWriteOption = fileWriteOption;
            return this;
        }

        @Override
        public Builder failureBehavior(FailureBehavior failureBehavior) {
            this.failureBehavior = failureBehavior;
            return this;
        }

        @Override
        public Builder executorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        @Override
        public Builder position(Long position) {
            this.position = position;
            return this;
        }

        @Override
        public FileTransformerConfiguration build() {
            return new FileTransformerConfiguration(this);
        }
    }

    @Override
    public String toString() {
        return ToString.builder("FileTransformerConfiguration")
                       .add("fileWriteOption", this.fileWriteOption)
                       .add("failureBehavior", this.failureBehavior)
                       .add("executorService", this.executorService)
                       .add("position", this.position)
                       .build();
    }
}
