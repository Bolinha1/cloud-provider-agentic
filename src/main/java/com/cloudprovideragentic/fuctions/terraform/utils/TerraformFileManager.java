package com.cloudprovideragentic.fuctions.terraform.utils;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Component
public class TerraformFileManager {

    private static final String INFRA_DIR = "infra";
    private static final String TF_FILENAME = "main.tf";

    /**
     * Prepares the infrastructure directory and writes the Terraform code.
     * If main.tf already exists, appends only the resource blocks from the new code.
     *
     * @param tfCode The Terraform code to write
     * @return Path to the infra directory where execution should occur
     * @throws IOException if directory creation or file writing fails
     */
    public Path prepareInfraDirectory(String tfCode) throws IOException {
        Path baseDir = Path.of(System.getProperty("user.dir"));
        Path infraDir = baseDir.resolve(INFRA_DIR);

        Files.createDirectories(infraDir);

        Path tfFile = infraDir.resolve(TF_FILENAME);

        if (Files.exists(tfFile)) {
            // Arquivo existe - fazer append apenas dos recursos
            String resourcesOnly = extractResourceBlocks(tfCode);
            if (!resourcesOnly.isEmpty()) {
                String separator = "\n\n# ========================================\n" +
                                 "# Recursos adicionados\n" +
                                 "# ========================================\n\n";
                Files.writeString(tfFile, separator + resourcesOnly, StandardOpenOption.APPEND);
            }
        } else {
            // Arquivo não existe - criar com código completo
            Files.writeString(tfFile, tfCode);
        }

        return infraDir;
    }

    /**
     * Extracts only resource blocks from Terraform code.
     * Ignores terraform{}, provider{}, and other configuration blocks.
     */
    private String extractResourceBlocks(String tfCode) {
        StringBuilder resources = new StringBuilder();
        String[] lines = tfCode.split("\n");
        boolean inResourceBlock = false;
        int braceCount = 0;
        StringBuilder currentBlock = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();

            // Detectar início de bloco resource
            if (trimmed.startsWith("resource ")) {
                inResourceBlock = true;
                braceCount = 0;
                currentBlock = new StringBuilder();
            }

            if (inResourceBlock) {
                currentBlock.append(line).append("\n");

                // Contar chaves para detectar fim do bloco
                for (char c : line.toCharArray()) {
                    if (c == '{') braceCount++;
                    if (c == '}') braceCount--;
                }

                // Fim do bloco resource
                if (braceCount == 0 && trimmed.endsWith("}")) {
                    resources.append(currentBlock).append("\n");
                    inResourceBlock = false;
                }
            }
        }

        return resources.toString();
    }
}
