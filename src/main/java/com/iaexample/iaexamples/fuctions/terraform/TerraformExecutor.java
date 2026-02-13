package com.iaexample.iaexamples.fuctions.terraform;

import com.iaexample.iaexamples.fuctions.terraform.model.TerraformResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class TerraformExecutor {

    private final Environment env;

    public TerraformExecutor(Environment env) {
        this.env = env;
    }

    public TerraformResponse execute(String tfCode) {
        try {
            // 1. Definir diretório de trabalho
            Path workDir = Path.of(System.getProperty("user.dir"));

            // 2. Salvar arquivo .tf
            Path tfPath = workDir.resolve("main.tf");
            Files.writeString(tfPath, tfCode);


            // 3. Configurar variáveis de ambiente AWS
            ProcessBuilder pb = new ProcessBuilder();
            Map<String, String> envVars = new ProcessBuilder().environment();
            envVars.put("AWS_ACCESS_KEY_ID", env.getProperty("aws_access_key_id"));
            envVars.put("AWS_SECRET_ACCESS_KEY", env.getProperty("aws_secret_access_key"));
            envVars.put("AWS_SECRET_ACCESS_KEY", env.getProperty("aws_secret_access_key"));
            envVars.put("AWS_SESSION_TOKEN", "179424");

            // 3. Executar terraform init
            ProcessBuilder initPb = new ProcessBuilder("terraform", "init", "-no-color");
            initPb.directory(workDir.toFile());
            Process initProcess = initPb.start();
            int initCode = initProcess.waitFor();

            if (initCode != 0) {
                String error = readProcessOutput(initProcess.getErrorStream());
                return new TerraformResponse("ERRO", tfCode, error);
            }

            // 4. Executar terraform plan
            ProcessBuilder planPb = new ProcessBuilder("terraform", "plan", "-no-color", "-out=tfplan");
            planPb.directory(workDir.toFile());
            planPb.environment().putAll(envVars);
            Process planProcess = planPb.start();
            String planOutput = readProcessOutput(planProcess.getInputStream());
            int planCode = planProcess.waitFor();

            if (planCode != 0) {
                String error = readProcessOutput(planProcess.getErrorStream());
                return new TerraformResponse("ERRO", tfCode, error);
            }

            // 5. Executar terraform apply
            ProcessBuilder applyPb = new ProcessBuilder("terraform", "apply", "-no-color", "tfplan");
            applyPb.directory(workDir.toFile());
            Process applyProcess = applyPb.start();
            String applyOutput = readProcessOutput(applyProcess.getInputStream());
            int applyCode = applyProcess.waitFor();

            if (applyCode != 0) {
                String error = readProcessOutput(applyProcess.getErrorStream());
                return new TerraformResponse("ERRO", tfCode, error);
            }

            return new TerraformResponse("SUCESSO", tfCode, applyOutput);

        } catch (Exception e) {
            return new TerraformResponse("ERRO", tfCode, e.getMessage());
        }
    }

    private String readProcessOutput(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}