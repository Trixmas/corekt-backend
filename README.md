# Corekt Backend

Corekt is a coherence tool.

- Swagger: http://localhost:8080/api/v1/swagger-ui/index.html (dev only)
- Allowed origins (CORS): http://localhost:3000
- Contributing guidelines: [Guidelines](CONTRIBUTING.md)

## Prerequisites

- Java 25 with Maven
- Docker Desktop. Download it from [here](https://www.docker.com/products/docker-desktop/)
- IDE, preferably IntelliJ IDEA. Download it from [here](https://www.jetbrains.com/idea/)

## Environments

### Development

### 1. Environment Variables

Update the `application.properties` file with your environment variables. The create the database container witl the
following command:

```bash
docker compose up -d postgres
```

Once your database is up and running, you can run the application with the following command:

```bash
mvn spring-boot:run
```

### Production

### 1. Environment Variables

Create a file named `.env` in the root directory of the project.

```env
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/corekt
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=yourpassword

# JWT
JWT_SECRET=your_secret_minimum_256_bits
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# Encryption
ENCRYPTION_MASTER_KEY=your_master_key
ENCRYPTION_RSA_PRIVATE_KEY=your_rsa_private_key

# Mistral AI
MISTRAL_API_TOKEN=your_mistral_token
MISTRAL_ANALYSIS_AGENT_ID=your_review_analysis_agent_id
MISTRAL_PARSED_DATA_AGENT_ID=your_parsed_data_agent_id
SPRING_AI_MISTRALAI_API_KEY=your_mistral_api_key

# LDAP (JumpCloud)
JUMPCLOUD_LDAP_URL=ldaps://ldap.jumpcloud.com:636
JUMPCLOUD_LDAP_BASE=your_ldap_base
JUMPCLOUD_LDAP_USERDN=your_ldap_userdn
JUMPCLOUD_LDAP_PASSWORD=your_ldap_password

# CORS
CORS_ALLOWED_ORIGINS=https://yourdomain.com

# Rate Limiting
RATELIMIT_REQUESTS=50
RATELIMIT_DURATION_MINUTES=1

# Taiga
TAIGA_API_BASE_URL=https://api.taiga.io/api/v1
```

### 2. Build and run

Run the following to build and run the application. The database will be created automatically.

```bash
docker compose up -d
```

## Reference Documentation

* [Apache Maven](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin](https://docs.spring.io/spring-boot/4.0.3/maven-plugin)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.0.3/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Web](https://docs.spring.io/spring-boot/4.0.3/reference/web/servlet.html)
* [Testcontainers](https://java.testcontainers.org/)
* [Testcontainers Postgres](https://java.testcontainers.org/modules/databases/postgres/)
* [Validation](https://docs.spring.io/spring-boot/4.0.3/reference/io/validation.html)
