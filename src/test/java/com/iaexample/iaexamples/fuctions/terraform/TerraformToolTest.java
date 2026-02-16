package com.iaexample.iaexamples.fuctions.terraform;

import com.iaexample.iaexamples.fuctions.terraform.model.plans.InfraPlan;
import com.iaexample.iaexamples.fuctions.terraform.model.plans.PlanResponse;
import com.iaexample.iaexamples.fuctions.terraform.model.specs.EcsClusterSpec;
import com.iaexample.iaexamples.fuctions.terraform.model.specs.S3Spec;
import com.iaexample.iaexamples.fuctions.terraform.model.specs.SqsSpec;
import com.iaexample.iaexamples.fuctions.terraform.model.terraform.ExecuteRequest;
import com.iaexample.iaexamples.fuctions.terraform.model.terraform.TerraformRequest;
import com.iaexample.iaexamples.fuctions.terraform.model.terraform.TerraformResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerraformToolTest {

    @Mock
    private InfraPlanService planService;

    @Mock
    private TerraformGenerator generator;

    @Mock
    private TerraformExecutor executor;

    private TerraformCodeHolder codeHolder;
    private TerraformTool terraformTool;
    private Function<TerraformRequest, PlanResponse> planejarInfraFunction;
    private Function<ExecuteRequest, TerraformResponse> executarInfraFunction;

    @BeforeEach
    void setUp() {
        codeHolder = new TerraformCodeHolder();
        terraformTool = new TerraformTool();
        planejarInfraFunction = terraformTool.planejarInfra(planService, generator, codeHolder);
        executarInfraFunction = terraformTool.executarInfra(codeHolder, executor);
    }

    @Nested
    @DisplayName("planejarInfra")
    class PlanejarInfraTests {

        @Test
        @DisplayName("Should generate plan and store Terraform code")
        void shouldGeneratePlanAndStoreCode() {
            var s3 = new S3Spec("my-bucket", true, true);
            var plan = new InfraPlan("us-east-1", List.of(s3), Map.of("env", "dev"));
            String tfCode = "resource \"aws_s3_bucket\" \"my-bucket\" {}";

            when(planService.gerarPlano(anyString())).thenReturn(plan);
            when(generator.generate(plan)).thenReturn(tfCode);

            PlanResponse response = planejarInfraFunction.apply(new TerraformRequest("Criar bucket S3"));

            assertEquals("PLANO_GERADO", response.status());
            assertEquals(tfCode, response.terraformCode());
            assertTrue(codeHolder.hasPendingCode());
            assertEquals(tfCode, codeHolder.retrieve());
        }

        @Test
        @DisplayName("Should include resource details in plan description")
        void shouldIncludeResourceDetailsInPlanDescription() {
            var s3 = new S3Spec("my-bucket", true, true);
            var plan = new InfraPlan("us-east-1", List.of(s3), Map.of());

            when(planService.gerarPlano(anyString())).thenReturn(plan);
            when(generator.generate(plan)).thenReturn("tf-code");

            PlanResponse response = planejarInfraFunction.apply(new TerraformRequest("Criar bucket"));

            assertTrue(response.planDescription().contains("S3 Bucket"));
            assertTrue(response.planDescription().contains("my-bucket"));
        }

        @Test
        @DisplayName("Should include SQS details in plan description")
        void shouldIncludeSqsDetailsInPlanDescription() {
            var sqs = new SqsSpec("my-queue", true, 60);
            var plan = new InfraPlan("us-east-1", List.of(sqs), Map.of());

            when(planService.gerarPlano(anyString())).thenReturn(plan);
            when(generator.generate(plan)).thenReturn("tf-code");

            PlanResponse response = planejarInfraFunction.apply(new TerraformRequest("Criar fila SQS"));

            assertTrue(response.planDescription().contains("SQS Queue"));
            assertTrue(response.planDescription().contains("my-queue"));
            assertTrue(response.planDescription().contains("fifo=true"));
            assertTrue(response.planDescription().contains("visibilityTimeout=60"));
        }

        @Test
        @DisplayName("Should include ECS details in plan description")
        void shouldIncludeEcsDetailsInPlanDescription() {
            var ecs = new EcsClusterSpec("my-cluster", "my-svc", "my-task", 256, 512);
            var plan = new InfraPlan("us-east-1", List.of(ecs), Map.of());

            when(planService.gerarPlano(anyString())).thenReturn(plan);
            when(generator.generate(plan)).thenReturn("tf-code");

            PlanResponse response = planejarInfraFunction.apply(new TerraformRequest("Criar ECS cluster"));

            assertTrue(response.planDescription().contains("ECS Cluster"));
            assertTrue(response.planDescription().contains("my-cluster"));
            assertTrue(response.planDescription().contains("service=my-svc"));
            assertTrue(response.planDescription().contains("task=my-task"));
        }

        @Test
        @DisplayName("Should include tags in plan description when present")
        void shouldIncludeTagsInPlanDescription() {
            var s3 = new S3Spec("bucket", true, true);
            var plan = new InfraPlan("us-east-1", List.of(s3), Map.of("env", "prod"));

            when(planService.gerarPlano(anyString())).thenReturn(plan);
            when(generator.generate(plan)).thenReturn("tf-code");

            PlanResponse response = planejarInfraFunction.apply(new TerraformRequest("Criar bucket"));

            assertTrue(response.planDescription().contains("Tags:"));
            assertTrue(response.planDescription().contains("env=prod"));
        }

        @Test
        @DisplayName("Should not include tags section when tags are null")
        void shouldNotIncludeTagsSectionWhenNull() {
            var s3 = new S3Spec("bucket", true, true);
            var plan = new InfraPlan("us-east-1", List.of(s3), null);

            when(planService.gerarPlano(anyString())).thenReturn(plan);
            when(generator.generate(plan)).thenReturn("tf-code");

            PlanResponse response = planejarInfraFunction.apply(new TerraformRequest("Criar bucket"));

            assertFalse(response.planDescription().contains("Tags:"));
        }

        @Test
        @DisplayName("Should not include tags section when tags are empty")
        void shouldNotIncludeTagsSectionWhenEmpty() {
            var s3 = new S3Spec("bucket", true, true);
            var plan = new InfraPlan("us-east-1", List.of(s3), Map.of());

            when(planService.gerarPlano(anyString())).thenReturn(plan);
            when(generator.generate(plan)).thenReturn("tf-code");

            PlanResponse response = planejarInfraFunction.apply(new TerraformRequest("Criar bucket"));

            assertFalse(response.planDescription().contains("Tags:"));
        }

        @Test
        @DisplayName("Should return error response when plan generation fails")
        void shouldReturnErrorWhenPlanGenerationFails() {
            when(planService.gerarPlano(anyString()))
                    .thenThrow(new RuntimeException("Connection failed"));

            PlanResponse response = planejarInfraFunction.apply(new TerraformRequest("Criar bucket"));

            assertEquals("ERRO", response.status());
            assertTrue(response.planDescription().contains("Connection failed"));
        }
    }

    @Nested
    @DisplayName("executarInfra")
    class ExecutarInfraTests {

        @Test
        @DisplayName("Should execute infrastructure when user confirms")
        void shouldExecuteWhenUserConfirms() {
            String tfCode = "resource \"aws_s3_bucket\" \"test\" {}";
            codeHolder.store(tfCode);
            var expectedResponse = new TerraformResponse("SUCESSO", tfCode, "Apply complete!");

            when(executor.execute(tfCode)).thenReturn(expectedResponse);

            TerraformResponse response = executarInfraFunction.apply(new ExecuteRequest(true));

            assertEquals("SUCESSO", response.status());
            assertFalse(codeHolder.hasPendingCode());
        }

        @Test
        @DisplayName("Should cancel execution when user denies")
        void shouldCancelWhenUserDenies() {
            codeHolder.store("some code");

            TerraformResponse response = executarInfraFunction.apply(new ExecuteRequest(false));

            assertEquals("CANCELADO", response.status());
            assertFalse(codeHolder.hasPendingCode());
            verify(executor, never()).execute(anyString());
        }

        @Test
        @DisplayName("Should return error when no pending plan exists")
        void shouldReturnErrorWhenNoPendingPlan() {
            TerraformResponse response = executarInfraFunction.apply(new ExecuteRequest(true));

            assertEquals("ERRO", response.status());
            verify(executor, never()).execute(anyString());
        }

        @Test
        @DisplayName("Should clear code holder after successful execution")
        void shouldClearCodeHolderAfterExecution() {
            String tfCode = "resource {}";
            codeHolder.store(tfCode);

            when(executor.execute(tfCode))
                    .thenReturn(new TerraformResponse("SUCESSO", tfCode, "Done"));

            executarInfraFunction.apply(new ExecuteRequest(true));

            assertFalse(codeHolder.hasPendingCode());
        }
    }
}
