# CalcIt

CalcIt is a RESTful calculator API built with Java 21, Spring Boot and Maven, delivered through an
automated CI/CD pipeline: GitHub → Jenkins → Docker Hub → Docker Compose.

## API

All operations take two numeric query parameters, `a` and `b`, and return JSON.

| Operation | Endpoint                         | Behaviour                            |
|-----------|----------------------------------|--------------------------------------|
| Add       | `GET /api/v1/add?a=&b=`          | `a + b`                              |
| Subtract  | `GET /api/v1/subtract?a=&b=`     | `a - b`                              |
| Multiply  | `GET /api/v1/multiply?a=&b=`     | `a * b`                              |
| Divide    | `GET /api/v1/divide?a=&b=`       | `a / b`; `b = 0` returns HTTP 400    |
| Health    | `GET /actuator/health`           | Spring Actuator health (`UP`/`DOWN`) |

Success:

```bash
$ curl "http://localhost:8081/api/v1/add?a=2&b=3"
{"operation":"add","a":2.0,"b":3.0,"result":5.0}
```

Errors (division by zero, missing, non-numeric or non-finite parameters) return HTTP 400:

```bash
$ curl "http://localhost:8081/api/v1/divide?a=1&b=0"
{"status":400,"error":"Bad Request","message":"Division by zero is not allowed"}

$ curl "http://localhost:8081/api/v1/subtract?a=5"
{"status":400,"error":"Bad Request","message":"Missing required query parameter: b"}

$ curl "http://localhost:8081/api/v1/multiply?a=x&b=2"
{"status":400,"error":"Bad Request","message":"Parameter 'a' must be a valid number"}
```

## Project Structure

```
CalcIt/
├── src/main/java/com/calcit/
│   ├── controller/      # REST endpoints
│   ├── service/         # calculator logic and validation
│   ├── dto/             # response and error records
│   └── exception/       # global exception handler (HTTP 400 bodies)
├── src/test/java/...    # JUnit 5 unit tests + MockMvc integration tests
├── pom.xml
├── Dockerfile           # multi-stage build (Maven/JDK -> JRE alpine)
├── .dockerignore
├── docker-compose.yml   # 2 API replicas behind an Nginx reverse proxy
├── nginx/nginx.conf
├── Jenkinsfile          # declarative CI/CD pipeline
└── .env.example
```

## Build, Test and Run Locally

Requires JDK 21 (the Maven wrapper downloads Maven).

```bash
./mvnw clean package -DskipTests   # build the executable JAR
./mvnw test                        # run tests; JUnit XML in target/surefire-reports/
java -jar target/calcit-0.0.1-SNAPSHOT.jar   # serves on http://localhost:8080
```

## Docker

```bash
docker build -t mattjoe/calcit:latest .
docker run --rm -p 8080:8080 mattjoe/calcit:latest
curl http://localhost:8080/actuator/health
```

The image is built in two stages: a `maven:3.9-eclipse-temurin-21` stage resolves dependencies
(cached as their own layer until `pom.xml` changes) and packages the JAR, and a
`eclipse-temurin:21-jre-alpine` runtime stage copies only that JAR, runs it as the non-root
`calcit` user, exposes port 8080 and declares a `HEALTHCHECK` on `/actuator/health`.

Docker Hub: https://hub.docker.com/r/mattjoe/calcit

## Docker Compose

```bash
docker compose up -d --wait     # builds the image if it is not present locally
docker compose ps
curl "http://localhost:8081/api/v1/multiply?a=4&b=3"
docker compose down
```

The stack (project name `calcit`) contains:

- `calcit`: the API, 2 replicas, health check on `/actuator/health`, `restart: unless-stopped`,
  configured through environment variables.
- `proxy`: Nginx on host port `8081` (override with `PROXY_PORT`), which load-balances across the
  replicas using Docker's DNS and only starts once the API is healthy.

Both run on the `calcit-net` bridge network; only the proxy publishes a port.
Copy `.env.example` to `.env` to change the image, tag or port.

## CI/CD Pipeline (Jenkins)

The `Jenkinsfile` defines a declarative pipeline triggered by a GitHub webhook (`githubPush`), with
Poll SCM every 5 minutes as a fallback.

| Stage        | What it does                                                                 |
|--------------|------------------------------------------------------------------------------|
| Checkout     | `checkout scm` from GitHub                                                   |
| Build        | `./mvnw clean package -DskipTests`                                           |
| Test         | `./mvnw test`, publishes JUnit reports; a test failure fails the build       |
| Docker Build | Builds `mattjoe/calcit:<BUILD_NUMBER>` and `:latest`                         |
| Push         | Logs in with the `dockerhub-cred` Jenkins credential and pushes both tags     |
| Deploy       | `docker compose up -d --wait` with the new tag (same project, so re-runs update in place) |
| Verify       | Checks `/actuator/health` and a sample calculation through the proxy         |

A success or failure email is sent with the Email Extension plugin.

### Jenkins setup

1. Plugins: Pipeline, Git, GitHub, Email Extension, JUnit, Credentials Binding, Timestamper.
2. Credentials: a *Username with password* credential with ID `dockerhub-cred`
   (Docker Hub username and access token).
3. Email: configure SMTP under *Manage Jenkins → System → Extended E-mail Notification*.
4. Job: *Pipeline from SCM*, repository `https://github.com/MJTheGreat3/CalcIt.git`,
   branch `*/main`, script path `Jenkinsfile`. Tick *GitHub hook trigger for GITScm polling*.
5. Webhook: in the GitHub repo, *Settings → Webhooks*, payload URL
   `https://<public-jenkins-url>/github-webhook/` (e.g. an ngrok tunnel to Jenkins), content type
   `application/json`, event *Just the push event*.

The Jenkinsfile assumes Jenkins runs on the same macOS host as Docker Desktop (Homebrew JDK 21 and
the Docker CLI in `/usr/local/bin`). Adjust `JAVA_HOME` and `PATH` in the `environment` block for
other agents.
