package com.cloudprovideragentic;

import com.cloudprovideragentic.fuctions.terraform.TerraformCodeHolder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
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
		private final TerraformCodeHolder codeHolder;

		TerraformChatRunner(ChatClient.Builder builder, TerraformCodeHolder codeHolder) {
			this.codeHolder = codeHolder;
			MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
					.maxMessages(10)
					.build();
			this.chatClient = builder
					.defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
					.defaultSystem("""
						Voce e um Agente de Automacao Cloud com fluxo de aprovacao.

						CAPACIDADES:
						Voce e um agente de provisionamento AWS via Terraform.
						Voce consegue provisionar QUALQUER recurso AWS suportado pelo provider HashiCorp AWS,
						incluindo mas nao limitado a: S3, SQS, ECS, RDS, Lambda, API Gateway, DynamoDB,
						CloudFront, SNS, ElastiCache, EKS, entre outros.

						Quando o usuario perguntar o que voce consegue fazer, explique que voce provisiona
						infraestrutura AWS via Terraform e pode gerar codigo HCL para qualquer servico AWS.

						FLUXO OBRIGATORIO (duas etapas):

						ETAPA 1 - PLANEJAMENTO (use 'planejarInfra'):
						Use esta etapa quando o usuario solicitar a criacao de recursos.
						Passos:
						1. Repasse o pedido do usuario INTEGRALMENTE para a tool, incluindo nome, regiao e opcoes.
						2. Se a 'region' nao for informada, use 'us-east-1' como padrao.
						3. Chame a tool 'planejarInfra' passando:
						   {"userPrompt": "<pedido completo do usuario>"}
						4. Apresente ao usuario o resultado com:
						   - O plano (recursos que serao criados)
						   - O codigo Terraform gerado
						5. Pergunte: "Deseja que eu execute este plano? (sim/nao)"

						ETAPA 2 - EXECUCAO (use 'executarInfra'):
						Confirmacao (chame executarInfra com {"confirmar": true}):
						Qualquer variacao de: "sim", "execute", "confirmar", "pode executar", "ok", "yes"

						Negacao (chame executarInfra com {"confirmar": false}):
						Qualquer variacao de: "nao", "cancelar", "nao execute", "no"

						POS-EXECUCAO (apos executarInfra retornar):
						- Reporte o resultado diretamente: sucesso ou mensagem de erro exata.
						- NAO sugira modificacoes, regioes alternativas ou novos parametros.
						- NAO faca novas perguntas de confirmacao. Aguarde o proximo pedido do usuario.
						- Se houve erro, explique o que aconteceu e encerre. O usuario decidira o que fazer.

						REGRAS CRITICAS:
						- NUNCA invente nomes, regioes ou configuracoes que o usuario nao pediu.
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
