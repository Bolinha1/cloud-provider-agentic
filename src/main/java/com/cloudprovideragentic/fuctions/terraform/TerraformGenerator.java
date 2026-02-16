package com.cloudprovideragentic.fuctions.terraform;

import com.cloudprovideragentic.fuctions.terraform.model.plans.InfraPlan;
import com.cloudprovideragentic.fuctions.terraform.model.specs.*;
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

            resource "aws_ecs_task_definition" "%s" {
              family                   = "%s"
              network_mode             = "awsvpc"
              requires_compatibilities = ["FARGATE"]
              cpu                      = "%d"
              memory                   = "%d"
              execution_role_arn       = aws_iam_role.%s_execution_role.arn

              container_definitions = jsonencode([{
                name      = "%s"
                image     = "nginx:latest"
                essential = true
                portMappings = [{
                  containerPort = 80
                  protocol      = "tcp"
                }]
              }])
            }

            resource "aws_iam_role" "%s_execution_role" {
              name = "%s-execution-role"

              assume_role_policy = jsonencode({
                Version = "2012-10-17"
                Statement = [{
                  Action = "sts:AssumeRole"
                  Effect = "Allow"
                  Principal = {
                    Service = "ecs-tasks.amazonaws.com"
                  }
                }]
              })
            }

            resource "aws_iam_role_policy_attachment" "%s_execution_policy" {
              role       = aws_iam_role.%s_execution_role.name
              policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
            }

            resource "aws_ecs_service" "%s" {
              name            = "%s"
              cluster         = aws_ecs_cluster.%s.id
              task_definition = aws_ecs_task_definition.%s.arn
              desired_count   = 1
              launch_type     = "FARGATE"

              network_configuration {
                subnets          = [aws_subnet.%s_subnet.id]
                security_groups  = [aws_security_group.%s_sg.id]
                assign_public_ip = true
              }
            }

            resource "aws_vpc" "%s_vpc" {
              cidr_block           = "10.0.0.0/16"
              enable_dns_hostnames = true
              enable_dns_support   = true
            }

            resource "aws_subnet" "%s_subnet" {
              vpc_id                  = aws_vpc.%s_vpc.id
              cidr_block              = "10.0.1.0/24"
              availability_zone       = data.aws_availability_zones.available.names[0]
              map_public_ip_on_launch = true
            }

            resource "aws_internet_gateway" "%s_igw" {
              vpc_id = aws_vpc.%s_vpc.id
            }

            resource "aws_route_table" "%s_rt" {
              vpc_id = aws_vpc.%s_vpc.id

              route {
                cidr_block = "0.0.0.0/0"
                gateway_id = aws_internet_gateway.%s_igw.id
              }
            }

            resource "aws_route_table_association" "%s_rta" {
              subnet_id      = aws_subnet.%s_subnet.id
              route_table_id = aws_route_table.%s_rt.id
            }

            resource "aws_security_group" "%s_sg" {
              name        = "%s-sg"
              description = "Security group for %s"
              vpc_id      = aws_vpc.%s_vpc.id

              ingress {
                from_port   = 80
                to_port     = 80
                protocol    = "tcp"
                cidr_blocks = ["0.0.0.0/0"]
              }

              egress {
                from_port   = 0
                to_port     = 0
                protocol    = "-1"
                cidr_blocks = ["0.0.0.0/0"]
              }
            }

            data "aws_availability_zones" "available" {
              state = "available"
            }
            """.formatted(
                ecs.clusterName(), ecs.clusterName(),
                ecs.taskName(), ecs.taskName(), ecs.cpu(), ecs.memory(), ecs.taskName(),
                ecs.taskName(),
                ecs.taskName(), ecs.taskName(),
                ecs.taskName(), ecs.taskName(),
                ecs.serviceName(), ecs.serviceName(), ecs.clusterName(), ecs.taskName(),
                ecs.serviceName(), ecs.serviceName(),
                ecs.serviceName(),
                ecs.serviceName(), ecs.serviceName(),
                ecs.serviceName(), ecs.serviceName(),
                ecs.serviceName(), ecs.serviceName(), ecs.serviceName(),
                ecs.serviceName(), ecs.serviceName(), ecs.serviceName(),
                ecs.serviceName(), ecs.serviceName(), ecs.serviceName(), ecs.serviceName()
        );
    }
}