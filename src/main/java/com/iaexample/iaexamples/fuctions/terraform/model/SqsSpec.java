package com.iaexample.iaexamples.fuctions.terraform.model;

public record SqsSpec(
        String name,
        boolean fifo,
        int visibilityTimeout
) implements ResourceSpec {}
