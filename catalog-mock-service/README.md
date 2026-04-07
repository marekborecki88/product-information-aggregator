# Catalog Mock Service

A simple Go + Gin mock service that provides product information via HTTP API.

## Build

```bash
go build -o catalog-mock-service .
```

## Run

```bash
go run main.go
```

### Docker

Build the image:
```bash
docker build -t catalog-mock-service .
```

Run the container:
```bash
docker run -p 8080:8080 catalog-mock-service
```

The service will start on `http://localhost:8080`

## API

### Get Product

```
GET /products/{id}?market={market}
```

**Parameters:**
- `id` (path): Product ID
- `market` (query): Market code

**Response:**
```json
{
  "id": "123",
  "name": "Sample Product 123",
  "price": 99.99,
  "market": "US",
  "description": "This is a mock product for market US",
  "images": [
    "https://images.example.com/products/123/main.jpg",
    "https://images.example.com/products/123/tractor-wheel.jpg",
    "https://images.example.com/products/123/tractor-engine.jpg",
    "https://images.example.com/products/123/tractor-cabin.jpg"
  ]
}
```
