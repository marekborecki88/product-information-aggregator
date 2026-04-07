# Kramp Mock Services

A project containing two mock services running with Docker Compose. Both services can be configured via environment variables.

## Quick Start

### Run with default values

```bash
podman-compose up
```

- `mock-service-1`: http://localhost:8081/api/v1 (delay: 500ms)
- `mock-service-2`: http://localhost:8082/api/v2 (delay: 3000ms)

### Run single service

```bash
podman-compose up mock-service-1
```

## Configuration

All parameters can be overridden via environment variables:

### Available Variables

| Variable | Default | Service | Description |
|----------|---------|---------|-------------|
| `SERVICE_PATH_1` | `/api/v1` | mock-service-1 | API endpoint path |
| `SERVICE_PATH_2` | `/api/v2` | mock-service-2 | API endpoint path |
| `DELAY_1` | `500` | mock-service-1 | Response delay in milliseconds |
| `DELAY_2` | `3000` | mock-service-2 | Response delay in milliseconds |
| `BODY_1` | `{"status": "fast"}` | mock-service-1 | JSON response body |
| `BODY_2` | `{"status": "slow"}` | mock-service-2 | JSON response body |

## Examples

### Override delay for single service

```bash
DELAY_1=2000 podman-compose up
```

### Override delay for both services

```bash
DELAY_1=1000 DELAY_2=1500 podman-compose up
```

### Override response body

```bash
BODY_1='{"response": "success"}' podman-compose up
```

### Override service path and delay

```bash
SERVICE_PATH_1=/custom/path DELAY_1=5000 podman-compose up
```

### Override multiple parameters

```bash
SERVICE_PATH_1=/v2 DELAY_1=3000 BODY_1='{"data": "custom"}' DELAY_2=2000 podman-compose up
```

## Services

### mock-service-1
- Port: `8081`
- Default path: `/api/v1`
- Default delay: `500ms`
- Default response: `{"status": "fast"}`

### mock-service-2
- Port: `8082`
- Default path: `/api/v2`
- Default delay: `3000ms`
- Default response: `{"status": "slow"}`

## Usage

All parameters are set as environment variables in the container. The Go application must read them using `os.Getenv()`.

Example endpoints:
- `curl http://localhost:8081/api/v1`
- `curl http://localhost:8082/api/v2`
