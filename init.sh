#!/bin/bash
set -e

echo "========================================="
echo "  Cloud Provider Agentic - Init"
echo "========================================="

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
echo "[1/3] Gerando JAR com Maven..."
./mvnw clean package -DskipTests -B

# 2. Verificar se o JAR foi gerado
JAR_FILE=$(ls target/*.jar 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
  echo "Erro: JAR não encontrado em target/"
  exit 1
fi
echo "[2/3] JAR gerado: $JAR_FILE"

# 3. Build da imagem e iniciar o app
echo "[3/3] Subindo container Docker..."
docker compose build

echo ""
echo "========================================="
echo "  Iniciando o agente interativo..."
echo "  Parar:  docker compose down"
echo "========================================="
echo ""
docker compose run --rm app
