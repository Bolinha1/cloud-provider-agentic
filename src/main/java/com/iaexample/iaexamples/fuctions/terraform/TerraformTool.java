package com.iaexample.iaexamples.fuctions.terraform;

import com.iaexample.iaexamples.fuctions.terraform.model.plans.InfraPlan;
import com.iaexample.iaexamples.fuctions.terraform.model.plans.PlanResponse;
import com.iaexample.iaexamples.fuctions.terraform.model.specs.*;
import com.iaexample.iaexamples.fuctions.terraform.model.terraform.ExecuteRequest;
import com.iaexample.iaexamples.fuctions.terraform.model.terraform.TerraformRequest;
import com.iaexample.iaexamples.fuctions.terraform.model.terraform.TerraformResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class TerraformTool {

    @Bean(name = "planejarInfra")
    public Function<TerraformRequest, PlanResponse> planejarInfra(
            InfraPlanService planService,
            TerraformGenerator generator,
            TerraformCodeHolder codeHolder
    ) {
        return request -> {
            try {
                InfraPlan plan = planService.gerarPlano(request.userPrompt());
                String tfCode = generator.generate(plan);

                codeHolder.store(tfCode);

                String description = buildPlanDescription(plan);

                return new PlanResponse("PLANO_GERADO", description, tfCode);
            } catch (Exception e) {
                return new PlanResponse("ERRO", e.getMessage(), "");
            }
        };
    }

    @Bean(name = "executarInfra")
    public Function<ExecuteRequest, TerraformResponse> executarInfra(
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

    private String buildPlanDescription(InfraPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("Regiao: ").append(plan.region()).append("\n");
        sb.append("Recursos a criar:\n");

        for (ResourceSpec r : plan.resources()) {
            switch (r) {
                case S3Spec s3 -> sb.append("  - S3 Bucket: ").append(s3.name())
                        .append(" (versioning=").append(s3.versioning())
                        .append(", encrypted=").append(s3.encrypted()).append(")\n");
                case SqsSpec sqs -> sb.append("  - SQS Queue: ").append(sqs.name())
                        .append(" (fifo=").append(sqs.fifo())
                        .append(", visibilityTimeout=").append(sqs.visibilityTimeout()).append(")\n");
                case EcsClusterSpec ecs -> sb.append("  - ECS Cluster: ").append(ecs.clusterName())
                        .append(" (service=").append(ecs.serviceName())
                        .append(", task=").append(ecs.taskName()).append(")\n");
            }
        }

        if (plan.tags() != null && !plan.tags().isEmpty()) {
            sb.append("Tags: ").append(
                    plan.tags().entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(Collectors.joining(", "))
            );
        }

        return sb.toString();
    }
}
