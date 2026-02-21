package com.cloudprovideragentic.fuctions.terraform;

import com.cloudprovideragentic.fuctions.terraform.model.plans.PlanResponse;
import com.cloudprovideragentic.fuctions.terraform.model.plans.TerraformPlanResult;
import com.cloudprovideragentic.fuctions.terraform.model.terraform.ExecuteRequest;
import com.cloudprovideragentic.fuctions.terraform.model.terraform.TerraformRequest;
import com.cloudprovideragentic.fuctions.terraform.model.terraform.TerraformResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class TerraformTool {

    @Bean(name = "planejarInfra")
    Function<TerraformRequest, PlanResponse> planejarInfra(
            TerraformCodeGeneratorService codeGenerator,
            TerraformCodeHolder codeHolder
    ) {
        return request -> {
            try {
                TerraformPlanResult result = codeGenerator.generate(request.userPrompt());
                codeHolder.store(result.terraformCode());
                return new PlanResponse("PLANO_GERADO", result.planDescription(), result.terraformCode());
            } catch (Exception e) {
                return new PlanResponse("ERRO", e.getMessage(), "");
            }
        };
    }

    @Bean(name = "executarInfra")
    Function<ExecuteRequest, TerraformResponse> executarInfra(
            TerraformCodeHolder codeHolder,
            TerraformExecutor executor
    ) {
        return request -> {
            if (!request.confirmar()) {
                codeHolder.clear();
                return new TerraformResponse("CANCELADO", "", "Execucao cancelada pelo usuario.");
            }

            if (!codeHolder.hasPendingCode()) {
                return new TerraformResponse("ERRO", "", "Nenhum plano pendente. Chame 'planejarInfra' primeiro.");
            }

            String tfCode = codeHolder.retrieve();
            TerraformResponse response = executor.execute(tfCode);
            codeHolder.clear();
            return response;
        };
    }
}
