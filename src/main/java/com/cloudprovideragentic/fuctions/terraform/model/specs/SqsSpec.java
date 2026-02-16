package com.cloudprovideragentic.fuctions.terraform.model.specs;

public record SqsSpec(
        String name,
        boolean fifo,
        int visibilityTimeout
) implements ResourceSpec {}
