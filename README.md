# sentimentAPI

> API REST para análise automática de sentimentos em feedbacks de clientes. Integração Java Spring Boot + ONNX Runtime para classificar avaliações em Positivo ou Negativo.

[![Status do Projeto](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)](https://github.com/Matheus-es/sentimentAPI)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![Último Commit](https://img.shields.io/github/last-commit/Matheus-es/sentimentAPI)](https://github.com/Matheus-es/sentimentAPI)

Sumário
- [Sobre](#sobre)
- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Pré-requisitos](#pré-requisitos)
- [Instalação](#instalação)
- [Configuração](#configuração)
- [Como rodar](#como-rodar)
- [Docker](#docker)
- [Contribuição](#contribuição)
- [Licença](#licença)


## Sobre
Solução completa para análise de sentimentos em textos através de comentários e feedback de clientes. O modelo desenvolvido pela equipe de Data Science foi integrado a uma API REST, permitindo que outras aplicações consumam automaticamente a predição.

Classificação binária: POSITIVO / NEGATIVO

## Funcionalidades
- Endpoints REST para classificação de texto (inferencia com modelo ONNX).
- Documentação OpenAPI (SpringDoc).
- Suporte a banco em memória H2 (padrão) e PostgreSQL (opcional).
- Imagem Docker pronta para produção com otimizações de memória para ambientes com pouca RAM.

## Tecnologias
- Linguagem: Java 21
- Framework: Spring Boot 3.4.1
- ONNX Runtime Java (versão usada no pom.xml: 1.20.0)
- Build: Maven
- Banco (opcional): H2 (runtime) / PostgreSQL (runtime)
- Ferramentas: Docker, docker-compose
- OpenAPI UI: springdoc-openapi

## Pré-requisitos
- Java 21 (JDK)
- Maven (ou use o wrapper incluído: ./mvnw)
- Docker e Docker Compose (se quiser rodar em contêiner)
- Conexão com internet caso o Dockerfile precise baixar o modelo ONNX automaticamente

## Instalação (local)

⚠️ Aviso importante
Para rodar a API localmente, é obrigatório baixar também o conteúdo do repositório de Data Science, pois é nele que se encontra o modelo ONNX utilizado na inferência de sentimentos.
Sem esse modelo, a aplicação não inicializa corretamente o runtime do ONNX.

Clone o repositório:
```bash
git clone https://github.com/Matheus-es/sentimentAPI.git
cd sentimentAPI
```

Build da aplicação (gera o jar em target/):
```bash
# usando wrapper (recomendado)
./mvnw clean package -DskipTests

# ou com Maven instalado
mvn clean package -DskipTests
```

Executando o JAR:
```bash
java -jar target/*.jar
```

Também é possível rodar em desenvolvimento:
```bash
# com wrapper
./mvnw spring-boot:run

# ou
mvn spring-boot:run
```

No Windows: use `mvnw.cmd` em vez de `./mvnw`.

## Configuração
Existe suporte para H2 (padrão) e PostgreSQL. Para configurações sensíveis, crie um arquivo `.env` (ou configure variáveis de ambiente) com as chaves necessárias.

Variáveis de ambiente importantes:
- SENTIMENT_MODEL_PATH: caminho absoluto para o arquivo do modelo ONNX (o Dockerfile define `/app/models/sentiment_model.onnx`)
- SPRING_DATASOURCE_URL: URL do banco (se usar PostgreSQL)
- SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD

Exemplo mínimo com `.env` (ajuste conforme seu ambiente):
```
SENTIMENT_MODEL_PATH=/caminho/para/sentiment_model.onnx
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/sentimentdb
SPRING_DATASOURCE_USERNAME=usuario
SPRING_DATASOURCE_PASSWORD=senha
```

Observação: se não fornecer `SENTIMENT_MODEL_PATH`, a aplicação pode falhar ao inicializar o runtime ONNX — confira o Dockerfile e a lógica da aplicação para comportamento padrão.

## Como rodar (API)
- Porta padrão: 8080 (conforme Dockerfile e configuração Spring Boot)
- Documentação OpenAPI (Swagger UI): /swagger-ui.html ou /swagger-ui/index.html (SpringDoc). Também disponível: /v3/api-docs


## Docker
O repositório contém um Dockerfile otimizado em multi-stage que:
- usa Maven para construir o JAR,
- baixa o modelo ONNX do repositório SentimentONE durante o build,
- copia o modelo para /app/models/sentiment_model.onnx,
- define `SENTIMENT_MODEL_PATH=/app/models/sentiment_model.onnx`,
- configura Java para uso em ambientes com pouca memória.

Build e run:
```bash
# Build
docker build -t sentiment-api .

# Rodar
docker run -p 8080:8080 --env SENTIMENT_MODEL_PATH=/app/models/sentiment_model.onnx sentiment-api
```

Usando docker-compose (há um arquivo docker-compose.yml no repositório). Exemplo:
```bash
docker-compose up --build
```

Observação: o Dockerfile baixa o modelo ONNX via URL no build; portanto, o build precisa de acesso à internet. Se preferir, baixe o modelo manualmente e monte o volume apontando para /app/models/sentiment_model.onnx.

## Licença
Este projeto está licenciado sob a MIT License — veja o arquivo [LICENSE](LICENSE).


## Contribuição
Contribuições são bem-vindas. Para contribuir:
1. Fork o repositório.
2. Crie uma branch com sua feature/fix: `git checkout -b feat/minha-feature`
3. Abra um Pull Request descrevendo a mudança.
4. Mantenha o padrão de código (format, testes) e adicione testes quando aplicável.

Sugestão: incluir um template de ISSUE/PR no repositório para padronizar contribuições.



Projeto original/fonte: [SentimentONE/sentimentAPI](https://github.com/SentimentONE/sentimentAPI)  
Fork realizado por: Matheus-es — https://github.com/Matheus-es/sentimentAPI
