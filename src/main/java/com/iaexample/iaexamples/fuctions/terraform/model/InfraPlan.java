package com.iaexample.iaexamples.fuctions.terraform.model;

import java.util.List;
import java.util.Map;

public record InfraPlan(
        String region,
        List<ResourceSpec> resources,
        Map<String, String> tags
) {}