package com.iaexample.iaexamples.fuctions.terraform.model;


public record S3Spec(
        String name,
        boolean versioning,
        boolean encrypted
) implements ResourceSpec {}
