# Etapa de build com Gradle
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Instala Gradle e ferramentas necessárias
RUN apk add --no-cache gradle ca-certificates && update-ca-certificates

# Copia os arquivos de build
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

# Constrói o JAR
RUN gradle bootJar --no-daemon

# Copia o jar final para o local padrão
RUN find build/libs -name "*.jar" -exec cp {} app.jar \;

# Etapa de runtime com JRE
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Instala dependências mínimas
RUN apk add --no-cache ca-certificates && update-ca-certificates

# Copia artefatos da etapa de build
COPY --from=builder /app/app.jar ./app.jar

# Exposição da porta
EXPOSE 8080

# Executa o app
ENTRYPOINT ["java", "-jar", "app.jar"]
