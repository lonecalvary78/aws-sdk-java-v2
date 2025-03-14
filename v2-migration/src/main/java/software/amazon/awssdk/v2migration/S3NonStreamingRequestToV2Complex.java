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

package software.amazon.awssdk.v2migration;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * These transforms are more complex (e.g., method chaining) and use JavaTemplate which replaces text directly instead of building
 * up the AST tree as in {@link S3NonStreamingRequestToV2}. Because of that, we need manually add imports as well as run this
 * recipe after the client transforms are completed, e.g. new AmazonS3() -> S3Client.builder().build()
 *
 */
@SdkInternalApi
public class S3NonStreamingRequestToV2Complex extends Recipe {

    private static final String V2_S3_PKG = "software.amazon.awssdk.services.s3";

    private static final MethodMatcher DISABLE_REQUESTER_PAYS = createMethodMatcher("disableRequesterPays(String)");
    private static final MethodMatcher ENABLE_REQUESTER_PAYS = createMethodMatcher("enableRequesterPays(String)");
    private static final MethodMatcher IS_REQUESTER_PAYS_ENABLED = createMethodMatcher("isRequesterPaysEnabled(String)");
    private static final MethodMatcher GET_OBJECT_AS_STRING = createMethodMatcher("getObjectAsString(String, String)");
    private static final MethodMatcher GET_URL = createMethodMatcher("getUrl(String, String)");
    private static final MethodMatcher LIST_BUCKETS = createMethodMatcher("listBuckets()");
    private static final MethodMatcher RESTORE_OBJECT = createMethodMatcher("restoreObject(String, String, int)");
    private static final MethodMatcher SET_OBJECT_REDIRECT_LOCATION =
        createMethodMatcher("objectRedirectLocation(String, String, String)");
    private static final MethodMatcher CHANGE_OBJECT_STORAGE_CLASS = createMethodMatcher(
        String.format("changeObjectStorageClass(String, String, %s.model.StorageClass)", V2_S3_PKG));
    private static final MethodMatcher CREATE_BUCKET = createMethodMatcher("createBucket(String, String)");

    private static MethodMatcher createMethodMatcher(String methodSignature) {
        return new MethodMatcher(V2_S3_PKG + ".S3Client " + methodSignature, true);
    }

    @Override
    public String getDisplayName() {
        return "V1 S3 non-streaming requests to V2";
    }

    @Override
    public String getDescription() {
        return "Transform usage of V1 S3 non-streaming requests to V2.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static final class Visitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {

            if (isCompleteMpuRequestMultipartUploadSetter(method)) {
                method = transformCompleteMpuRequestCompletedPartsArg(method);
                return super.visitMethodInvocation(method, executionContext);
            }

            if (DISABLE_REQUESTER_PAYS.matches(method, false)) {
                method = transformSetRequesterPays(method, false);
                return super.visitMethodInvocation(method, executionContext);
            }
            if (ENABLE_REQUESTER_PAYS.matches(method, false)) {
                method = transformSetRequesterPays(method, true);
                return super.visitMethodInvocation(method, executionContext);
            }
            if (IS_REQUESTER_PAYS_ENABLED.matches(method, false)) {
                method = transformIsRequesterPays(method);
                return super.visitMethodInvocation(method, executionContext);
            }
            if (GET_OBJECT_AS_STRING.matches(method, false)) {
                method = transformGetObjectAsString(method);
                return super.visitMethodInvocation(method, executionContext);
            }
            if (GET_URL.matches(method, false)) {
                method = transformGetUrl(method);
                return super.visitMethodInvocation(method, executionContext);
            }
            if (LIST_BUCKETS.matches(method, false)) {
                method = transformListBuckets(method);
                return super.visitMethodInvocation(method, executionContext);
            }
            if (RESTORE_OBJECT.matches(method, false)) {
                method = transformRestoreObject(method);
                return super.visitMethodInvocation(method, executionContext);
            }
            if (SET_OBJECT_REDIRECT_LOCATION.matches(method, false)) {
                method = transformSetObjectRedirectLocation(method);
                return super.visitMethodInvocation(method, executionContext);
            }
            if (CHANGE_OBJECT_STORAGE_CLASS.matches(method, false)) {
                method = transformChangeObjectStorageClass(method);
                return super.visitMethodInvocation(method, executionContext);
            }
            if (CREATE_BUCKET.matches(method, false)) {
                method = transformCreateBucket(method);
                return super.visitMethodInvocation(method, executionContext);
            }
            return super.visitMethodInvocation(method, executionContext);
        }

