# Availability Mock Service

A mock service providing product availability information including stock levels, warehouse locations, and expected delivery dates.

## Endpoints

### GET /availability/:id

Returns availability information for a given product ID.

**Parameters:**
- `id` (path parameter): The product ID

**Query Parameters:**
- None

**Response Example:**
```json
{
  "productId": "3",
  "availability": [
    {
      "stockLevel": 15,
      "warehouseLocation": "Berlin",
      "expectedDelivery": "2026-04-08"
    },
    {
      "stockLevel": 8,
      "warehouseLocation": "Amsterdam",
      "expectedDelivery": "2026-04-09"
    },
    {
      "stockLevel": 25,
      "warehouseLocation": "Warsaw",
      "expectedDelivery": "2026-04-07"
    }
  ]
}
```

**Note:** Mock data is currently only available for product ID `3`.

## Development

```bash
go run main.go
```

## Docker

```bash
docker build -t availability-mock-service .
docker run -p 8080:8080 availability-mock-service
```
