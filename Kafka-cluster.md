Para realizar la práctica, vamos a crear un cluster Kafka. Para ello vamos a utilizar docker compose. 
Declararemos los siguientes servicios:
* Un servidor Zookeeper versión 3.8 para que gestione los metadatos.
* Tres brokers Kafka versión 2.8.1 (compatible con scala 2.12).

Declararemos un volumen para cada servicio.

Adicionalmente, para la realización de la segunda práctica se ha añadido a la network un contenedor con una imagen de Presto.

Este es el contenido del archivo docker-compose.yml:

```yaml
version: "3.5"

networks:
  cluster-kafka:
    driver: bridge
services:
  zookeeper:
    image: docker.io/bitnami/zookeeper:3.8
    ports:
      - "2181"
    environment:
      - ALLOW_ANONYMOUS_LOGIN=yes
    volumes:
      - zookeeper_data:/bitnami/zookeeper
    networks:
      - cluster-kafka
  kafka-0:
    image: docker.io/bitnami/kafka:2.8.1
    container_name: kafka-0
    hostname: kafka-0
    ports:
      - "29092:29092"
    environment:
      - KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181
      - KAFKA_CFG_BROKER_ID=0
      - ALLOW_PLAINTEXT_LISTENER=yes
      - KAFKA_CFG_LISTENERS= PLAINTEXT://:9092,CONNECTIONS_FROM_HOST://:29092
      - KAFKA_CFG_ADVERTISED_LISTENERS= PLAINTEXT://kafka-0:9092,CONNECTIONS_FROM_HOST://localhost:29092
      - KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP= PLAINTEXT:PLAINTEXT,CONNECTIONS_FROM_HOST:PLAINTEXT
      - KAFKA_CFG_INTER_BROKER_LISTENER_NAME= PLAINTEXT
    volumes:
      - kafka_0_data:/bitnami/kafka
    depends_on:
      - zookeeper
    networks:
      - cluster-kafka
  kafka-1:
    image: docker.io/bitnami/kafka:2.8.1
    container_name: kafka-1
    hostname: kafka_1
    ports:
      - "29093:29093"
    environment:
      - KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181
      - KAFKA_CFG_BROKER_ID=1
      - ALLOW_PLAINTEXT_LISTENER=yes
      - KAFKA_CFG_LISTENERS= PLAINTEXT://:9092,CONNECTIONS_FROM_HOST://:29093
      - KAFKA_CFG_ADVERTISED_LISTENERS= PLAINTEXT://kafka-1:9092,CONNECTIONS_FROM_HOST://localhost:29093
      - KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP= PLAINTEXT:PLAINTEXT,CONNECTIONS_FROM_HOST:PLAINTEXT
      - KAFKA_CFG_INTER_BROKER_LISTENER_NAME= PLAINTEXT
    volumes:
      - kafka_1_data:/bitnami/kafka
    depends_on:
      - zookeeper
    networks:
      - cluster-kafka
  kafka-2:
    image: docker.io/bitnami/kafka:2.8.1
    container_name: kafka-2
    hostname: kafka-2
    ports:
      - "29094:29092"
    environment:
      - KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181
      - KAFKA_CFG_BROKER_ID=2
      - ALLOW_PLAINTEXT_LISTENER=yes
      - KAFKA_CFG_LISTENERS= PLAINTEXT://:9092,CONNECTIONS_FROM_HOST://:29094
      - KAFKA_CFG_ADVERTISED_LISTENERS= PLAINTEXT://kafka-2:9092,CONNECTIONS_FROM_HOST://localhost:29094
      - KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP= PLAINTEXT:PLAINTEXT,CONNECTIONS_FROM_HOST:PLAINTEXT
      - KAFKA_CFG_INTER_BROKER_LISTENER_NAME= PLAINTEXT
    volumes:
      - kafka_2_data:/bitnami/kafka
    depends_on:
      - zookeeper
    networks:
      - cluster-kafka
  presto:
    image: ahanaio/prestodb-sandbox
    ports:
      - '8080:8080'
    expose:
      - '8080'
    volumes:
      - ./presto_etc/catalog/kafka.properties:/opt/presto-server/etc/catalog/kafka.properties
      - ./presto_etc/kafka:/opt/presto-server/etc/kafka
volumes:
  zookeeper_data:
    driver: local
  kafka_0_data:
    driver: local
  kafka_1_data:
    driver: local
  kafka_2_data:
    driver: local
```
