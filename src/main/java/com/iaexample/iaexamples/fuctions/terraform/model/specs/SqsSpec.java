package com.iaexample.iaexamples.fuctions.terraform.model.specs;

public record SqsSpec(
        String name,
        boolean fifo,
        int visibilityTimeout
) implements ResourceSpec {}
