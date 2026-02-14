package com.iaexample.iaexamples.fuctions.terraform.model.specs;

public record EcsClusterSpec(
        String clusterName,
        String serviceName,
        String taskName,
        int cpu,
        int memory
) implements ResourceSpec {}
