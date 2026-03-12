# ProtVar MCP Server

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server that exposes ProtVar protein variant data as AI-accessible tools, built with Spring Boot and Spring AI.

---

## Architecture

```
Claude Code / Claude.ai
        │
        │  HTTP (Streamable HTTP transport)
        ▼
ProtVar MCP Server  (this app)
        │
        │  REST
        ▼
ProtVar API (wwwdev / wwwint / prod)
```

The MCP server acts as a bridge — it wraps ProtVar REST API calls as MCP tools that AI clients can invoke directly.

---

## Prerequisites

- Java 21
- Maven 3.9+
- [Claude Code CLI](https://docs.anthropic.com/en/docs/claude-code/overview) (for local testing)

---

## Running Locally

### From IntelliJ

1. Open `ProtvarMcpApplication.java`
2. Click the green ▶ button next to `main()`
3. Set the active profile via **Run → Edit Configurations → Active profiles**: `dev` or `int`

Or set it via **VM options**:
```
-Dspring.profiles.active=dev
```

### From the terminal

```bash
# Using Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or build and run the JAR
mvn clean package -DskipTests
java -jar target/protvar-mcp-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### Profiles

| Profile | ProtVar API target |
|---|---|
| `dev` (default) | https://wwwdev.ebi.ac.uk/ProtVar/api |
| `int` | https://wwwint.ebi.ac.uk/ProtVar/api |

---

## Verifying the Server is Running

The app exposes a health endpoint via Spring Actuator:

```
http://localhost:8081/ProtVar/actuator/health
```

Expected response: `{"status":"UP"}`

---

## Connecting Claude Code (Local)

Once the server is running, register it with Claude Code:

```bash
claude mcp add protvar-mcp --transport http http://localhost:8081/ProtVar/mcp
```

Verify the connection:

```bash
claude mcp list
```

Expected output:
```
protvar-mcp: http://localhost:8081/ProtVar/mcp (HTTP) - ✓ Connected
```

Inside a Claude Code session, run `/mcp` to list connected servers and available tools.

To remove or reconfigure:
```bash
claude mcp remove protvar-mcp
```

---

## Connecting Claude Code (Deployed)

```bash
# int environment
claude mcp add protvar-mcp-int --transport http https://wwwint.ebi.ac.uk/ProtVar/mcp

# dev environment
claude mcp add protvar-mcp-dev --transport http https://wwwdev.ebi.ac.uk/ProtVar/mcp
```

For **Claude.ai** (the web product), add it as a remote connector via **Settings → Integrations → Remote MCP** using the same HTTPS URL.

---

## Transport: Why HTTP?

| Transport | Use case |
|---|---|
| `stdio` | Local only — Claude Code launches the app as a subprocess |
| `http` (Streamable HTTP) | Local or remote — connects to a running server over HTTP/S |

This project uses **Streamable HTTP** transport so the same server can be used locally during development and deployed to Kubernetes for production use, with no configuration changes to the server itself.

---

## Key Dependencies

| Dependency | Purpose |
|---|---|
| `spring-ai-starter-mcp-server-webmvc` | Exposes MCP tools over Streamable HTTP |
| `spring-boot-starter-web` | Embedded Tomcat / Spring MVC |
| `spring-boot-starter-actuator` | Health endpoint at `/actuator/health` |
| `spring-boot-devtools` | Hot reload during local development |

> **Note:** `spring-ai-starter-mcp-server` (without `-webmvc`) only supports STDIO transport and will not expose an HTTP endpoint.

---

## Deployment (Kubernetes / Helm)

The app is deployed via GitLab CI and Helm charts.

| Branch | Environment | Helm values |
|---|---|---|
| `dev` | Beta / dev | `values-dev.yaml` |
| `int` | Internal | `values-int.yaml` |
| `main` | Production | _not yet configured_ |

The CI pipeline runs: `test → build → build_docker_image → deploy`

The Kubernetes Ingress exposes the app at `/ProtVar` on the environment hostname. The Ingress is configured with extended proxy timeouts (`proxy-read-timeout: 3600`) to support long-lived Streamable HTTP connections.

---

## Project Structure

```
src/
└── main/
    └── java/uk/ac/ebi/protvar/mcp/
        ├── ProtvarMcpApplication.java   # Entry point, registers MCP tools
        ├── config/
        │   └── RestClientConfig.java    # RestClient bean pointing to ProtVar API
        └── tools/
            └── FoldxTool.java           # @Tool: FoldX predictions
resources/
├── application.properties              # Base config (port, MCP path, context path)
├── application-dev.properties          # dev API URL
└── application-int.properties          # int API URL
```

---

## Adding New Tools

1. Create a new `@Component` class in `tools/`
2. Annotate methods with `@Tool(description = "...")`
3. Register the bean in `ProtvarMcpApplication`:

```java
@Bean
public ToolCallbackProvider protvarTools(FoldxTool foldxTool, MyNewTool myNewTool) {
    return MethodToolCallbackProvider.builder()
            .toolObjects(foldxTool, myNewTool)
            .build();
}
```

## Future Improvement (very useful)
Instead of calling the API via HTTP, you can create a shared library:
```
protvar-common
```
Then both apps share logic.
```
protvar-api
protvar-mcp
        ↓
    protvar-common
```

This avoids HTTP overhead.