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

package software.amazon.awssdk.codegen.model.intermediate;

import static software.amazon.awssdk.codegen.internal.Constant.LF;
import static software.amazon.awssdk.codegen.internal.Constant.REQUEST_CLASS_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Constant.RESPONSE_CLASS_SUFFIX;
import static software.amazon.awssdk.codegen.internal.DocumentationUtils.removeFromEnd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.model.intermediate.customization.ShapeCustomizationInfo;
import software.amazon.awssdk.codegen.model.service.XmlNamespace;
import software.amazon.awssdk.utils.StringUtils;

public class ShapeModel extends DocumentationModel implements HasDeprecation {

    private String c2jName;
    // shapeName might be later modified by the customization.
    private String shapeName;
    // the local variable name inside marshaller/unmarshaller implementation
    private boolean deprecated;
    private String deprecatedMessage;
    private String type;
    private List<String> required;
    private boolean hasPayloadMember;
    private boolean hasHeaderMember;
    private boolean hasStatusCodeMember;
    private boolean hasStreamingMember;
    private boolean hasRequiresLengthMember;
    private boolean wrapper;
    private boolean simpleMethod;
    private String requestSignerClassFqcn;
    private EndpointDiscovery endpointDiscovery;

    private List<MemberModel> members;
    private List<EnumModel> enums;

    private VariableModel variable;

    private ShapeMarshaller marshaller;
    private ShapeUnmarshaller unmarshaller;

    private String errorCode;
    private Integer httpStatusCode;
    private boolean fault;

    private ShapeCustomizationInfo customization = new ShapeCustomizationInfo();

    private boolean isEventStream;

    private boolean isEvent;

    private XmlNamespace xmlNamespace;

    private boolean document;

    private boolean union;

    private boolean retryable;
    private boolean throttling;

    public ShapeModel() {
    }

    public ShapeModel(String c2jName) {
        this.c2jName = c2jName;
    }

    public String getShapeName() {
        return shapeName;
    }

    public void setShapeName(String shapeName) {
        this.shapeName = shapeName;
    }

    @Override
    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getDeprecatedMessage() {
        return deprecatedMessage;
    }

    public void setDeprecatedMessage(String deprecatedMessage) {
        this.deprecatedMessage = deprecatedMessage;
    }

    public String getC2jName() {
        return c2jName;
    }

    public void setC2jName(String c2jName) {
        this.c2jName = c2jName;
    }

    public String getType() {
        return type;
    }



