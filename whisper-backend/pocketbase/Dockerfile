FROM alpine:3.20

# Create user + install tools
RUN adduser -D -H -s /sbin/nologin pb && \
    apk add --no-cache unzip curl ca-certificates

# Choose PocketBase version (change later if needed)
ARG PB_VERSION=0.22.20

# Download PocketBase binary and install
RUN curl -L -o /tmp/pb.zip \
      https://github.com/pocketbase/pocketbase/releases/download/v${PB_VERSION}/pocketbase_${PB_VERSION}_linux_amd64.zip \
 && unzip /tmp/pb.zip -d /usr/local/bin \
 && rm /tmp/pb.zip \
 && chmod +x /usr/local/bin/pocketbase

USER pb
WORKDIR /data

# Persist data/public
VOLUME ["/data/pb_data","/data/pb_public"]

EXPOSE 8090

# Run PocketBase and point to our mounted volumes
ENTRYPOINT ["/usr/local/bin/pocketbase","serve","--http=0.0.0.0:8090","--dir","/data/pb_data","--publicDir","/data/pb_public"]
