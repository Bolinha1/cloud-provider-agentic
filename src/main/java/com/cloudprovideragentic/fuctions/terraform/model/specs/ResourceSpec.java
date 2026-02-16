package com.cloudprovideragentic.fuctions.terraform.model.specs;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = S3Spec.class, name = "S3Spec"),
        @JsonSubTypes.Type(value = SqsSpec.class, name = "SqsSpec"),
        @JsonSubTypes.Type(value = EcsClusterSpec.class, name = "EcsClusterSpec")
})
public sealed interface ResourceSpec
        permits S3Spec, SqsSpec, EcsClusterSpec {
}
