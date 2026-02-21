package com.cloudprovideragentic.fuctions.terraform;

import com.cloudprovideragentic.fuctions.terraform.model.plans.PlanResponse;
import com.cloudprovideragentic.fuctions.terraform.model.plans.TerraformPlanResult;
import com.cloudprovideragentic.fuctions.terraform.model.terraform.ExecuteRequest;
import com.cloudprovideragentic.fuctions.terraform.model.terraform.TerraformRequest;
import com.cloudprovideragentic.fuctions.terraform.model.terraform.TerraformResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerraformToolTest {

    @Mock
    private TerraformCodeGeneratorService codeGenerator;

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
        planejarInfraFunction = terraformTool.planejarInfra(codeGenerator, codeHolder);
        executarInfraFunction = terraformTool.executarInfra(codeHolder, executor);
    }

    @Nested
    @DisplayName("planejarInfra")
    class PlanejarInfraTests {

        @Test
        @DisplayName("Should generate plan and store Terraform code")
        void shouldGeneratePlanAndStoreCode() {
            String tfCode = "resource \"aws_s3_bucket\" \"my-bucket\" {}";
            String description = "Será criado: S3 bucket my-bucket com versionamento";
            var planResult = new TerraformPlanResult(description, tfCode);

            when(codeGenerator.generate(anyString())).thenReturn(planResult);

            PlanResponse response = planejarInfraFunction.apply(new TerraformRequest("Criar bucket S3"));

            assertEquals("PLANO_GERADO", response.status());
            assertEquals(tfCode, response.terraformCode());
            assertEquals(description, response.planDescription());
            assertTrue(codeHolder.hasPendingCode());
            assertEquals(tfCode, codeHolder.retrieve());
        }

        @Test
        @DisplayName("Should pass user prompt to code generator")
        void shouldPassUserPromptToCodeGenerator() {
            String userPrompt = "Criar bucket S3 na regiao sa-east-1";
            var planResult = new TerraformPlanResult("Descrição", "tf-code");

            when(codeGenerator.generate(anyString())).thenReturn(planResult);

            planejarInfraFunction.apply(new TerraformRequest(userPrompt));

            verify(codeGenerator).generate(userPrompt);
        }

        @Test
        @DisplayName("Should return error response when code generation fails")
        void shouldReturnErrorWhenCodeGenerationFails() {
            when(codeGenerator.generate(anyString()))
                    .thenThrow(new RuntimeException("Connection failed"));

            PlanResponse response = planejarInfraFunction.apply(new TerraformRequest("Criar bucket"));

            assertEquals("ERRO", response.status());
            assertTrue(response.planDescription().contains("Connection failed"));
            assertFalse(codeHolder.hasPendingCode());
        }

        @Test
        @DisplayName("Should not include tags section when tags are null")
        void shouldNotStoreCodeOnError() {
            when(codeGenerator.generate(anyString()))
                    .thenThrow(new RuntimeException("Model unavailable"));

            planejarInfraFunction.apply(new TerraformRequest("Criar infra"));

            assertFalse(codeHolder.hasPendingCode());
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
