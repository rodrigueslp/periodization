# Use JDK 21 como imagem base
FROM eclipse-temurin:21-jdk-alpine

# Define o diretório de trabalho
WORKDIR /app

# Copia os arquivos gradle para resolução de dependências
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Cria o wrapper gradle se não existir
RUN if [ ! -f "gradlew" ]; then touch gradlew && chmod +x gradlew; fi

# Copia o código fonte
COPY src ./src

# Constrói a aplicação
RUN ./gradlew bootJar --no-daemon

# Cria diretório para armazenamento de arquivos
RUN mkdir -p files

# Configuração de runtime
EXPOSE 8080

# Define variável de ambiente específica do Railway para usar a porta atribuída
ENV PORT=8080

# Ponto de entrada para executar a aplicação
ENTRYPOINT ["java", "-jar", "/app/build/libs/*.jar"]
