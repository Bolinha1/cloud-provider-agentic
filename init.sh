#!/bin/bash
set -e

echo "========================================="
echo "  Cloud Provider Agentic - Init"
echo "========================================="
echo ""
echo "  Banco de dados: PostgreSQL (chatmemory)"
echo "  Tabela:         spring_ai_chat_memory"
echo "  Inicialização:  postgres/init.sql"
echo ""

# 0. Verificar se .env existe
if [ ! -f .env ]; then
  echo "Erro: arquivo .env não encontrado!"
  echo "Crie o arquivo .env com base no .env.example:"
  echo ""
  echo "  cp .env.example .env"
  echo ""
  echo "E preencha suas credenciais AWS."
  exit 1
fi

# 1. Gerar o binário JAR
echo "[1/4] Gerando JAR com Maven..."
./mvnw clean package -DskipTests -B

# 2. Verificar se o JAR foi gerado
JAR_FILE=$(ls target/*.jar 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
  echo "Erro: JAR não encontrado em target/"
  exit 1
fi
echo "[2/4] JAR gerado: $JAR_FILE"

# 3. Limpar conteúdo da pasta infra/
echo "[3/4] Limpando pasta infra/..."
rm -f infra/*.tf infra/*.tfplan infra/.terraform.lock.hcl
rm -rf infra/.terraform infra/tfplan

# 4. Build das imagens
echo "[4/5] Fazendo build das imagens Docker..."
docker compose build

# 5. Subir o Postgres e aguardar healthcheck, depois iniciar o app
echo "[5/5] Subindo PostgreSQL e iniciando o agente..."
echo ""
echo "========================================="
echo "  Iniciando o agente interativo..."
echo "  Parar:   docker compose down"
echo "========================================="
echo ""
docker compose run --rm app
