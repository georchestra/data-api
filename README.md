# Opendata services

## Build & run

### Requirements

Those components are created with the following requirements:
* Java 17 JDK
* Maven 3.8+
* Docker


### Building

To build the services:

```shell script
./mvnw clean install
```

### Running

The simple build command above created the docker images.

Now run the docker composition as follows, the first time it might need to download some additional images for the rabbitmq event broker and the postgresql config database:

```shell script
docker-compose up -d
```

### Calling services

Test the service using the token:

```shell script
```



### Development/debug

To run one service directly without docker, use the `local` profile.

```shell script
mvn spring-boot:run -Dspring-boot.run.profiles=dev,local -f src/services/data-indexer/
```

## Bugs

TBD

## Roadmap

TBD

## Contributing

To set license header use:

```shell script
./mvnw license:format
```
