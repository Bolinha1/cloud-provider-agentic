package com.iaexample.iaexamples.fuctions.terraform;

import com.iaexample.iaexamples.fuctions.terraform.model.TerraformRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
public class TerraformAgentController {

    private final ChatClient chatClient;

    public TerraformAgentController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                      Você é um Agente de Automação Cloud.
                      Quando o usuário pedir para criar um recurso (ex: bucket S3):
                      1. Identifique o nome do recurso e a 'region'.
                      2. Se a 'region' não for informada, use 'us-east-1' como padrão.
                      3. Chame a tool 'provisionarInfra' passando EXCLUSIVAMENTE este formato:
                         {"userPrompt": "Criar [recurso] com nome [nome] na regiao [region]"}

                      NÃO peça e NÃO manipule chaves de acesso AWS.
                    """)
                .defaultToolNames("provisionarInfra")
                .build();
    }

    @PostMapping("/chat/provisionar")
    public String chat(@RequestBody String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
