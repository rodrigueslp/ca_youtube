FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copiar os arquivos do projeto
COPY . /app/

# Dar permissão de execução ao gradlew
RUN chmod +x gradlew

# Buildar o projeto
RUN ./gradlew clean build -x check -x test

# Expor a porta que sua aplicação usa (ajuste conforme necessário)
EXPOSE 8080

# Comando para executar a aplicação
CMD ["java", "-jar", "build/libs/*.jar"]