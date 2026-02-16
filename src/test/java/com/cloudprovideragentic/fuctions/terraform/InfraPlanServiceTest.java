package com.cloudprovideragentic.fuctions.terraform;

import com.cloudprovideragentic.fuctions.terraform.model.plans.InfraPlan;
import com.cloudprovideragentic.fuctions.terraform.model.specs.S3Spec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfraPlanServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    @Test
    @DisplayName("Should generate infrastructure plan from user prompt")
    void shouldGenerateInfraPlanFromUserPrompt() {
        var expectedPlan = new InfraPlan(
                "us-east-1",
                List.of(new S3Spec("test-bucket", true, true)),
                Map.of("env", "dev")
        );

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(InfraPlan.class)).thenReturn(expectedPlan);

        InfraPlanService service = new InfraPlanService(chatClientBuilder);
        InfraPlan result = service.gerarPlano("Criar bucket S3 com nome test-bucket");

        assertEquals("us-east-1", result.region());
        assertEquals(1, result.resources().size());
        assertInstanceOf(S3Spec.class, result.resources().getFirst());
    }

    @Test
    @DisplayName("Should pass user prompt to the chat client")
    void shouldPassUserPromptToChatClient() {
        String userPrompt = "Criar bucket S3 na regiao sa-east-1";

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(InfraPlan.class))
                .thenReturn(new InfraPlan("sa-east-1", List.of(), Map.of()));

        InfraPlanService service = new InfraPlanService(chatClientBuilder);
        service.gerarPlano(userPrompt);

        verify(requestSpec).user(userPrompt);
    }

    @Test
    @DisplayName("Should propagate exception when chat client fails")
    void shouldPropagateExceptionWhenChatClientFails() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("API unavailable"));

        InfraPlanService service = new InfraPlanService(chatClientBuilder);

        assertThrows(RuntimeException.class,
                () -> service.gerarPlano("Criar bucket S3"));
    }
}