    @JsonIgnore
    public void setType(ShapeType shapeType) {
        setType(shapeType.getValue());
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonIgnore
    public ShapeType getShapeType() {
        return ShapeType.fromValue(type);
    }

    public ShapeModel withType(String type) {
        this.type = type;
        return this;
    }

    // Returns the list of C2j member names that are required for this shape.
    public List<String> getRequired() {
        return required;
    }

    public void setRequired(List<String> required) {
        this.required = required;
    }

    public boolean isHasPayloadMember() {
        return hasPayloadMember;
    }

    public void setHasPayloadMember(boolean hasPayloadMember) {
        this.hasPayloadMember = hasPayloadMember;
    }

    public ShapeModel withHasPayloadMember(boolean hasPayloadMember) {
        setHasPayloadMember(hasPayloadMember);
        return this;
    }

    /**
     * @return The member explicitly designated as the payload member
     */
    @JsonIgnore
    public MemberModel getPayloadMember() {
        MemberModel payloadMember = null;
        for (MemberModel member : members) {
            if (member.getHttp().getIsPayload()) {
                if (payloadMember == null) {
                    payloadMember = member;
                } else {
                    throw new IllegalStateException(
                            String.format("Only one payload member can be explicitly set on %s. This is likely an error in " +
                                          "the C2J model", c2jName));
                }
            }
        }
        return payloadMember;
    }

    /**
     * @return The list of members whose location is not specified. If no payload member is
     *         explicitly set then these members will appear in the payload
     */
    @JsonIgnore
    public List<MemberModel> getUnboundMembers() {
        List<MemberModel> unboundMembers = new ArrayList<>();
        if (members != null) {
            for (MemberModel member : members) {
                if (member.getHttp().getLocation() == null && !member.getHttp().getIsPayload()) {
                    if (hasPayloadMember) {
                        // There is an explicit payload, but this unbound
                        // member isn't it.
                        // Note: Somewhat unintuitive, explicit payloads don't
                        // have an explicit location; they're identified by
                        // the payload HTTP trait being true.
                        throw new IllegalStateException(String.format(
                                "C2J Shape %s has both an explicit payload member and unbound (no explicit location) member, %s."
                                + " This is undefined behavior, verify the correctness of the C2J model.",
                                c2jName, member.getName()));
                    }
                    unboundMembers.add(member);
                }
            }
        }
        return unboundMembers;
    }

    /**
     * @return The list of members whose are not marked with either eventheader or eventpayload trait.
     */
    @JsonIgnore
    public List<MemberModel> getUnboundEventMembers() {
        if (members == null) {
            return new ArrayList<>();
        }

        return members.stream()
                      .filter(m -> !m.isEventHeader())
                      .filter(m -> !m.isEventPayload())
                      .collect(Collectors.toList());
    }

    /**
     * @return True if the shape has an explicit payload member or implicit payload member(s).
     */
    public boolean hasPayloadMembers() {
        return hasPayloadMember ||
               getExplicitEventPayloadMember() != null ||
               hasImplicitPayloadMembers();

    }

    public boolean hasImplicitPayloadMembers() {
        return !getUnboundMembers().isEmpty() ||
               hasImplicitEventPayloadMembers();
    }

    public boolean hasImplicitEventPayloadMembers() {
        return isEvent() && !getUnboundEventMembers().isEmpty();
    }

    /**
     * Explicit event payload member will have "eventpayload" trait set to true.
     * There can be at most only one member that can be declared as explicit payload.
     *
     * @return the member that has the 'eventpayload' trait set to true. If none found, return null.
     */
    public MemberModel getExplicitEventPayloadMember() {
        if (members == null) {
            return null;
        }

        return members.stream()
                      .filter(MemberModel::isEventPayload)
                      .findFirst()
                      .orElse(null);
    }

    /**
     * If all members in shape have eventheader trait, then there is no payload
     */
    public boolean hasNoEventPayload() {
        return members == null || members.stream().allMatch(MemberModel::isEventHeader);
    }

    public boolean isHasStreamingMember() {
        return hasStreamingMember;
    }

    public void setHasStreamingMember(boolean hasStreamingMember) {
        this.hasStreamingMember = hasStreamingMember;
    }

    public ShapeModel withHasStreamingMember(boolean hasStreamingMember) {
        setHasStreamingMember(hasStreamingMember);
        return this;
    }

    public boolean isHasRequiresLengthMember() {
        return hasRequiresLengthMember;
    }

    public void setHasRequiresLengthMember(boolean hasRequiresLengthMember) {
        this.hasRequiresLengthMember = hasRequiresLengthMember;
    }

    public ShapeModel withHasRequiresLengthMember(boolean hasRequiresLengthMember) {
        setHasRequiresLengthMember(hasRequiresLengthMember);
        return this;
    }

    public boolean isHasHeaderMember() {
        return hasHeaderMember;
    }

    public void setHasHeaderMember(boolean hasHeaderMember) {
        this.hasHeaderMember = hasHeaderMember;
    }

    public ShapeModel withHasHeaderMember(boolean hasHeaderMember) {
        setHasHeaderMember(hasHeaderMember);
        return this;
    }

    public boolean isHasStatusCodeMember() {
        return hasStatusCodeMember;
    }

    public void setHasStatusCodeMember(boolean hasStatusCodeMember) {
        this.hasStatusCodeMember = hasStatusCodeMember;
    }

    public boolean isWrapper() {
        return wrapper;
    }

    public void setWrapper(boolean wrapper) {
        this.wrapper = wrapper;
    }

    public boolean isSimpleMethod() {
        return simpleMethod;
    }

    public void setSimpleMethod(boolean simpleMethod) {
        this.simpleMethod = simpleMethod;
    }

    public ShapeModel withHasStatusCodeMember(boolean hasStatusCodeMember) {
        setHasStatusCodeMember(hasStatusCodeMember);
        return this;
    }

    public MemberModel getMemberByVariableName(String memberVariableName) {
        for (MemberModel memberModel : members) {
            if (memberModel.getVariable().getVariableName().equals(memberVariableName)) {
                return memberModel;
            }
        }
        throw new IllegalArgumentException("Unknown member variable name: " + memberVariableName);
    }

    public MemberModel getMemberByName(String memberName) {
        for (MemberModel memberModel : members) {
            if (memberModel.getName().equals(memberName)) {
                return memberModel;
            }
        }
        return null;
    }

    public MemberModel getMemberByC2jName(String memberName) {
        for (MemberModel memberModel : members) {
            if (memberModel.getC2jName().equals(memberName)) {
                return memberModel;
            }
        }
        return null;
    }

    public List<MemberModel> getMembers() {
        if (members == null) {
            return Collections.emptyList();
        }
        return members;
    }

    /**
     * @return All non-streaming members of the shape.
     */
    public List<MemberModel> getNonStreamingMembers() {
        return getMembers().stream()
                           // Filter out binary streaming members
                           .filter(m -> !m.getHttp().getIsStreaming())
                           // Filter out event stream members (if shape is null then it's primitive and we should include it).
                           .filter(m -> m.getShape() == null || !m.getShape().isEventStream)
                           .collect(Collectors.toList());
    }

    public void setMembers(List<MemberModel> members) {
        this.members = members;
    }

    public void addMember(MemberModel member) {
        if (this.members == null) {
            this.members = new ArrayList<>();
        }
        members.add(member);
    }

    public List<EnumModel> getEnums() {
        return enums;
    }

    public void setEnums(List<EnumModel> enums) {
        this.enums = enums;
    }

    public void addEnum(EnumModel enumModel) {
        if (this.enums == null) {
            this.enums = new ArrayList<>();
        }
        this.enums.add(enumModel);
    }

    public VariableModel getVariable() {
        return variable;
    }

    public void setVariable(VariableModel variable) {
        this.variable = variable;
    }

    public ShapeMarshaller getMarshaller() {
        return marshaller;
    }

    public void setMarshaller(ShapeMarshaller marshaller) {
        this.marshaller = marshaller;
    }

    public ShapeUnmarshaller getUnmarshaller() {
        return unmarshaller;
    }

    public void setUnmarshaller(ShapeUnmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    public ShapeCustomizationInfo getCustomization() {
        return customization;
    }

    public void setCustomization(ShapeCustomizationInfo customization) {
        this.customization = customization;
    }

    public Map<String, MemberModel> getMembersAsMap() {
        Map<String, MemberModel> shapeMembers = new HashMap<>();

        // Creating a map of shape's members. This map is used below when
        // fetching the details of a member.
        List<MemberModel> memberModels = getMembers();
        if (memberModels != null) {
            for (MemberModel model : memberModels) {
                shapeMembers.put(model.getName(), model);
            }
        }
        return shapeMembers;
    }

    /**
     * Tries to find the member model associated with the given c2j member name from this shape
     * model. Returns the member model if present else returns null.
     */
    public MemberModel tryFindMemberModelByC2jName(String memberC2jName, boolean ignoreCase) {

        List<MemberModel> memberModels = getMembers();
        String expectedName = ignoreCase ? StringUtils.lowerCase(memberC2jName)
                                               : memberC2jName;

        if (memberModels != null) {
            for (MemberModel member : memberModels) {
                String actualName = ignoreCase ? StringUtils.lowerCase(member.getC2jName())
                                               : member.getC2jName();

                if (expectedName.equals(actualName)) {
                    return member;
                }
            }
        }
        return null;
    }

    /**
     * Returns the member model associated with the given c2j member name from this shape model.
     */
    public MemberModel findMemberModelByC2jName(String memberC2jName) {

        MemberModel model = tryFindMemberModelByC2jName(memberC2jName, false);

        if (model == null) {
            throw new IllegalArgumentException(memberC2jName + " member (c2j name) does not exist in the shape.");
        }

        return model;
    }

    /**
     * Takes in the c2j member name as input and removes if the shape contains a member with the
     * given name. Return false otherwise.
     */
    public boolean removeMemberByC2jName(String memberC2jName, boolean ignoreCase) {
        // Implicitly depending on the default equals and hashcode
        // implementation of the class MemberModel
        MemberModel model = tryFindMemberModelByC2jName(memberC2jName, ignoreCase);
        return model == null ? false : members.remove(model);
    }

    /**
     * Returns the enum model for the given enum value.
     * Returns null if no such enum value exists.
     */
    public EnumModel findEnumModelByValue(String enumValue) {

        if (enums != null) {
            for (EnumModel enumModel : enums) {
                if (enumValue.equals(enumModel.getValue())) {
                    return enumModel;
                }
            }
        }
        return null;
    }

    @JsonIgnore
    public String getDocumentationShapeName() {
        switch (getShapeType()) {
            case Request:
                return removeFromEnd(shapeName, REQUEST_CLASS_SUFFIX);
            case Response:
                return removeFromEnd(shapeName, RESPONSE_CLASS_SUFFIX);
            default:
                return c2jName;
        }
    }

    public String getUnionTypeGetterDocumentation() {
        return "Retrieve an enum value representing which member of this object is populated. "
               + LF + LF
               + "When this class is returned in a service response, this will be {@link Type#UNKNOWN_TO_SDK_VERSION} if the "
               + "service returned a member that is only known to a newer SDK version."
               + LF + LF
               + "When this class is created directly in your code, this will be {@link Type#UNKNOWN_TO_SDK_VERSION} if zero "
               + "members are set, and {@code null} if more than one member is set.";
    }

    @Override
    public String toString() {
        return shapeName;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Return the httpStatusCode of the exception shape. This value is present only for modeled exceptions.
     */
    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public boolean isRequestSignerAware() {
        return requestSignerClassFqcn != null;
    }

    public String getRequestSignerClassFqcn() {
        return requestSignerClassFqcn;
    }

    public void setRequestSignerClassFqcn(String authorizerClass) {
        this.requestSignerClassFqcn = authorizerClass;
    }

    public EndpointDiscovery getEndpointDiscovery() {
        return endpointDiscovery;
    }

    public void setEndpointDiscovery(EndpointDiscovery endpointDiscovery) {
        this.endpointDiscovery = endpointDiscovery;
    }

    /**
     * @return True if the shape is an 'eventstream' shape. The eventstream shape is the tagged union like
     * container that holds individual 'events'.
     */
    public boolean isEventStream() {
        return this.isEventStream;
    }

    public ShapeModel withIsEventStream(boolean isEventStream) {
        this.isEventStream = isEventStream;
        return this;
    }

    /**
     * @return True if the shape is an 'event'. I.E. It is a member of the eventstream and represents one logical event
     * that can be delivered on the event stream.
     */
    public boolean isEvent() {
        return this.isEvent;
    }

    public ShapeModel withIsEvent(boolean isEvent) {
        this.isEvent = isEvent;
        return this;
    }

    public XmlNamespace getXmlNamespace() {
        return xmlNamespace;
    }

    public ShapeModel withXmlNamespace(XmlNamespace xmlNamespace) {
        this.xmlNamespace = xmlNamespace;
        return this;
    }

    public void setXmlNamespace(XmlNamespace xmlNamespace) {
        this.xmlNamespace = xmlNamespace;
    }

    public boolean isDocument() {
        return document;
    }

    public ShapeModel withIsDocument(boolean document) {
        this.document = document;
        return this;
    }

    public boolean isUnion() {
        return union;
    }

    public void withIsUnion(boolean union) {
        this.union = union;
    }

    public boolean isFault() {
        return fault;
    }

    public ShapeModel withIsFault(boolean fault) {
        this.fault = fault;
        return this;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public ShapeModel withIsRetryable(boolean retryable) {
        this.retryable = retryable;
        return this;
    }

    public boolean isThrottling() {
        return throttling;
    }

    public ShapeModel withIsThrottling(boolean throttling) {
        this.throttling = throttling;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ShapeModel that = (ShapeModel) o;
        return deprecated == that.deprecated
               && hasPayloadMember == that.hasPayloadMember
               && hasHeaderMember == that.hasHeaderMember
               && hasStatusCodeMember == that.hasStatusCodeMember
               && hasStreamingMember == that.hasStreamingMember
               && hasRequiresLengthMember == that.hasRequiresLengthMember
               && wrapper == that.wrapper
               && simpleMethod == that.simpleMethod
               && fault == that.fault
               && isEventStream == that.isEventStream
               && isEvent == that.isEvent
               && document == that.document
               && union == that.union
               && retryable == that.retryable
               && throttling == that.throttling
               && Objects.equals(c2jName, that.c2jName)
               && Objects.equals(shapeName, that.shapeName)
               && Objects.equals(deprecatedMessage, that.deprecatedMessage)
               && Objects.equals(type, that.type)
               && Objects.equals(required, that.required)
               && Objects.equals(requestSignerClassFqcn, that.requestSignerClassFqcn)
               && Objects.equals(endpointDiscovery, that.endpointDiscovery)
               && Objects.equals(members, that.members)
               && Objects.equals(enums, that.enums)
               && Objects.equals(variable, that.variable)
               && Objects.equals(marshaller, that.marshaller)
               && Objects.equals(unmarshaller, that.unmarshaller)
               && Objects.equals(errorCode, that.errorCode)
               && Objects.equals(httpStatusCode, that.httpStatusCode)
               && Objects.equals(customization, that.customization)
               && Objects.equals(xmlNamespace, that.xmlNamespace);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(c2jName);
        result = 31 * result + Objects.hashCode(shapeName);
        result = 31 * result + Boolean.hashCode(deprecated);
        result = 31 * result + Objects.hashCode(deprecatedMessage);
        result = 31 * result + Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(required);
        result = 31 * result + Boolean.hashCode(hasPayloadMember);
        result = 31 * result + Boolean.hashCode(hasHeaderMember);
        result = 31 * result + Boolean.hashCode(hasStatusCodeMember);
        result = 31 * result + Boolean.hashCode(hasStreamingMember);
        result = 31 * result + Boolean.hashCode(hasRequiresLengthMember);
        result = 31 * result + Boolean.hashCode(wrapper);
        result = 31 * result + Boolean.hashCode(simpleMethod);
        result = 31 * result + Objects.hashCode(requestSignerClassFqcn);
        result = 31 * result + Objects.hashCode(endpointDiscovery);
        result = 31 * result + Objects.hashCode(members);
        result = 31 * result + Objects.hashCode(enums);
        result = 31 * result + Objects.hashCode(variable);
        result = 31 * result + Objects.hashCode(marshaller);
        result = 31 * result + Objects.hashCode(unmarshaller);
        result = 31 * result + Objects.hashCode(errorCode);
        result = 31 * result + Objects.hashCode(httpStatusCode);
        result = 31 * result + Boolean.hashCode(fault);
        result = 31 * result + Objects.hashCode(customization);
        result = 31 * result + Boolean.hashCode(isEventStream);
        result = 31 * result + Boolean.hashCode(isEvent);
        result = 31 * result + Objects.hashCode(xmlNamespace);
        result = 31 * result + Boolean.hashCode(document);
        result = 31 * result + Boolean.hashCode(union);
        result = 31 * result + Boolean.hashCode(retryable);
        result = 31 * result + Boolean.hashCode(throttling);
        return result;
    }
}
