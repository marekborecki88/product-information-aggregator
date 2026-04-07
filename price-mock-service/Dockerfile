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

CMD ["./server"]