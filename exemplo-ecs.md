# Exemplo de Criação de Cluster ECS com Serviço

## Especificação JSON

```json
{
  "region": "us-east-1",
  "resources": [
    {
      "@type": "EcsClusterSpec",
      "clusterName": "meu-cluster",
      "serviceName": "meu-servico",
      "taskName": "minha-task",
      "cpu": 256,
      "memory": 512
    }
  ]
}
```

## Recursos Criados

A partir da especificação acima, o sistema agora cria automaticamente:

### 1. **Cluster ECS** (`meu-cluster`)
   - O cluster principal onde os serviços serão executados

### 2. **Task Definition** (`minha-task`)
   - Definição da tarefa Fargate
   - CPU: 256 unidades
   - Memória: 512 MB
   - Container: nginx:latest (exemplo)
   - Porta: 80

### 3. **Serviço ECS** (`meu-servico`)
   - Executa dentro do cluster `meu-cluster`
   - Usa a task definition `minha-task`
   - Desired count: 1 instância
   - Launch type: Fargate

### 4. **Infraestrutura de Rede**
   - **VPC**: Rede isolada (10.0.0.0/16)
   - **Subnet**: Sub-rede pública (10.0.1.0/24)
   - **Internet Gateway**: Acesso à internet
   - **Route Table**: Roteamento do tráfego
   - **Security Group**: Permite tráfego HTTP na porta 80

### 5. **IAM Roles**
   - **Execution Role**: Para o ECS executar as tasks
   - **Policy Attachment**: Permissões necessárias para ECS

## Código Terraform Gerado

O sistema gera automaticamente todo o código Terraform necessário, incluindo:

```hcl
resource "aws_ecs_cluster" "meu-cluster" {
  name = "meu-cluster"
}

resource "aws_ecs_task_definition" "minha-task" {
  family                   = "minha-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.minha-task_execution_role.arn
  # ... container definitions
}

resource "aws_ecs_service" "meu-servico" {
  name            = "meu-servico"
  cluster         = aws_ecs_cluster.meu-cluster.id
  task_definition = aws_ecs_task_definition.minha-task.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  # ... network configuration
}

# + VPC, Subnet, Security Group, IAM Roles, etc.
```

## Fluxo de Uso

1. Envie a especificação JSON ao agente
2. O agente gera o código Terraform completo
3. Você aprova o plano
4. O Terraform cria toda a infraestrutura
5. O serviço ECS fica disponível e rodando no cluster

## Observações

- O container padrão é `nginx:latest` - você pode personalizar modificando o generator
- A configuração usa Fargate (serverless) - não precisa gerenciar instâncias EC2
- O serviço fica em uma subnet pública com IP público atribuído
- O Security Group permite tráfego HTTP de qualquer origem (0.0.0.0/0)