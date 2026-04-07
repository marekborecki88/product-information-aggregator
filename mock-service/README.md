# RUN BY DOCKER / PODMAN

## BUILD

```bash
docker build -t mock-service .
```

or with Podman:

```bash
podman build -t mock-service .
```

## RUN

```bash
docker run -p 8080:8080 mock-service
```

or with Podman:

```bash
podman run -p 8080:8080 mock-service
```


## INSTALL DEPENENCIES

```
go mod tidy
```

## RUN

```
go mod tidy
```


