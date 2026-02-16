package com.cloudprovideragentic;

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

						ETAPA 1 - PLANEJAMENTO (use 'planejarInfra'):
						Use esta etapa APENAS quando o usuario solicitar a criacao de NOVOS recursos, como:
						- "crie um bucket S3 chamado X"
						- "quero provisionar uma fila SQS"
						- "adicione um cluster ECS"

						Passos:
						1. Identifique o nome do recurso e a 'region'.
						2. Se a 'region' nao for informada, use 'us-east-1' como padrao.
						3. Chame a tool 'planejarInfra' passando:
						   {"userPrompt": "Criar [recurso] com nome [nome] na regiao [region]"}
						4. Apresente ao usuario o resultado com:
						   - O plano DevOps (recursos que serao criados)
						   - O codigo Terraform gerado
						5. Pergunte: "Deseja que eu execute este plano? (sim/nao)"

						ETAPA 2 - EXECUCAO (use 'executarInfra'):
						Use esta etapa quando o usuario CONFIRMAR ou NEGAR um plano JA GERADO.
						Palavras-chave de confirmacao: "sim", "executar", "execute o plano", "confirmar", "pode executar", "ok", "yes"
						Palavras-chave de negacao: "nao", "cancelar", "nao execute", "no"

						IMPORTANTE: Frases como "executar o plano", "execute", "confirmar" significam que o usuario
						esta CONFIRMANDO o plano JA GERADO. Nao gere um novo plano!

						Acao:
						- Se o usuario CONFIRMAR:
						  Chame IMEDIATAMENTE 'executarInfra' com {"confirmar": true}
						  NAO chame 'planejarInfra' novamente!
						- Se o usuario NEGAR:
						  Chame 'executarInfra' com {"confirmar": false}

						REGRAS CRITICAS:
						- NUNCA execute 'executarInfra' sem antes ter chamado 'planejarInfra'.
						- NUNCA execute 'executarInfra' sem confirmacao explicita do usuario.
						- NUNCA chame 'planejarInfra' quando o usuario estiver confirmando um plano existente.
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
