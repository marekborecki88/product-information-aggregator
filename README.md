# Product Information Aggregator

A service that aggregates product information from multiple downstream services into a single unified response.
The project consists of one core aggregation service written in **Kotlin / Spring Boot** and four lightweight mock services written in **Go**.

---

## Architecture

```
                        ┌─────────────────────────┐
                        │   aggregation-service   │  :8080
                        │   (Kotlin/Spring Boot)  │
                        └────────────┬────────────┘
                                     │  calls in parallel
           ┌─────────────────────────┼──────────────────────────┼──────────────────────────┐
           ▼                         ▼                          ▼                          ▼
  ┌─────────────────┐   ┌──────────────────────┐   ┌────────────────────────┐   ┌──────────────────────┐
  │ catalog-mock    │   │  price-mock-service  │   │ availability-mock      │   │ customer-mock        │
  │ service  :8081  │   │  :8082               │   │ service  :8083         │   │ service  :8084       │
  └─────────────────┘   └──────────────────────┘   └────────────────────────┘   └──────────────────────┘
```

The aggregation service calls all four downstream services and combines their responses into one.

---

## How to run the service

Make sure you have **Docker** and **Docker Compose** installed (or podman), then run:


```bash
docker-compose up --build
```
or with podman:

```bash
podman-compose up --build
```

Note: If building fails, try once again or build images independently, it can happen because of the container memory limit.
```bash
docker build -t aggregation-service ./aggregation-service
docker build -t catalog-mock-service ./catalog-mock-service
docker build -t price-mock-service ./price-mock-service
docker build -t availability-mock-service ./availability-mock-service
docker build -t customer-mock-service ./customer-mock-service
```

To stop the services, run:

```bash
docker-compose down -v --rmi all
```
or with podman: 

```bash
podman-compose down -v --rmi all
```

This will build and start all five services:

| Service                  | Port  |
|--------------------------|-------|
| aggregation-service      | 8080  |
| catalog-mock-service     | 8081  |
| price-mock-service       | 8082  |
| availability-mock-service| 8083  |
| customer-mock-service    | 8084  |

---

## API

### Get product details

```
GET http://localhost:8080/products/{productId}?market={market}&customerId={customerId}
```

| Parameter    | Required | Description                                               |
|--------------|----------|-----------------------------------------------------------|
| `productId`  | ✅        | Product identifier (mock data available for `3`)         |
| `market`     | ✅        | Market locale, e.g. `pl-PL`, `de-DE`, `nl-NL`            |
| `customerId` | ❌        | Customer identifier (mock data available for `1`, `2`)   |

**Example request:**

```bash
curl "http://localhost:8080/products/3?market=pl-PL&customerId=1"
```

---

## Mock services

Each mock service exposes its data endpoint and an admin endpoint to simulate latency:

### Catalog mock (`catalog-mock-service`)
| Method | Path                          | Query Parameters | Description                     |
|--------|-------------------------------|------------------|------------------------------------|
| GET    | `/catalog/products/:id`       | `market` (required) | Returns product catalog data    |
| POST   | `/admin/delay`                | - | Sets response delay in ms       |
| POST   | `/admin/error`                | - | Sets HTTP error status code     |

### Price mock (`price-mock-service`)
| Method | Path            | Query Parameters | Description                             |
|--------|-----------------|------------------|-------------------------------------------|
| GET    | `/prices/:id`   | `market` (required), `customerId` (optional) | Returns base price, discount & final price |
| POST   | `/admin/delay`  | - | Sets response delay in ms               |
| POST   | `/admin/error`  | - | Sets HTTP error status code             |

### Availability mock (`availability-mock-service`)
| Method | Path                   | Query Parameters | Description                             |
|--------|------------------------|------------------|--------------------------------------------|
| GET    | `/availability/:id`    | `market` (required) | Returns stock level & delivery estimate |
| POST   | `/admin/delay`         | - | Sets response delay in ms               |
| POST   | `/admin/error`         | - | Sets HTTP error status code             |

