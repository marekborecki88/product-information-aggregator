# Build stage
FROM golang:1.25-alpine AS builder

WORKDIR /app

COPY go.mod go.sum ./

RUN go mod download

COPY main.go .

RUN CGO_ENABLED=0 GOOS=linux go build -o catalog-mock-service .

# Runtime stage
FROM alpine:latest

WORKDIR /app

COPY --from=builder /app/catalog-mock-service .

EXPOSE 8080

CMD ["./catalog-mock-service"]
