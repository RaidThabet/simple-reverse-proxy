# Simple Reverse Proxy

A very simple reverse proxy built with Java and Netty.

## Architecture

![Simple Reverse Proxy Architecture](docs/Simple%20Reverse%20Proxy.png)

## Pipeline

The server's Netty pipeline consists of the following handler chain:
1. `HttpServerCodec`: Encodes and decodes HTTP requests and responses.
2. `HttpObjectAggregator`: Aggregates multiple HTTP messages into a single `FullHttpRequest` or `FullHttpResponse` (max 65536 bytes).
3. `RateLimitHandler`: Ensures rate limiting using a Token Bucket algorithm, returning `429 Too Many Requests` when limits are exceeded.
4. `RouterHandler`: Inspects the requested URI prefix and matches it with configured upstreams.
5. `ForwardHandler`: Acts as the proxy client. It opens a new Netty Channel to the target upstream, sends the HTTP request, and wires the upstream's response back to the inbound channel via `UpstreamHandler`.

## Features

* **Asynchronous & Event-Driven**: Built on Netty 4.2 for throughput and non-blocking I/O.
* **Rate Limiting**: Token Bucket algorithm to protect backend services from being overwhelmed.
* **Configuration Driven**: Uses `config.yaml` to dynamically set up routes, upstreams, and rate-limiting rules.
* **URI Prefix Routing**: Dynamically maps requested HTTP paths to specific backend servers.

## Configuration

The proxy is configured via `src/main/resources/config.yaml`. The following YAML config is an example:

```yaml
server:
  port: 8080
  ratelimit:
    maxTokens: 20
    refillRatePerSecond: 20

routes:
  - prefix: /api/order
    upstream:
      host: localhost
      port: 8081
  - prefix: /api/product
    upstream:
      host: localhost
      port: 8082
```

## Getting Started

Build the project using Maven:

```bash
mvn clean compile
```

Run the reverse proxy server:

```bash
mvn exec:java -Dexec.mainClass="ReverseProxyServer"
```

## Tech Stack

* **Java 25** - Programming language
* **Netty 4.2** - Async event-driven network application framework
* **SnakeYAML** - YAML parser for configuration loading
* **Maven** - Dependency management and building
* **SLF4J** - Logging facade