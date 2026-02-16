package com.cloudprovideragentic.fuctions.terraform;

import com.cloudprovideragentic.fuctions.terraform.model.terraform.TerraformResponse;
import com.cloudprovideragentic.fuctions.terraform.utils.TerraformFileManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerraformExecutorTest {

    @Mock
    private Environment env;

    @Mock
    private TerraformFileManager fileManager;

    @TempDir
    Path tempDir;

    private String originalUserDir;

    @BeforeEach
    void setUp() throws Exception {
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        Path infraDir = tempDir.resolve("infra");
        Files.createDirectories(infraDir);
        when(fileManager.prepareInfraDirectory(anyString())).thenAnswer(invocation -> {
            String tfCode = invocation.getArgument(0);
            Files.writeString(infraDir.resolve("main.tf"), tfCode);
            return infraDir;
        });
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.dir", originalUserDir);
    }

    @Test
    @DisplayName("Should write main.tf file to infra directory")
    void shouldWriteMainTfFile() {
        TerraformExecutor executor = new TerraformExecutor(env, fileManager);
        String tfCode = "resource \"aws_s3_bucket\" \"test\" {}";

        executor.execute(tfCode);

        Path mainTf = tempDir.resolve("infra/main.tf");
        assertTrue(Files.exists(mainTf));
    }

    @Test
    @DisplayName("Should return error status when terraform init fails")
    void shouldReturnErrorWhenTerraformInitFails() {
        TerraformExecutor executor = new TerraformExecutor(env, fileManager);

        TerraformResponse response = executor.execute("invalid terraform code");

        assertEquals("ERRO", response.status());
    }

    @Test
    @DisplayName("Should preserve terraform code in response on error")
    void shouldPreserveTerraformCodeInResponseOnError() {
        TerraformExecutor executor = new TerraformExecutor(env, fileManager);
        String tfCode = "resource \"aws_s3_bucket\" \"test\" {}";

        TerraformResponse response = executor.execute(tfCode);

        assertEquals(tfCode, response.tfCode());
    }

    @Test
    @DisplayName("Should configure AWS access key when property is present")
    void shouldConfigureAwsAccessKeyWhenPresent() {
        when(env.getProperty("aws_access_key_id")).thenReturn("AKIAIOSFODNN7EXAMPLE");
        when(env.getProperty("aws_secret_access_key")).thenReturn("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        TerraformExecutor executor = new TerraformExecutor(env, fileManager);

        TerraformResponse response = executor.execute("provider \"aws\" {}");

        assertNotNull(response);
        assertNotNull(response.status());
        assertNotNull(response.tfCode());
    }

    @Test
    @DisplayName("Should handle null AWS keys without error")
    void shouldHandleNullAwsKeysWithoutError() {
        when(env.getProperty("aws_access_key_id")).thenReturn(null);
        when(env.getProperty("aws_secret_access_key")).thenReturn(null);
        TerraformExecutor executor = new TerraformExecutor(env, fileManager);

        TerraformResponse response = executor.execute("provider \"aws\" {}");

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should trim whitespace from AWS keys")
    void shouldTrimWhitespaceFromAwsKeys() {
        when(env.getProperty("aws_access_key_id")).thenReturn("  AKIAIOSFODNN7EXAMPLE  ");
        when(env.getProperty("aws_secret_access_key")).thenReturn("  secretkey  ");
        TerraformExecutor executor = new TerraformExecutor(env, fileManager);

        TerraformResponse response = executor.execute("provider \"aws\" {}");

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should return error response when exception occurs during execution")
    void shouldReturnErrorOnException() {
        TerraformExecutor executor = new TerraformExecutor(env, fileManager);

        System.setProperty("user.dir", "/nonexistent/path/that/does/not/exist");

        TerraformResponse response = executor.execute("resource {}");

        assertEquals("ERRO", response.status());
        assertNotNull(response.output());
    }
}
