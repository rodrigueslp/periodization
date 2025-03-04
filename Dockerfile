# Use JDK 21 como imagem base
FROM eclipse-temurin:21-jdk-alpine

# Define o diretório de trabalho
WORKDIR /app

# Copia os arquivos gradle para resolução de dependências
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Se você tiver o gradlew no projeto, descomente a linha abaixo
# COPY gradlew ./
# RUN chmod +x ./gradlew

# Se não tiver o gradlew, vamos usar o gradle instalado na imagem
RUN apk add --no-cache gradle

# Copia o código fonte
COPY src ./src

# Constrói a aplicação
RUN gradle bootJar --no-daemon

# Encontra o arquivo JAR gerado e o move para um local conhecido
RUN find /app/build/libs -name "*.jar" -exec mv {} /app/app.jar \; || echo "Jar not found"

# Verifica se o JAR foi criado
RUN ls -la /app/

# Cria diretório para armazenamento de arquivos
RUN mkdir -p files

# Configuração de runtime
EXPOSE 8080

# Define variável de ambiente específica do Railway para usar a porta atribuída
ENV PORT=8080

# Ponto de entrada para executar a aplicação
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
