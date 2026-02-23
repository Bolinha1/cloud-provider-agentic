# Cloud Provider Agentic

Cloud automation agent that provisions AWS infrastructure via Terraform using artificial intelligence. The agent uses Spring AI with Amazon Bedrock (Nova Pro model) to interpret natural language requests and automatically generate/execute Terraform code.

## Supported Resources

The agent supports **any AWS resource** through LLM-based code generation. The following are explicitly covered in the system prompt with best practices:

- **S3 Bucket** - Object storage (versioning, encryption, lifecycle policies)
- **SQS Queue** - Message queue (FIFO, Dead Letter Queue, visibility timeout)
- **ECS Cluster** - Container cluster with Fargate (service, task definition)
- **SNS** - Notification service (topics, subscriptions)

## Prerequisites

### Running with Docker (recommended)

- Docker and Docker Compose
- AWS account with permissions to create the desired resources
- Access to Amazon Bedrock with the `amazon.nova-pro-v1:0` model enabled

### Running locally

- Java 21+
- Maven 3.8+
- Terraform CLI installed and available in PATH
- AWS account with permissions to create the desired resources
- Access to Amazon Bedrock with the `amazon.nova-pro-v1:0` model enabled

## AWS Configuration

The project uses environment variables for authentication. **Never put credentials directly in the code.**

### 1. Export environment variables

```bash
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
```

### 2. Or create a `.env` file in the project root

```bash
cp .env.example .env
```

Edit the `.env` file with your credentials:

```
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_REGION=us-east-1
```

> The `.env` file is already in `.gitignore` and will not be versioned.

### Bedrock Configuration

The project is configured to use the `us-east-1` region and the `amazon.nova-pro-v1:0` model. To change these settings, edit `src/main/resources/application.properties`:

```properties
spring.ai.bedrock.aws.region=us-east-1
spring.ai.bedrock.converse.chat.options.model=amazon.nova-pro-v1:0
spring.ai.bedrock.converse.chat.options.temperature=0.8
spring.ai.bedrock.converse.chat.options.max-tokens=1000
```

Make sure the model is enabled in your AWS account under **Amazon Bedrock > Model access**.

## Running with Docker

The `init.sh` script automates the entire process: compiles the JAR, builds the Docker image and starts the interactive agent.

```bash
sh init.sh
```

The script performs the following steps:
1. Checks if the `.env` file exists
2. Builds the JAR with Maven (`./mvnw clean package -DskipTests`)
3. Cleans previous Terraform state files from `infra/`
4. Builds the Docker image (Java 21 + Terraform 1.12.1)
5. Starts the container in interactive mode

To stop the container:

```bash
docker compose down
```

## Running locally (without Docker)

### Tests

```bash
./mvnw test
```

### Agent

```bash
./mvnw spring-boot:run
```

On startup, the agent displays an interactive prompt in the terminal:

```
===========================================
  Agente de Automação Cloud - Terraform
  Digite 'sair' para encerrar.
===========================================

Você>
```

### Usage examples

```
Você> O que você consegue fazer?
Agente> Eu consigo provisionar os seguintes recursos AWS: S3, SQS, ECS...

Você> Crie um bucket S3 chamado meu-bucket-dados
Agente> [Shows the generated Terraform plan]
        Deseja que eu execute este plano? (sim/não)

Você> sim
Agente> [Runs terraform init, plan and apply]
```

To exit, type `sair` or `exit`.

## How It Works

The agent follows a two-stage approval workflow:

1. **STAGE 1 — Plan (`planejarInfra` tool):** The LLM generates Terraform HCL code from the user's natural language request. The plan is shown for review before anything is applied.
2. **STAGE 2 — Execute (`executarInfra` tool):** Upon user confirmation, runs `terraform init` → `terraform plan` → `terraform apply` against the generated code.

The `infra/` directory is persisted (mounted as a Docker volume) so that Terraform state is maintained across executions, enabling incremental resource additions.

## Project Structure

```
src/main/java/com/cloudprovideragentic/
├── IaexamplesApplication.java              # Entry point and interactive chat
└── fuctions/terraform/
    ├── TerraformTool.java                  # Agent tool definitions (planejarInfra, executarInfra)
    ├── TerraformCodeGeneratorService.java  # Terraform code generation via LLM
    ├── TerraformExecutor.java              # Terraform init/plan/apply execution
    ├── TerraformCodeHolder.java            # Temporary storage for generated code
    ├── utils/
    │   └── TerraformFileManager.java       # .tf file management on disk
    └── model/
        ├── plans/                          # PlanResponse, TerraformPlanResult
        └── terraform/                      # TerraformRequest, ExecuteRequest, TerraformResponse
```

📚 Read more: See more information visit this article on Medium: [Spring AI e Tool Callings na construção de um agente provedor de infra](https://medium.com/@eduardo.borsato.oli/spring-ai-e-tool-callings-na-constru%C3%A7%C3%A3o-de-um-agente-provedor-de-infra-3acb87bffa82)
