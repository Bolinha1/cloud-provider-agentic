package com.iaexample.iaexamples;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@SpringBootApplication
public class IaexamplesApplication {

	public static void main(String[] args) {
		SpringApplication.run(IaexamplesApplication.class, args);
	}

	@Component
	static class TerraformChatRunner implements CommandLineRunner {

		private final ChatClient chatClient;

		TerraformChatRunner(ChatClient.Builder builder) {
			this.chatClient = builder
					.defaultSystem("""
						Voce e um Agente de Automacao Cloud com fluxo de aprovacao.

						SERVICOS SUPORTADOS:
						Voce consegue provisionar APENAS os seguintes recursos AWS:
						1. S3 Bucket - Armazenamento de objetos (opcoes: nome, versioning, encryption)
						2. SQS Queue - Fila de mensagens (opcoes: nome, fifo, visibilityTimeout)
						3. ECS Cluster - Cluster de containers (opcoes: clusterName, serviceName, taskName)

						Quando o usuario perguntar o que voce consegue fazer, liste esses 3 servicos de forma clara.
						Se o usuario pedir um recurso que NAO esta na lista acima, informe educadamente que
						no momento voce so suporta S3, SQS e ECS.

						FLUXO OBRIGATORIO (duas etapas):

						ETAPA 1 - PLANEJAMENTO:
						Quando o usuario pedir para criar recursos AWS:
						1. Identifique o nome do recurso e a 'region'.
						2. Se a 'region' nao for informada, use 'us-east-1' como padrao.
						3. Chame a tool 'planejarInfra' passando:
						   {"userPrompt": "Criar [recurso] com nome [nome] na regiao [region]"}
						4. Apresente ao usuario o resultado com:
						   - O plano DevOps (recursos que serao criados)
						   - O codigo Terraform gerado
						5. Pergunte: "Deseja que eu execute este plano? (sim/nao)"

						ETAPA 2 - EXECUCAO:
						- Se o usuario confirmar (sim, confirmar, ok, pode executar, etc):
						  Chame a tool 'executarInfra' com {"confirmar": true}
						- Se o usuario negar (nao, cancelar, etc):
						  Chame a tool 'executarInfra' com {"confirmar": false}

						REGRAS:
						- NUNCA execute 'executarInfra' sem antes ter chamado 'planejarInfra'.
						- NUNCA execute 'executarInfra' sem confirmacao explicita do usuario.
						- NAO peca e NAO manipule chaves de acesso AWS.
						- NUNCA inclua tags <thinking> ou blocos de raciocinio na resposta.
						- Responda sempre de forma direta, clara e em portugues.
						""")
					.defaultToolNames("planejarInfra", "executarInfra")
					.build();
		}

		@Override
		public void run(String... args) {
			Scanner scanner = new Scanner(System.in);

			System.out.println("===========================================");
			System.out.println("  Agente de Automação Cloud - Terraform");
			System.out.println("  Digite 'sair' para encerrar.");
			System.out.println("===========================================");

			while (true) {
				System.out.print("\nVocê> ");
				String input = scanner.nextLine().trim();

				if (input.equalsIgnoreCase("sair") || input.equalsIgnoreCase("exit")) {
					System.out.println("Encerrando. Até logo!");
					break;
				}

				if (input.isEmpty()) {
					continue;
				}

				try {
					System.out.println("\nProcessando...");
					String response = chatClient.prompt()
							.user(input)
							.call()
							.content();
					String cleanResponse = response.replaceAll("(?s)<thinking>.*?</thinking>\\s*", "").trim();
					System.out.println("\nAgente> " + cleanResponse);
				} catch (Exception e) {
					System.out.println("\nErro: " + e.getMessage());
				}
			}

			scanner.close();
		}
	}
}
