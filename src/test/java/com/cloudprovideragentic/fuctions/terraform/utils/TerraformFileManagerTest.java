package com.cloudprovideragentic.fuctions.terraform.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TerraformFileManagerTest {

    private TerraformFileManager fileManager;

    @TempDir
    Path tempDir;

    private String originalUserDir;

    @BeforeEach
    void setUp() {
        fileManager = new TerraformFileManager();
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.dir", originalUserDir);
    }

    @Test
    @DisplayName("Should create infra directory")
    void shouldCreateInfraDirectory() throws Exception {
        String tfCode = "resource \"aws_s3_bucket\" \"test\" {}";

        Path result = fileManager.prepareInfraDirectory(tfCode);

        assertTrue(Files.exists(result));
        assertTrue(Files.isDirectory(result));
        assertEquals(tempDir.resolve("infra"), result);
    }

    @Test
    @DisplayName("Should write main.tf file in infra directory")
    void shouldWriteMainTfFile() throws Exception {
        String tfCode = "resource \"aws_s3_bucket\" \"test\" {}";

        Path infraDir = fileManager.prepareInfraDirectory(tfCode);
        Path mainTf = infraDir.resolve("main.tf");

        assertTrue(Files.exists(mainTf));
        assertTrue(Files.isRegularFile(mainTf));
    }

    @Test
    @DisplayName("Should write correct Terraform code content to file")
    void shouldWriteCorrectTerraformContent() throws Exception {
        String tfCode = "resource \"aws_sqs_queue\" \"test\" { name = \"test-queue\" }";

        Path infraDir = fileManager.prepareInfraDirectory(tfCode);
        String content = Files.readString(infraDir.resolve("main.tf"));

        assertEquals(tfCode, content);
    }

    @Test
    @DisplayName("Should return infra directory path")
    void shouldReturnInfraDirectoryPath() throws Exception {
        String tfCode = "provider \"aws\" {}";

        Path result = fileManager.prepareInfraDirectory(tfCode);

        assertEquals(tempDir.resolve("infra"), result);
        assertTrue(result.toString().endsWith("infra"));
    }

    @Test
    @DisplayName("Should handle multiple calls and append resources")
    void shouldHandleMultipleCalls() throws Exception {
        String tfCode1 = "resource \"aws_s3_bucket\" \"bucket1\" {}";
        String tfCode2 = "resource \"aws_s3_bucket\" \"bucket2\" {}";

        fileManager.prepareInfraDirectory(tfCode1);
        Path infraDir = fileManager.prepareInfraDirectory(tfCode2);

        String content = Files.readString(infraDir.resolve("main.tf"));

        // Deve conter ambos os recursos (append)
        assertTrue(content.contains("resource \"aws_s3_bucket\" \"bucket1\""));
        assertTrue(content.contains("resource \"aws_s3_bucket\" \"bucket2\""));
    }

    @Test
    @DisplayName("Should create parent directory if it doesn't exist")
    void shouldCreateParentDirectoryIfNotExists() throws Exception {
        String tfCode = "resource \"aws_s3_bucket\" \"test\" {}";

        assertFalse(Files.exists(tempDir.resolve("infra")));

        fileManager.prepareInfraDirectory(tfCode);

        assertTrue(Files.exists(tempDir.resolve("infra")));
        assertTrue(Files.exists(tempDir.resolve("infra/main.tf")));
    }

    @Test
    @DisplayName("Should append resources when main.tf already exists")
    void shouldAppendResourcesWhenFileExists() throws Exception {
        String firstCode = """
                terraform {
                  required_providers {
                    aws = { source = "hashicorp/aws" }
                  }
                }

                provider "aws" {
                  region = "us-east-1"
                }

                resource "aws_s3_bucket" "bucket1" {
                  bucket = "bucket1"
                }
                """;

        String secondCode = """
                terraform {
                  required_providers {
                    aws = { source = "hashicorp/aws" }
                  }
                }

                provider "aws" {
                  region = "us-east-1"
                }

                resource "aws_sqs_queue" "queue1" {
                  name = "queue1"
                }
                """;

        // Primeira chamada - cria arquivo
        Path infraDir = fileManager.prepareInfraDirectory(firstCode);
        String firstContent = Files.readString(infraDir.resolve("main.tf"));

        // Verifica que contém o código completo
        assertTrue(firstContent.contains("terraform {"));
        assertTrue(firstContent.contains("provider \"aws\""));
        assertTrue(firstContent.contains("resource \"aws_s3_bucket\""));

        // Segunda chamada - deve fazer append apenas do recurso
        fileManager.prepareInfraDirectory(secondCode);
        String finalContent = Files.readString(infraDir.resolve("main.tf"));

        // Verifica que contém ambos os recursos
        assertTrue(finalContent.contains("resource \"aws_s3_bucket\" \"bucket1\""));
        assertTrue(finalContent.contains("resource \"aws_sqs_queue\" \"queue1\""));

        // Verifica que terraform{} e provider{} aparecem apenas uma vez
        int terraformCount = countOccurrences(finalContent, "terraform {");
        int providerCount = countOccurrences(finalContent, "provider \"aws\"");

        assertEquals(1, terraformCount, "terraform block should appear only once");
        assertEquals(1, providerCount, "provider block should appear only once");
    }

    @Test
    @DisplayName("Should extract only resource blocks when appending")
    void shouldExtractOnlyResourceBlocksWhenAppending() throws Exception {
        String firstCode = "provider \"aws\" {}\nresource \"aws_s3_bucket\" \"b1\" { bucket = \"b1\" }";
        String secondCode = "provider \"aws\" {}\nresource \"aws_sqs_queue\" \"q1\" { name = \"q1\" }";

        fileManager.prepareInfraDirectory(firstCode);
        fileManager.prepareInfraDirectory(secondCode);

        Path infraDir = tempDir.resolve("infra");
        String content = Files.readString(infraDir.resolve("main.tf"));

        // Deve conter ambos os recursos
        assertTrue(content.contains("resource \"aws_s3_bucket\" \"b1\""));
        assertTrue(content.contains("resource \"aws_sqs_queue\" \"q1\""));

        // Provider deve aparecer apenas uma vez (do primeiro código)
        assertEquals(1, countOccurrences(content, "provider \"aws\""));
    }

    @Test
    @DisplayName("Should handle multiple resources in a single plan")
    void shouldHandleMultipleResourcesInSinglePlan() throws Exception {
        String codeWithMultipleResources = """
                provider "aws" { region = "us-east-1" }

                resource "aws_s3_bucket" "bucket1" {
                  bucket = "bucket1"
                }

                resource "aws_s3_bucket" "bucket2" {
                  bucket = "bucket2"
                }
                """;

        Path infraDir = fileManager.prepareInfraDirectory(codeWithMultipleResources);
        String content = Files.readString(infraDir.resolve("main.tf"));

        assertTrue(content.contains("resource \"aws_s3_bucket\" \"bucket1\""));
        assertTrue(content.contains("resource \"aws_s3_bucket\" \"bucket2\""));
        assertEquals(1, countOccurrences(content, "provider \"aws\""));
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
