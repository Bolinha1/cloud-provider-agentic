# Cloud Provider Agentic

Cloud automation agent that provisions AWS infrastructure via Terraform using artificial intelligence. The agent uses Spring AI with Amazon Bedrock (Nova Pro model) to interpret natural language requests and automatically generate/execute Terraform code.

## Supported Resources

- **S3 Bucket** - Object storage (name, versioning, encryption)
- **SQS Queue** - Message queue (name, fifo, visibilityTimeout)
- **ECS Cluster** - Container cluster (clusterName, serviceName, taskName)

## Prerequisites

### Running with Docker (recommended)

- Docker and Docker Compose
- AWS account with permissions to create S3, SQS and ECS resources
- Access to Amazon Bedrock with the `amazon.nova-pro-v1:0` model enabled

### Running locally

- Java 21+
- Maven 3.8+
- Terraform CLI installed and available in PATH
- AWS account with permissions to create S3, SQS and ECS resources
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
3. Builds the Docker image (Java 21 + Terraform CLI)
4. Starts the container in interactive mode

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
Agente> Eu consigo provisionar os seguintes recursos AWS: S3, SQS e ECS...

Você> Crie um bucket S3 chamado meu-bucket-dados
Agente> [Shows the generated Terraform plan]
        Deseja que eu execute este plano? (sim/não)

Você> sim
Agente> [Runs terraform init, plan and apply]
```

To exit, type `sair` or `exit`.

## Project Structure

```
src/main/java/com/cloudprovideragentic/
├── IaexamplesApplication.java            # Entry point and interactive chat
└── fuctions/terraform/
    ├── TerraformTool.java                # Agent tool definitions (planejarInfra, executarInfra)
    ├── InfraPlanService.java             # Infrastructure plan generation via LLM
    ├── TerraformGenerator.java           # Terraform code generation from plan
    ├── TerraformExecutor.java            # Terraform init/plan/apply execution
    ├── TerraformCodeHolder.java          # Temporary storage for generated code
    ├── utils/
    │   └── TerraformFileManager.java     # .tf file management on disk
    └── model/
        ├── specs/                        # Resource specifications (S3, SQS, ECS)
        ├── plans/                        # Plan and response models
        └── terraform/                    # Terraform request/response
```

📚 Read more: See more information visit this article on Medium: [Spring AI e Tool Callings na construção de um agente provedor de infra](https://medium.com/@eduardo.borsato.oli/spring-ai-e-tool-callings-na-constru%C3%A7%C3%A3o-de-um-agente-provedor-de-infra-3acb87bffa82)