### Customer mock (`customer-mock-service`)
| Method | Path                              | Query Parameters | Description                        |
|--------|-----------------------------------|-|----|  
| GET    | `/customer-context/:customerId`   | - | Returns customer segment & preferences |
| POST   | `/admin/delay`                    | - | Sets response delay in ms          |
| POST   | `/admin/error`                    | - | Sets HTTP error status code        |


**Example — get product catalog with market parameter:**

```bash
curl "http://localhost:8081/catalog/products/3?market=pl-PL"
```

**Example — get price with market and customer ID parameters:**

```bash
curl "http://localhost:8082/prices/3?market=pl-PL&customerId=1"
```

**Example — get availability for specific market:**

```bash
curl "http://localhost:8083/availability/3?market=de-DE"
```

**Example — simulate 200ms delay on catalog service:**

```bash
curl -X POST http://localhost:8081/admin/delay \
     -H "Content-Type: application/json" \
     -d '{"delayMs": 200}'
```
**Example — simulate 500 error on price service:**

```bash
curl -X POST http://localhost:8082/admin/error \
     -H "Content-Type: application/json" \
     -d '{"error": 500}'
```

**Reset error status (return to normal operation):**

```bash
curl -X POST http://localhost:8082/admin/error \
     -H "Content-Type: application/json" \
     -d '{}'
```

---

## Health check

```bash
curl http://localhost:8080/actuator/health
```

---

## Metrics

The aggregation service exposes metrics for monitoring client requests to downstream services.

### List all available metrics

```bash
curl http://localhost:8080/actuator/metrics
```

### Get metrics for specific client

Each downstream service has metrics available:

```bash
# Availability client metrics
curl http://localhost:8080/actuator/metrics/availability.client.request

# Catalog client metrics
curl http://localhost:8080/actuator/metrics/catalog.client.request

# Price client metrics
curl http://localhost:8080/actuator/metrics/price.client.request

# Customer client metrics
curl http://localhost:8080/actuator/metrics/customer.client.request
```

### Filter metrics by outcome

Metrics can be filtered by outcome (success, timeout, error):

```bash
# Successful requests
curl "http://localhost:8080/actuator/metrics/availability.client.request?tag=outcome:success"

# Timeout requests
curl "http://localhost:8080/actuator/metrics/availability.client.request?tag=outcome:timeout"

# Error requests
curl "http://localhost:8080/actuator/metrics/availability.client.request?tag=outcome:error"
```

## Design decisions

I've decided to mock services in Go because of its simplicity and ease of creating lightweight HTTP servers. Go's standard library provides excellent support for building RESTful APIs with minimal boilerplate, making it ideal for quickly setting up mock services.
Mocks are more for check case to case than performance testing.

I've decided to provide rich response data with information about upstream failure and reason.
In case core upstream fail (product catalog), the response will contain only product ID and error details.
In case customer context there is default value and information if response was degraded. It means is the response is original from upstream or default value because of upstream failure.

I've decided to write main service in Kotlin, but I'm not working with Kotlin.

## What I would improve with more time

- Implement Circuit breaker, retry and cache, add switch to mock services to toggle between strict mode (like now, return error status when it is set and use given delay) and random mode where error is returned randomly with given probability and delay is within given range normally distributed (60% in the middle of range, and some small percent out of scope bellow and above) to make possible do performance test with all features like circuit breaker, retry and caching.
- Add contract testing to ensure the aggregation service correctly handles responses from upstream services, including error scenarios.
- I would add Grafana to visualize metrics and make it easier to analyze the performance of the aggregation service and its interactions with downstream services.
- Add tests for aggregate response (3-4 hours is much less than I need to design and implemented it, even with copilot)

## Answer for question 'Option A: "The Assortment team wants to add a 'Related Products' service (200ms latency, 90% reliability). How would your design accommodate this? Should it be required or optional?"'

I would be optional, because it is just enricher, and less important then availability, price and customer context.
200 ms of latency is a lot and it double the biggest latency at this moment.
To adapt this feature I would do exactly what for other enrichers and additionally:
- prepare caching strategy, because of high latency, to cache related products for some time and avoid calling this service for every request.
- prepare warming-up batch to prepare cache with the most popular products for the peak time.
- A/B testing for static cache and dynamic calculated data for related products.


