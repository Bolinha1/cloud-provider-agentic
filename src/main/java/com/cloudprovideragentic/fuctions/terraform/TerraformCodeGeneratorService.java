package com.cloudprovideragentic.fuctions.terraform;

import com.cloudprovideragentic.fuctions.terraform.model.plans.TerraformPlanResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class TerraformCodeGeneratorService {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            Você é um especialista em Terraform para AWS.
            Dado o pedido do usuário, gere um JSON com exatamente esta estrutura:

            {
              "planDescription": "Descrição clara e legível dos recursos que serão criados",
              "terraformCode": "terraform {\\n  required_providers {\\n    aws = {\\n      source = \\"hashicorp/aws\\"\\n    }\\n  }\\n}\\n\\nprovider \\"aws\\" {\\n  region = \\"us-east-1\\"\\n}\\n\\nresource \\"aws_s3_bucket\\" \\"example\\" {\\n  bucket = \\"example\\"\\n}"
            }

            Retorne APENAS o JSON, sem blocos de código markdown, sem explicações fora do JSON.

            FORMATO OBRIGATÓRIO DO TERRAFORM:
            - Use EXCLUSIVAMENTE sintaxe HCL (HashiCorp Configuration Language)
            - NUNCA use JSON Terraform format (nunca use { "resource": { "aws_s3_bucket": ... } })
            - Exemplo correto de HCL: resource "aws_s3_bucket" "name" { bucket = "name" }
            - O campo "terraformCode" é uma string JSON: newlines devem ser \\n e aspas devem ser \\"

            INSTRUÇÕES GERAIS PARA O CÓDIGO TERRAFORM:
            - Sempre inclua o bloco terraform{} com required_providers para aws (source = "hashicorp/aws")
            - Sempre inclua o bloco provider "aws" com a região adequada (padrão: us-east-1)
            - Use nomes de recursos compatíveis com Terraform (sem espaços, use hifens ou underscores)

            INSTRUÇÕES PARA S3:
            - Crie aws_s3_bucket com o nome especificado
            - Se versionamento solicitado, adicione aws_s3_bucket_versioning com status "Enabled"
            - Se criptografia solicitada, adicione aws_s3_bucket_server_side_encryption_configuration
            - Adicione aws_s3_bucket_public_access_block com todos os campos true por padrão

            INSTRUÇÕES PARA SQS:
            - Crie aws_sqs_queue com o nome especificado
            - Se FIFO solicitado, o nome deve terminar com .fifo e adicione content_based_deduplication = true
            - Configure visibility_timeout_seconds conforme solicitado (padrão: 30)
            - Se Dead Letter Queue solicitada, crie uma segunda fila e configure redrive_policy

            INSTRUÇÕES PARA ECS:
            - Use FARGATE como launch_type
            - Crie aws_ecs_cluster, aws_ecs_task_definition e aws_ecs_service
            - Crie IAM role de execução com a policy AmazonECSTaskExecutionRolePolicy
            - Crie infraestrutura de rede: aws_vpc, aws_subnet, aws_internet_gateway,
              aws_route_table, aws_route_table_association, aws_security_group
            - Configure network_mode = "awsvpc" na task definition

            INSTRUÇÕES PARA OUTROS SERVIÇOS AWS:
            - Para RDS: use aws_db_instance com security groups e subnet groups adequados
            - Para Lambda: use aws_lambda_function com aws_iam_role e aws_lambda_permission
            - Para API Gateway: use aws_api_gateway_rest_api integrado com o backend adequado
            - Para qualquer outro serviço AWS não listado acima, siga os mesmos padrões estabelecidos:
              blocos terraform{} e provider, recursos nomeados coerentemente, referências entre recursos
              via atributos (ex: .id, .arn), e código estritamente compatível com a documentação
              oficial do Terraform Registry (registry.terraform.io/providers/hashicorp/aws)
            """;

    public TerraformCodeGeneratorService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public TerraformPlanResult generate(String userPrompt) {
        TerraformPlanResult result = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(Objects.requireNonNull(userPrompt, "userPrompt não pode ser nulo"))
                .call()
                .entity(TerraformPlanResult.class);

        if (result == null) {
            throw new IllegalStateException("O modelo não retornou um plano válido.");
        }

        String cleanCode = result.terraformCode()
                .replaceAll("```(?:terraform|hcl)?\\n?", "")
                .replaceAll("```", "")
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .trim();

        return new TerraformPlanResult(result.planDescription(), cleanCode);
    }
}
