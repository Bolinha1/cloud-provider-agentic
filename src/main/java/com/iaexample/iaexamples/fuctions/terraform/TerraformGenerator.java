package com.iaexample.iaexamples.fuctions.terraform;

import com.iaexample.iaexamples.fuctions.terraform.model.InfraPlan;
import com.iaexample.iaexamples.fuctions.terraform.model.specs.*;
import org.springframework.stereotype.Component;

@Component
public class TerraformGenerator {

    public String generate(InfraPlan plan) {
        StringBuilder tf = new StringBuilder();

        tf.append("""
            terraform {
              required_providers {
                aws = {
                  source = "hashicorp/aws"
                }
              }
            }

            provider "aws" {
              region = "%s"
            }
            """.formatted(plan.region()));

        for (ResourceSpec r : plan.resources()) {
            switch (r) {
                case S3Spec s3 -> tf.append(s3(s3));
                case SqsSpec sqs -> tf.append(sqs(sqs));
                case EcsClusterSpec ecs -> tf.append(ecs(ecs));
            }
        }

        return tf.toString();
    }

    private String s3(S3Spec s3) {
        return """
            resource "aws_s3_bucket" "%s" {
              bucket = "%s"
            }

            resource "aws_s3_bucket_versioning" "%s_v" {
              bucket = aws_s3_bucket.%s.id
              versioning_configuration { status = "%s" }
            }
            """.formatted(
                s3.name(),
                s3.name(),
                s3.name(),
                s3.name(),
                s3.versioning() ? "Enabled" : "Suspended"
        );
    }

    private String sqs(SqsSpec sqs) {
        return """
            resource "aws_sqs_queue" "%s" {
              name = "%s"
              fifo_queue = %s
              visibility_timeout_seconds = %d
            }
            """.formatted(
                sqs.name(),
                sqs.name(),
                sqs.fifo(),
                sqs.visibilityTimeout()
        );
    }

    private String ecs(EcsClusterSpec ecs) {
        return """
            resource "aws_ecs_cluster" "%s" {
              name = "%s"
            }
            """.formatted(
                ecs.clusterName(),
                ecs.clusterName()
        );
    }
}