# Use JDK 21 como imagem base
FROM eclipse-temurin:21-jdk-alpine

# Define o diretório de trabalho
WORKDIR /app

# Copia os arquivos gradle para resolução de dependências
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Instala o Gradle (caso não tenha wrapper)
RUN apk add --no-cache gradle gettext

# Copia o código fonte
COPY src ./src

# Constrói a aplicação
RUN gradle bootJar --no-daemon

# Move o .jar gerado para local conhecido
RUN find /app/build/libs -name "*.jar" -exec mv {} /app/app.jar \; || echo "Jar not found"

# Copia a pasta do New Relic
COPY newrelic/ /app/newrelic/

# Cria diretório adicional (se necessário pela app)
RUN mkdir -p files

# Exponha a porta
EXPOSE 8080
ENV PORT=8080

# Entrypoint com:
# - envsubst para resolver variáveis no newrelic.yml
# - echo das variáveis e do newrelic.yml interpolado
# - cat do log do agente após execução
# (mantém todas as etapas anteriores...)

ENTRYPOINT ["/bin/sh", "-c", "\
  echo '== Interpolando newrelic.yml com variáveis de ambiente ==' && \
  envsubst < /app/newrelic/newrelic.yml > /app/newrelic/newrelic-final.yml && \
  echo '== VARIÁVEIS DE AMBIENTE ==' && \
  echo NEW_RELIC_LICENSE_KEY=$NEW_RELIC_LICENSE_KEY && \
  echo NEW_RELIC_APP_NAME=$NEW_RELIC_APP_NAME && \
  echo '== newrelic-final.yml ==' && \
  cat /app/newrelic/newrelic-final.yml && \
  echo '== INICIANDO APLICAÇÃO ==' && \
  java -javaagent:/app/newrelic/newrelic.jar \
       -Dnewrelic.config.file=/app/newrelic/newrelic-final.yml \
       -jar /app/app.jar & \
  sleep 5 && \
  echo '== LOG DO AGENTE NEW RELIC (PARCIAL) ==' && \
  tail -n 50 /app/newrelic/logs/newrelic_agent.log && \
  wait"]

