package com.cloudprovideragentic.fuctions.terraform;

import com.cloudprovideragentic.fuctions.terraform.model.plans.InfraPlan;
import com.cloudprovideragentic.fuctions.terraform.model.specs.EcsClusterSpec;
import com.cloudprovideragentic.fuctions.terraform.model.specs.S3Spec;
import com.cloudprovideragentic.fuctions.terraform.model.specs.SqsSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TerraformGeneratorTest {

    private TerraformGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new TerraformGenerator();
    }

    @Test
    @DisplayName("Should generate Terraform provider block with correct region")
    void shouldGenerateProviderBlockWithCorrectRegion() {
        var plan = new InfraPlan("sa-east-1", List.of(), Map.of());

        String result = generator.generate(plan);

        assertTrue(result.contains("region = \"sa-east-1\""));
        assertTrue(result.contains("source = \"hashicorp/aws\""));
    }

    @Test
    @DisplayName("Should generate S3 bucket resource with versioning enabled")
    void shouldGenerateS3BucketWithVersioningEnabled() {
        var s3 = new S3Spec("my-bucket", true, true);
        var plan = new InfraPlan("us-east-1", List.of(s3), Map.of());

        String result = generator.generate(plan);

        assertTrue(result.contains("resource \"aws_s3_bucket\" \"my-bucket\""));
        assertTrue(result.contains("bucket = \"my-bucket\""));
        assertTrue(result.contains("resource \"aws_s3_bucket_versioning\" \"my-bucket_v\""));
        assertTrue(result.contains("status = \"Enabled\""));
    }

    @Test
    @DisplayName("Should generate S3 bucket resource with versioning suspended")
    void shouldGenerateS3BucketWithVersioningSuspended() {
        var s3 = new S3Spec("my-bucket", false, false);
        var plan = new InfraPlan("us-east-1", List.of(s3), Map.of());

        String result = generator.generate(plan);

        assertTrue(result.contains("status = \"Suspended\""));
    }

    @Test
    @DisplayName("Should generate SQS queue resource")
    void shouldGenerateSqsQueueResource() {
        var sqs = new SqsSpec("my-queue", false, 30);
        var plan = new InfraPlan("us-east-1", List.of(sqs), Map.of());

        String result = generator.generate(plan);

        assertTrue(result.contains("resource \"aws_sqs_queue\" \"my-queue\""));
        assertTrue(result.contains("name = \"my-queue\""));
        assertTrue(result.contains("fifo_queue = false"));
        assertTrue(result.contains("visibility_timeout_seconds = 30"));
    }

    @Test
    @DisplayName("Should generate SQS FIFO queue resource")
    void shouldGenerateSqsFifoQueueResource() {
        var sqs = new SqsSpec("my-queue", true, 60);
        var plan = new InfraPlan("us-east-1", List.of(sqs), Map.of());

        String result = generator.generate(plan);

        assertTrue(result.contains("fifo_queue = true"));
        assertTrue(result.contains("visibility_timeout_seconds = 60"));
    }

    @Test
    @DisplayName("Should generate ECS cluster resource")
    void shouldGenerateEcsClusterResource() {
        var ecs = new EcsClusterSpec("my-cluster", "my-service", "my-task", 256, 512);
        var plan = new InfraPlan("us-east-1", List.of(ecs), Map.of());

        String result = generator.generate(plan);

        assertTrue(result.contains("resource \"aws_ecs_cluster\" \"my-cluster\""));
        assertTrue(result.contains("name = \"my-cluster\""));
    }

    @Test
    @DisplayName("Should generate complete ECS stack with service and task definition")
    void shouldGenerateCompleteEcsStack() {
        var ecs = new EcsClusterSpec("meu-cluster", "meu-servico", "minha-task", 256, 512);
        var plan = new InfraPlan("us-east-1", List.of(ecs), Map.of());

        String result = generator.generate(plan);

        // Verifica o cluster
        assertTrue(result.contains("resource \"aws_ecs_cluster\" \"meu-cluster\""));
        assertTrue(result.contains("name = \"meu-cluster\""));

        // Verifica a task definition
        assertTrue(result.contains("resource \"aws_ecs_task_definition\" \"minha-task\""));
        assertTrue(result.contains("family                   = \"minha-task\""));
        assertTrue(result.contains("requires_compatibilities = [\"FARGATE\"]"));
        assertTrue(result.contains("cpu                      = \"256\""));
        assertTrue(result.contains("memory                   = \"512\""));

        // Verifica o serviço
        assertTrue(result.contains("resource \"aws_ecs_service\" \"meu-servico\""));
        assertTrue(result.contains("name            = \"meu-servico\""));
        assertTrue(result.contains("cluster         = aws_ecs_cluster.meu-cluster.id"));
        assertTrue(result.contains("task_definition = aws_ecs_task_definition.minha-task.arn"));
        assertTrue(result.contains("desired_count   = 1"));
        assertTrue(result.contains("launch_type     = \"FARGATE\""));

        // Verifica os recursos de rede (VPC, subnet, etc.)
        assertTrue(result.contains("resource \"aws_vpc\" \"meu-servico_vpc\""));
        assertTrue(result.contains("resource \"aws_subnet\" \"meu-servico_subnet\""));
        assertTrue(result.contains("resource \"aws_security_group\" \"meu-servico_sg\""));
        assertTrue(result.contains("resource \"aws_internet_gateway\" \"meu-servico_igw\""));

        // Verifica a IAM role
        assertTrue(result.contains("resource \"aws_iam_role\" \"minha-task_execution_role\""));
        assertTrue(result.contains("resource \"aws_iam_role_policy_attachment\" \"minha-task_execution_policy\""));
    }

    @Test
    @DisplayName("Should generate multiple resources in a single plan")
    void shouldGenerateMultipleResourcesInSinglePlan() {
        var s3 = new S3Spec("bucket-1", true, true);
        var sqs = new SqsSpec("queue-1", false, 30);
        var ecs = new EcsClusterSpec("cluster-1", "svc-1", "task-1", 256, 512);
        var plan = new InfraPlan("us-east-1", List.of(s3, sqs, ecs), Map.of());

        String result = generator.generate(plan);

        assertTrue(result.contains("resource \"aws_s3_bucket\" \"bucket-1\""));
        assertTrue(result.contains("resource \"aws_sqs_queue\" \"queue-1\""));
        assertTrue(result.contains("resource \"aws_ecs_cluster\" \"cluster-1\""));
    }
}
