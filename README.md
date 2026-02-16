# Cloud Provider Agentic

Agente de automação cloud que provisiona infraestrutura AWS via Terraform usando inteligência artificial. O agente utiliza Spring AI com Amazon Bedrock (modelo Nova Pro) para interpretar solicitações em linguagem natural e gerar/executar código Terraform automaticamente.

## Recursos suportados

- **S3 Bucket** - Armazenamento de objetos (nome, versioning, encryption)
- **SQS Queue** - Fila de mensagens (nome, fifo, visibilityTimeout)
- **ECS Cluster** - Cluster de containers (clusterName, serviceName, taskName)

## Pré-requisitos

- Java 21+
- Maven 3.8+
- Terraform CLI instalado e disponível no PATH
- Conta AWS com permissões para criar recursos S3, SQS e ECS
- Acesso ao Amazon Bedrock com o modelo `amazon.nova-pro-v1:0` habilitado

## Configuração das variáveis AWS

O projeto utiliza variáveis de ambiente para autenticação. **Nunca coloque credenciais diretamente no código.**

### 1. Exportar variáveis de ambiente

```bash
export AWS_ACCESS_KEY_ID=sua-access-key
export AWS_SECRET_ACCESS_KEY=sua-secret-key
```

### 2. Ou criar um arquivo `.env` na raiz do projeto

```
AWS_ACCESS_KEY_ID=sua-access-key
AWS_SECRET_ACCESS_KEY=sua-secret-key
```

> O arquivo `.env` já está no `.gitignore` e não será versionado.

### Configuração do Bedrock

O projeto está configurado para usar a região `us-east-1` e o modelo `amazon.nova-pro-v1:0`. Para alterar, edite o arquivo `src/main/resources/application.properties`:

```properties
spring.ai.bedrock.aws.region=us-east-1
spring.ai.bedrock.converse.chat.options.model=amazon.nova-pro-v1:0
spring.ai.bedrock.converse.chat.options.temperature=0.8
spring.ai.bedrock.converse.chat.options.max-tokens=1000
```

Certifique-se de que o modelo esteja habilitado na sua conta AWS em **Amazon Bedrock > Model access**.

## Executar os testes

```bash
./mvnw test
```

Ou com Maven instalado globalmente:

```bash
mvn test
```

## Executar o agente

```bash
./mvnw spring-boot:run
```

Ou com Maven instalado globalmente:

```bash
mvn spring-boot:run
```

Ao iniciar, o agente exibe um prompt interativo no terminal:

```
===========================================
  Agente de Automação Cloud - Terraform
  Digite 'sair' para encerrar.
===========================================

Você>
```

### Exemplos de uso

```
Você> O que você consegue fazer?
Agente> Eu consigo provisionar os seguintes recursos AWS: S3, SQS e ECS...

Você> Crie um bucket S3 chamado meu-bucket-dados
Agente> [Apresenta o plano Terraform gerado]
        Deseja que eu execute este plano? (sim/não)

Você> sim
Agente> [Executa terraform init, plan e apply]
```

Para encerrar, digite `sair` ou `exit`.

## Estrutura do projeto

```
src/main/java/com/cloudprovideragentic/
├── IaexamplesApplication.java            # Ponto de entrada e chat interativo
└── fuctions/terraform/
    ├── TerraformTool.java                # Definição das tools do agente (planejarInfra, executarInfra)
    ├── InfraPlanService.java             # Geração do plano de infraestrutura via LLM
    ├── TerraformGenerator.java           # Geração de código Terraform a partir do plano
    ├── TerraformExecutor.java            # Execução de terraform init/plan/apply
    ├── TerraformCodeHolder.java          # Armazenamento temporário do código gerado
    ├── utils/
    │   └── TerraformFileManager.java     # Gerenciamento de arquivos .tf no disco
    └── model/
        ├── specs/                        # Especificações dos recursos (S3, SQS, ECS)
        ├── plans/                        # Modelos de plano e resposta
        └── terraform/                    # Request/Response do Terraform
```
