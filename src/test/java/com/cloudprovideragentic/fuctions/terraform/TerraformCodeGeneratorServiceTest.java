package com.cloudprovideragentic.fuctions.terraform;

import com.cloudprovideragentic.fuctions.terraform.model.plans.TerraformPlanResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerraformCodeGeneratorServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    @Test
    @DisplayName("Should return TerraformPlanResult from model response")
    void shouldReturnPlanResultFromModel() {
        var expected = new TerraformPlanResult(
                "Será criado: S3 bucket my-bucket com versionamento",
                "resource \"aws_s3_bucket\" \"my-bucket\" {}"
        );

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(TerraformPlanResult.class)).thenReturn(expected);

        var service = new TerraformCodeGeneratorService(chatClientBuilder);
        TerraformPlanResult result = service.generate("Criar bucket S3 my-bucket com versionamento");

        assertEquals(expected.planDescription(), result.planDescription());
        assertEquals(expected.terraformCode(), result.terraformCode());
    }

    @Test
    @DisplayName("Should pass user prompt to the chat client")
    void shouldPassUserPromptToChatClient() {
        String userPrompt = "Criar fila SQS FIFO chamada pedidos";
        var planResult = new TerraformPlanResult("Descrição", "tf-code");

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(TerraformPlanResult.class)).thenReturn(planResult);

        var service = new TerraformCodeGeneratorService(chatClientBuilder);
        service.generate(userPrompt);

        verify(requestSpec).user(userPrompt);
    }

    @Test
    @DisplayName("Should strip markdown code fences from terraform code")
    void shouldStripMarkdownCodeFences() {
        String rawCode = "```terraform\nresource \"aws_s3_bucket\" \"b\" {}\n```";
        var modelResult = new TerraformPlanResult("Descrição", rawCode);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(TerraformPlanResult.class)).thenReturn(modelResult);

        var service = new TerraformCodeGeneratorService(chatClientBuilder);
        TerraformPlanResult result = service.generate("Criar bucket");

        assertFalse(result.terraformCode().contains("```"));
        assertTrue(result.terraformCode().contains("resource \"aws_s3_bucket\""));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when model returns null")
    void shouldThrowWhenModelReturnsNull() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(TerraformPlanResult.class)).thenReturn(null);

        var service = new TerraformCodeGeneratorService(chatClientBuilder);

        assertThrows(IllegalStateException.class, () -> service.generate("Criar bucket"));
    }

    @Test
    @DisplayName("Should throw NullPointerException when userPrompt is null")
    void shouldThrowWhenUserPromptIsNull() {
        when(chatClientBuilder.build()).thenReturn(chatClient);

        var service = new TerraformCodeGeneratorService(chatClientBuilder);

        assertThrows(NullPointerException.class, () -> service.generate(null));
    }

    @Test
    @DisplayName("Should propagate exception when chat client fails")
    void shouldPropagateExceptionWhenChatClientFails() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("API unavailable"));

        var service = new TerraformCodeGeneratorService(chatClientBuilder);

        assertThrows(RuntimeException.class, () -> service.generate("Criar bucket S3"));
    }
}
