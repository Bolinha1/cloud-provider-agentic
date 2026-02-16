package com.cloudprovideragentic.fuctions.terraform.model.plans;

import com.cloudprovideragentic.fuctions.terraform.model.specs.ResourceSpec;

import java.util.List;
import java.util.Map;

public record InfraPlan(
        String region,
        List<ResourceSpec> resources,
        Map<String, String> tags
) {}
