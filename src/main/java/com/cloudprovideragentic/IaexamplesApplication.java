package com.cloudprovideragentic;

import com.cloudprovideragentic.fuctions.terraform.TerraformCodeHolder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.Objects;
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

		TerraformChatRunner(ChatClient.Builder builder, TerraformCodeHolder codeHolder,
				JdbcChatMemoryRepository chatMemoryRepository) {
			this.codeHolder = codeHolder;
			MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
					.chatMemoryRepository(Objects.requireNonNull(chatMemoryRepository))
					.maxMessages(10)
					.build();
			this.chatClient = builder
					.defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
					.defaultSystem("""
						Voce e um Agente de Automacao Cloud com fluxo de aprovacao.""")
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
