# Build stage
FROM golang:1.26-alpine AS builder
WORKDIR /app
COPY . .
RUN go mod tidy
RUN CGO_ENABLED=0 go build -o server main.go

# Final stage
FROM alpine:latest
WORKDIR /root/
COPY --from=builder /app/server .

EXPOSE 8080

# Entrypoint allows passing additional flags during 'docker run'
ENTRYPOINT ["./server"]

# Default values if no arguments are provided
CMD ["-path", "/mock", "-body", "{\"message\": \"default\"}", "-delay", "0"]