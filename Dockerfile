# Etapa de build com Gradle e New Relic
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Instala Gradle e ferramentas necessárias
RUN apk add --no-cache gradle wget unzip ca-certificates && update-ca-certificates

# Baixa e extrai o New Relic Java Agent
RUN wget -O newrelic-java.zip https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip \
    && unzip -q newrelic-java.zip \
    && rm newrelic-java.zip

# Copia os arquivos de build
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

# Constrói o JAR
RUN gradle bootJar --no-daemon

# Copia o jar final para o local padrão
RUN find build/libs -name "*.jar" -exec cp {} app.jar \;

# Etapa de runtime com JRE e agente do New Relic
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Instala dependências mínimas
RUN apk add --no-cache ca-certificates && update-ca-certificates

# Copia artefatos da etapa de build
COPY --from=builder /app/app.jar ./app.jar
COPY --from=builder /app/newrelic ./newrelic

# (Opcional) Copia o seu arquivo newrelic.yml com variáveis de ambiente
COPY newrelic/newrelic.yml ./newrelic/newrelic.yml

# Variáveis para New Relic (podem ser sobrescritas no Railway)
ENV NEW_RELIC_APP_NAME="PeriodizationApp"
ENV NEW_RELIC_LOG_LEVEL="info"

# Exposição da porta
EXPOSE 8080

# Executa o app com o agente New Relic ativado
ENTRYPOINT ["sh", "-c", "echo 'Iniciando app com New Relic...' && java -javaagent:/app/newrelic/newrelic.jar -jar app.jar"]