        private boolean isCompleteMpuRequestMultipartUploadSetter(J.MethodInvocation method) {
            JavaType.FullyQualified completeMpuRequest = TypeUtils.asFullyQualified(JavaType.buildType(
                "software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest.Builder"));
            return "multipartUpload".equals(method.getSimpleName()) &&
                   TypeUtils.isAssignableTo(completeMpuRequest, method.getSelect().getType());
        }

        private J.MethodInvocation transformCompleteMpuRequestCompletedPartsArg(J.MethodInvocation method) {
            addImport("CompletedMultipartUpload");
            String v2Method = "CompletedMultipartUpload.builder().parts(#{any()}).build()";

            return JavaTemplate.builder(v2Method).build()
                               .apply(getCursor(), method.getCoordinates().replaceArguments(),
                                      method.getArguments().get(0));
        }

        private J.MethodInvocation transformCreateBucket(J.MethodInvocation method) {
            String v2Method = "#{any()}.createBucket(CreateBucketRequest.builder().bucket(#{any()})"
                              + ".createBucketConfiguration(CreateBucketConfiguration.builder().locationConstraint(#{any()})"
                              + ".build()).build())";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1));

            addImport("CreateBucketRequest");
            addImport("CreateBucketConfiguration");
            return method;
        }

        private J.MethodInvocation transformChangeObjectStorageClass(J.MethodInvocation method) {
            String v2Method = "#{any()}.copyObject(CopyObjectRequest.builder().sourceBucket(#{any()}).sourceKey(#{any()})"
                              + ".destinationBucket(#{any()}).destinationKey(#{any()})"
                              + ".storageClass(#{any()}).build())";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1),
                                        method.getArguments().get(0), method.getArguments().get(1),
                                        method.getArguments().get(2));

            addImport("CopyObjectRequest");
            return method;
        }

        private J.MethodInvocation transformSetObjectRedirectLocation(J.MethodInvocation method) {
            String v2Method = "#{any()}.copyObject(CopyObjectRequest.builder().sourceBucket(#{any()}).sourceKey(#{any()})"
                              + ".destinationBucket(#{any()}).destinationKey(#{any()})"
                              + ".websiteRedirectLocation(#{any()}).build())";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1),
                                        method.getArguments().get(0), method.getArguments().get(1),
                                        method.getArguments().get(2));

            addImport("CopyObjectRequest");
            return method;
        }

        private J.MethodInvocation transformRestoreObject(J.MethodInvocation method) {
            String v2Method = "#{any()}.restoreObject(RestoreObjectRequest.builder().bucket(#{any()}).key(#{any()})"
                              + ".restoreRequest(RestoreRequest.builder().days(#{any()}).build()).build())";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1), method.getArguments().get(2));

            addImport("RestoreObjectRequest");
            addImport("RestoreRequest");
            return method;
        }

        private J.MethodInvocation transformListBuckets(J.MethodInvocation method) {
            String v2Method = "#{any()}.listBuckets().buckets()";
            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect());

            return method;
        }

        private J.MethodInvocation transformGetObjectAsString(J.MethodInvocation method) {
            String v2Method = "#{any()}.getObjectAsBytes(GetObjectRequest.builder().bucket(#{any()}).key(#{any()})"
                              + ".build()).asUtf8String()";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1));

            addImport("GetObjectRequest");
            return method;
        }

        private J.MethodInvocation transformGetUrl(J.MethodInvocation method) {
            String v2Method = "#{any()}.utilities().getUrl(GetUrlRequest.builder().bucket(#{any()}).key(#{any()}).build())";
            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1));

            addImport("GetUrlRequest");
            return method;
        }

        private J.MethodInvocation transformIsRequesterPays(J.MethodInvocation method) {
            String v2Method = "#{any()}.getBucketRequestPayment(GetBucketRequestPaymentRequest.builder().bucket(#{any()})"
                              + ".build()).payer().toString().equals(\"Requester\")";
            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0));

            addImport("GetBucketRequestPaymentRequest");
            return method;
        }

        private J.MethodInvocation transformSetRequesterPays(J.MethodInvocation method, boolean enable) {
            String payer = enable ? "REQUESTER" : "BUCKET_OWNER";
            String v2Method = String.format("#{any()}.putBucketRequestPayment(PutBucketRequestPaymentRequest.builder()"
                                            + ".bucket(#{any()}).requestPaymentConfiguration("
                                            + "RequestPaymentConfiguration.builder().payer(Payer.%s).build())"
                                            + ".build())", payer);
            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0));

            addImport("PutBucketRequestPaymentRequest");
            addImport("RequestPaymentConfiguration");
            addImport("Payer");
            return method;
        }

        private void addImport(String pojoName) {
            String fqcn = "software.amazon.awssdk.services.s3.model." + pojoName;
            doAfterVisit(new AddImport<>(fqcn, null, false));
        }
    }
}
