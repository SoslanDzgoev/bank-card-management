# Stage 1: Build
# Используем полный JDK-образ для сборки Maven-проекта
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Сначала копируем только pom.xml и скачиваем зависимости —
# этот слой закешируется и не будет пересобираться при изменении только кода
COPY pom.xml .
RUN apt-get update && apt-get install -y --no-install-recommends maven && rm -rf /var/lib/apt/lists/* && mvn dependency:go-offline -B

# Теперь копируем исходники и собираем jar, пропуская тесты
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime
# Используем лёгкий JRE-образ — не тащим в продакшн весь JDK и Maven
FROM eclipse-temurin:17-jre

WORKDIR /app

# Создаём непривилегированного пользователя — контейнер не должен работать от root
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser

COPY --from=builder /app/target/bankcards-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
