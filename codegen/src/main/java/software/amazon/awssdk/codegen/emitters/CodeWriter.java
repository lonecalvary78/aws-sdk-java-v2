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

package software.amazon.awssdk.codegen.emitters;

import static software.amazon.awssdk.codegen.internal.Constant.JAVA_FILE_NAME_SUFFIX;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Formats the generated code and write it to the underlying file. The caller should call the flush
 * method to write the contents to the file. This class is intended to be used only by the code
 * generation system and is not to be used for public use.
 */
public class CodeWriter extends StringWriter {

    /**
     * The code transformation that should be applied before code is written.
     */
    private final CodeTransformer codeWriteTransformer;

    /**
     * The code transformation that should be applied before source code is "compared" for equality. This is only used when
     * attempting to clobber a file that has already been generated.
     */
    private final CodeTransformer codeComparisonTransformer = new LinkRemover();

    private final String dir;

    private final String file;

    /**
     * Constructor to use for .java files.
     * @param dir
     *             output directory where the file is to be created.
     * @param file
     *             name of the file without .java suffix.
     */
    public CodeWriter(String dir, String file) {
        this(dir, file, JAVA_FILE_NAME_SUFFIX, false);
    }

    public CodeWriter(String dir, String file, boolean disableFormatter) {
        this(dir, file, JAVA_FILE_NAME_SUFFIX, disableFormatter);
    }

    /**
     * Constructor to use for custom file suffixes.
     *
     * @param dir
     *              output directory where the file is to be created.
     * @param file
     *             name of the file excluding suffix.
     * @param fileNameSuffix
     *             suffix to be appended at the end of file name.
     */
    public CodeWriter(String dir, String file, String fileNameSuffix, boolean disableFormatter) {
        if (dir == null) {
            throw new IllegalArgumentException(
                    "Output Directory cannot be null.");
        }

        if (file == null) {
            throw new IllegalArgumentException("File name cannot be null.");
        }

        if (fileNameSuffix == null) {
            throw new IllegalArgumentException("File name suffix cannot be null.");
        }

        if (!file.endsWith(fileNameSuffix)) {
            file = file + fileNameSuffix;
        }

        this.dir = dir;
        this.file = file;
        Utils.createDirectory(dir);

        if (disableFormatter) {
            codeWriteTransformer = CodeTransformer.chain(new UnusedImportRemover());
        } else {
            codeWriteTransformer = CodeTransformer.chain(new UnusedImportRemover(),
                                                         new JavaCodeFormatter());
        }
    }

    /**
     * This method is expected to be called only once during the code generation process after the
     * template processing is done.
     */
    @Override
    public void flush() {
        try {
            Path outputFile = Paths.get(dir, this.file);
            String contents = getBuffer().toString();
            String formattedContents = codeWriteTransformer.apply(contents);

            if (fileSize(outputFile) == 0) {
                try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
                    writer.write(formattedContents);
                    writer.flush();
                }
            } else {
                validateFileContentMatches(outputFile, formattedContents);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private long fileSize(Path outputFile) throws IOException {
        try {
            return Files.size(outputFile);
        } catch (NoSuchFileException e) {
            return 0;
        }
    }

    private void validateFileContentMatches(Path outputFile, String newFileContents) throws IOException {
        byte[] currentFileBytes = Files.readAllBytes(outputFile);
        String currentFileContents = new String(currentFileBytes, StandardCharsets.UTF_8);

        String currentContentForComparison = codeComparisonTransformer.apply(currentFileContents);
        String newContentForComparison = codeComparisonTransformer.apply(newFileContents);

        if (!StringUtils.equals(currentContentForComparison, newContentForComparison)) {
            throw new IllegalStateException("Attempted to clobber existing file (" + outputFile + ") with a new file that has " +
                                            "different content. This may indicate forgetting to clean up old generated files " +
                                            "before running the generator?\n" +
                                            "Existing file: \n" + currentContentForComparison + "\n\n" +
                                            "New file: \n" + newContentForComparison);
        }
    }
}
