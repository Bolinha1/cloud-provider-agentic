package com.iaexample.iaexamples.fuctions.terraform;

import com.iaexample.iaexamples.fuctions.terraform.model.plans.InfraPlan;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class InfraPlanService {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
           Você é um arquiteto AWS especialista em Terraform.
           Gere um JSON seguindo estritamente esta estrutura para que o Jackson possa desserializar:

           {
             "region": "string",
             "resources": [
               {
                 "@type": "S3Spec",
                 "name": "nome-do-bucket",
                 "versioning": true,
                 "encrypted": true
               },
               {
                 "@type": "SqsSpec",
                 "name": "nome-fila",
                 "fifo": false,
                 "visibilityTimeout": 30
               }
             ],
             "tags": { "Chave": "Valor" }
           }

           Regras cruciais:
           - O campo "@type" deve ser EXATAMENTE o nome da classe (S3Spec, SqsSpec ou EcsClusterSpec).
           - Não aninhe os recursos dentro de chaves com o nome do recurso.
           - Retorne apenas o JSON.
        """;

    public InfraPlanService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public InfraPlan gerarPlano(String userPrompt) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .entity(InfraPlan.class);
    }
}