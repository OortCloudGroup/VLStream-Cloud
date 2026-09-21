FROM golang:1.23.8-alpine3.21 AS gateway-build
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go test ./... \
    && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -trimpath -ldflags="-s -w" -o /out/vls-tunnel-runtime .

FROM alpine:3.21 AS rathole-download
ARG RATHOLE_VERSION=v0.5.0
ARG RATHOLE_ARCH=x86_64-unknown-linux-gnu
ARG RATHOLE_ZIP_SHA256=3e7d0d0f365120cd3cd351d147d1a12ee960c8068b464d4dd533a3821873b80e
RUN apk add --no-cache ca-certificates curl unzip \
    && curl --fail --location --proto '=https' --tlsv1.2 \
       "https://github.com/rathole-org/rathole/releases/download/${RATHOLE_VERSION}/rathole-${RATHOLE_ARCH}.zip" \
       --output /tmp/rathole.zip \
    && echo "${RATHOLE_ZIP_SHA256}  /tmp/rathole.zip" | sha256sum -c - \
    && unzip /tmp/rathole.zip -d /out \
    && chmod 0755 /out/rathole

FROM debian:bookworm-slim
RUN apt-get update \
    && apt-get install --yes --no-install-recommends ca-certificates libssl3 \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --home-dir /var/lib/vls-tunnel --create-home vls-tunnel
COPY --from=gateway-build /out/vls-tunnel-runtime /usr/local/bin/vls-tunnel-runtime
COPY --from=rathole-download /out/rathole /usr/local/bin/rathole
RUN install -d -o vls-tunnel -g vls-tunnel -m 0700 /var/lib/vls-tunnel
USER 10001:10001
EXPOSE 2333 8088
ENTRYPOINT ["/usr/local/bin/vls-tunnel-runtime"]
