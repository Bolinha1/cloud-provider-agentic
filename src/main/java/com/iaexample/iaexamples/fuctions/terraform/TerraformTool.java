package com.iaexample.iaexamples.fuctions.terraform;

import com.iaexample.iaexamples.fuctions.terraform.model.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class TerraformTool {

    @Bean(name = "provisionarInfra")
    public Function<TerraformRequest, TerraformResponse> provisionarInfra(
            InfraPlanService planService,
            TerraformGenerator generator,
            TerraformExecutor executor
    ) {
        return request -> {
            InfraPlan plan = planService.gerarPlano(request.userPrompt());
            String tf = generator.generate(plan);
            return executor.execute(tf);
        };
    }
}