# Práctica I

En esta práctica crearemos topics en Kafka, les añadiremos contenido y lo visualizaremos.

Lo primero es iniciar el cluster. Para ello arrancamos un docker compose con el archivo docker-compose-cluster.yml. Tal y como está configurado, arrancará el servidor zookeeper antes que los brokers kafka (esto es necesario).

En la pestaña build log en la parte de servicios de IntelliJ nos saldrá el siguiente mensaje:

```commandline
Creating network "kafka_default" with the default driver
Creating kafka_presto_1    ... done
Creating kafka_zookeeper_1 ... done
Creating kafka_kafka-2_1   ... done
Creating kafka_kafka-1_1   ... done
Creating kafka_kafka-0_1   ... done
```

Ya tenemos iniciado tanto Zookeeper como Kafka.

Para empezar a trabajar con topics, abrimos un terminal en cualquier broker Kafka y nos movemos al siguiente directorio:

```commandline
cd /opt/bitnami/kafka
```
Este será nuestro directorio de trabajo dentro del broker, ya que es aquí donde están definidos dos comandos Kafka. 

Si invocamos cualquiera de estos comandos, podremos ver los diferentes parámetros de entrada que admite y cuáles de estos son obligatorios.
Por ejemplo si queremos ver los parámetros que admite el comando kafka-topics.sh, introduciríamos el siguiente comando:
```commandline
kafka-topics.sh
```
que produce el siguiente resultado:
```text
Create, delete, describe, or change a topic.
Option                                   Description                            
------                                   -----------                            
--alter                                  Alter the number of partitions,        
                                           replica assignment, and/or           
                                           configuration for the topic.         
--at-min-isr-partitions                  if set when describing topics, only    
                                           show partitions whose isr count is   
                                           equal to the configured minimum. Not 
                                           supported with the --zookeeper       
                                           option.                              
--bootstrap-server <String: server to    REQUIRED: The Kafka server to connect  
  connect to>                              to. In case of providing this, a     
                                           direct Zookeeper connection won't be 
                                           required.                            
...
```

## Topics
Ahora creemos nuestro primer topic:
```commandline
kafka-topics.sh --bootstrap-server localhost:9092 --topic first_topic --create --partitions 3 --replication-factor 2
```
Nos devolverá por pantalla el siguiente mensaje:
```commandline
Created topic first_topic.
```
Parámetros:
* --bootstrap-server: define el cluster Kafka al que nos conectamos.
* --topic: nombre del topic al que nos referimos.
* --create: es la acción que vamos a hacer sobre el topic, en este caso crearlo.
* --partitions: número de particiones en las que dividiremos el topic.
* --replication-factor: factor de replicación del topic en el cluster. 

En mi caso al trabajar con un cluster con más de un broker se puede fijar un factor de replicación de 2. 
En el caso de trabajar con un solo broker esto no es posible y hay que dejarlo en 1.

Listemos ahora los topics que se encuentran en el cluster:
```commandline
kafka-topics.sh --bootstrap-server localhost:9092 --list
```
Nos listará el topic que acabamos de crear además de los que estén ya creados.
```commandline
first_topic
```
Para pedir información detallada sobre un topic en concreto utilizamos el comando --describe junto al topic que queremos describir:
```commandline
kafka-topics.sh --bootstrap-server localhost:9092 --topic first_topic --describe
```
Resultado por pantalla:
```text
Topic: first_topic      TopicId: Qcz8XuhgTKigFwdtqF1ZrQ PartitionCount: 3       ReplicationFactor: 2    Configs: segment.bytes=1073741824
        Topic: first_topic      Partition: 0    Leader: 0       Replicas: 0,2   Isr: 0,2
        Topic: first_topic      Partition: 1    Leader: 2       Replicas: 2,1   Isr: 2,1
        Topic: first_topic      Partition: 2    Leader: 1       Replicas: 1,0   Isr: 1,0
```
Como se puede apreciar, el comando --describe nos da información sobre el número de particiones, el factor de replicación, la organización de las particiones 
y las copias dentro del cluster. Además, podemos saber qué broker lidera cada partición.

Ahora creamos un segundo topic:
```commandline
kafka-topics.sh --bootstrap-server localhost:9092 --topic second_topic --create --partitions 6 --replication-factor 1
```
```text
Created topic second_topic.
```
Lo visualizamos listando los topics existentes:
```commandline
kafka-topics.sh --bootstrap-server localhost:9092 --list
```
```text
first_topic
second_topic
```
Por último borramos este topic con el comando --delete:
```commandline
kafka-topics.sh --bootstrap-server localhost:9092 --topic second_topic --delete
```
Si listamos de nuevo los topics, solo nos aparece first_topic.
## Producers
Ahora nutriremos de datos nuestro topic. 
Para ello, una vez creado el topic (en realidad no es necesario, 
ya que la opción de crear un topic en caso de que no exista está habilitada en la configuración del servidor kafka), 
creamos un producer que apunte a nuestro topic e introduciremos unos cuantos mensajes:
```commandline 
kafka-console-producer.sh --broker-list localhost:9092 --topic first_topic 
>Hola Mundo            
>Aprendiendo kafka
>Mas texto para seguir aprendiendo
>Otro mensaje
>Ctrl+C
```
Para cerrar el producer pulsaremos ```Ctrl + C```.

Ahora le pasamos más propiedades al producer. Esta propiedad en especial espera la
sincronización de las réplicas para realizar la confirmación, 
lo cual garantiza que el registro no se pierda 
mientras al menos un broker (en el que se encuentra una réplica de este registro) se
mantenga levantado y en ejecución. La propiedad ```acks=all``` es la garantía mas fuerte de
disponibilidad.

En la misma consola lanzamos el producer
```commandline 
kafka-console-producer.sh --broker-list localhost:9092 --topic first_topic --producer-property acks=all
>Un mensaje que es confirmado
>Otro mensaje mas
>Seguimos aprendiendo
>Ctrl+C
```
Ahora intentaremos escribir un mensaje en un topic que no existe, ¿Qué pasará?
```commandline 
kafka-console-producer.sh --broker-list localhost:9092 --topic new_topic
>Mensaje en topic nuevo inexistente
[2022-06-06 19:17:28,385] WARN [Producer clientId=console-producer] Error while fetching metadata 
with correlation id 3 : {new_topic=LEADER_NOT_AVAILABLE} (org.apache.kafka.clients.NetworkClient)
>Otro mensaje en este topic nuevo
>Un ultimo mensaje
>Ctrl+C
```
El topic se crea solo, ya que la opción de creado automático está activada por defecto en la configuración del cluster.
En esta configuración se encuentran también los valores por defecto para algunos parámetros, 
como por ejemplo el número de particiones por defecto de un topic. 

Al insertar el primer mensaje, nos salta un warning que se debe a que a las particiones del topic no se le han asignado
un broker líder. Se le asigna un líder automáticamente y en los siguientes mensajes ya no aparece el Warning.

Lanzamos un ```--describe``` de este topic para verlo más en detalle:
```commandline
kafka-topics.sh --bootstrap-server localhost:9092 --topic new_topic --describe
```
```text
Topic: new_topic        TopicId: WSnokXgOQcGvZ2Gdt-eG2g PartitionCount: 1       ReplicationFactor: 1    Configs: segment.bytes=1073741824
        Topic: new_topic        Partition: 0    Leader: 1       Replicas: 1     Isr: 1
```
Podemos ver que los valores por defecto del número de particiones y del factor de replicación son ambos 1. 
También podemos ver que se le ha asignado el Broker 1 (están el 0, 1 y 2) y que,
al ser solamente una partición y una copia, el topic se encuentra almacenado exclusivamente en este broker. 

## Consumers

Ahora veremos las formas de consumir los mensajes de kafka
```commandline
kafka-console-consumer.sh
```
Exploramos todos los parámetros y opciones que tiene ese comando.

Creamos un consumer que apunte al topic first_topic:
```commandline
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic first_topic
```
No lee todo el topic, sino que lee todos los nuevos mensajes que lleguen una vez
lanzado el consumidor, esa es la razón por la cual no ha leído los mensajes que habíamos escrito antes.

Así que en otra terminal empezamos a escribir de nuevo en este topic para ver
los mensajes.
Este producer a diferencia de como hemos hecho anteriormente lo dejaremos
ejecutando (NO utilizamos Ctrl+C)
```commandline 
kafka-console-producer.sh --broker-list localhost:9092 --topic first_topic
>Hola nuevo mensaje para el consumer
>Mas mensajes de prueba
> mas mensajes
>
```
En la terminal en la que tenemos el consumer irán apareciendo los mensajes a medida que los vamos enviando:
```text
Hola nuevo mensaje para el consumer
Mas mensajes de prueba
 mas mensajes
```
Para leer todos los mensajes cancelamos el consumer (utilizando Ctrl+C) y lo lanzamos de nuevo,
pero indicando que lea los mensajes desde el principio del topic, con el comando ```--from-beginning```, y así podremos ver todos los mensajes además de los que vayan
llegando.
```commandline 
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic first_topic --from-beginning
Hola Mundo
Un mensaje que es confirmado
Seguimos aprendiendo
 mas mensajes
Aprendiendo kafka
Otro mensaje
Otro mensaje mas
Hola nuevo mensaje para el consumer
Mas texto para seguir aprendiendo
Mas mensajes de prueba

```
Como se puede apreciar nos devuelve todos los mensajes pero desordenados. 
Si añadimos un nuevo mensaje al topic, este aparecerá en consumer que tenemos abierto.

## Consumers Group

Ahora veremos el uso de los consumers group donde el group es el equivalente a lo que
sería el nombre de nuestra aplicación. Tiene a su vez la definición de grupo, ya que consiste en un grupo
de consumidores.
Esta ha de ser la forma de realizar el consumo de datos desde kafka por las aplicaciones
(por ejemplo spark streaming) cuando involucremos alto rendimiento, concurrencia, entre otras cosas.

En la consola donde estábamos ejecutando el consumer lanzamos el consumer que
forma parte de un consumer group
```commandline
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic first_topic --group my-first-application 
```
En la otra consola lanzamos de nuevo el producer para escribir en el first_topic y
empezamos a mandar mensajes.
```commandline
kafka-console-producer.sh --broker-list localhost:9092 --topic first_topic
```
Ahora abrimos otra terminal y lanzamos otro consumer con el mismo
grupo de consumo.
```commandline
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic first_topic --group my-first-application
```
Empezamos a lanzar mensajes desde la otra consola producer y vemos como los
consumidores van leyendo los mensajes, distribuyéndose estos mensajes entre los 2 consumidores.

Y podemos incluso crear otro consumer que forme parte del grupo y seguir lanzando 
mensajes y ver como los consumidores del grupo van leyendo los mensajes.

### Ejercicios propuestos

***1)*** ¿Qué pasaría si cancelamos (utilizando Ctrl+C) uno de los
consumidores (quedando 2) y seguimos enviando mensajes por el producer?
* Que los mensajes ahora se dividen entre los dos consumers que quedan.

***2)*** ¿Qué pasaría si cancelamos otro de los consumidores (quedando ya
solo 1) y seguimos enviando mensajes por el producer?
* Todos los mensajes irían a parar al consumer que quede.

***3)*** ¿Qué sucede si lanzamos otro consumidor, pero está vez de un grupo
llamado my-second-application leyendo el topic desde el principio (```--from-beginning```)?
```commandline
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic first_topic --group my-second-application --from-beginning
```
* Que el consumer lee todos los mensajes desde el principio. 
* El orden solo está asegurado entre los mensajes recogidos por el mismo consumer.

***4)*** Cancela el consumidor y ¿Qué sucede si lanzamos de nuevo el
consumidor, pero formando parte del grupo my-second-application?
¿Aparecen los mensajes desde el principio?
* No aparece ningún mensaje, ya que todos los mensajes ya han sido leídos por los otros consumers del grupo.

***5)*** Cancela el consumer, a su vez aprovecha de enviar más mensajes utilizando el producer y de nuevo
lanza el consumidor formando parte del grupo my-second_application ¿Cuál fue el resultado?
* Lee solamente los nuevos mensajes.

Por último exploraremos otro comando
```commandline
kafka-consumer-groups.sh
```
Una vez analizadas todas las opciones y argumentos que recibe, lanzamos el siguiente
comando
```commandline
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
my-first-application
my-second-application
```
que nos lista los grupos de consumo.

Podremos visualizar el offset por particiones
```commandline
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group my-second-application
```
```text
Consumer group 'my-second-application' has no active members.

GROUP                 TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
my-second-application first_topic     0          7               7               0               -               -               -
my-second-application first_topic     1          5               5               0               -               -               -
my-second-application first_topic     2          9               9               0               -               -               -
```
El comando nos informa si el grupo de consumidores tiene miembros activos y el número de offsets por partición.

Si ahora introducimos más mensajes en el topic y arrancamos un consumidor en el mismo grupo, 
los mensajes serán recogidos y se generarán offsets en las particiones a las que vayan a parar.

Resetearemos el offset del consumer group.
Para ello nos enfocaremos en la propiedad ```--reset-offsets```
```commandline
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group my-first-application --reset-offsets --to-earliest --execute --topic first_topic
```
```text
GROUP                          TOPIC                          PARTITION  NEW-OFFSET     
my-first-application           first_topic                    0          0              
my-first-application           first_topic                    1          0              
my-first-application           first_topic                    2          0            
```
¿Qué pasa si reiniciamos el consumidor?
```commandline
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic first_topic --group my-first-application
```
Al haber reseteado los offsets del grupo, el consumer lee todos los mensajes desde el principio.

Si visualizamos ahora los offsets:
```commandline
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe  --group my-first-application
```
```text
GROUP                TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
my-first-application first_topic     0          7               7               0               -               -               -
my-first-application first_topic     1          5               5               0               -               -               -
my-first-application first_topic     2          9               9               0               -               -               -
```
Probamos otra opción para mover el cursor. El comando ```--shift-by n``` mueve el offset n puestos para los mensajes
recibidos por cada partición. Vamos a probarlo:
```commandline 
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group my-first-application --reset-offsets --shift-by 2 --execute --topic first_topic
[2022-06-07 08:22:30,428] WARN New offset (9) is higher than latest offset for topic partition first_topic-0. Value will be set to 7 (kafka.admin.ConsumerGroupCommand$)
```
Como podemos ver, al mover los offsets hacia atrás, no tenemos nuevos resultados,
ya que no hay mensajes anteriores sin coger y las particiones no cambian. 

Ahora probamos a movernos hacia adelante en los offsets con ```--shift-by -2```:
```commandline 
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group my-first-application --reset-offsets --shift-by -2 --execute --topic first_topic
```
```text
GROUP                          TOPIC                          PARTITION  NEW-OFFSET     
my-first-application           first_topic                    0          5              
my-first-application           first_topic                    1          3              
my-first-application           first_topic                    2          7    
```
Podemos ver que el número de offsets para cada partición a disminuido en 2.

Probamos de nuevo lanzar el consumidor asociado al consumer group my-first-application y vemos los mensajes que muestra
de acuerdo a la modificación que realizamos del offset
```commandline
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic first_topic --group my-first-application
```
Los 2 mensajes más antiguos de cada partición desaparecen.

## Ejercicio Final
* Crear topic llamado "topic_app" con 3 particiones y replication-factor = 1.
```commandline
kafka-topics.sh --bootstrap-server localhost:9092 --topic topic_app --create --partitions 3 --replication-factor 1
Created topic topic_app.
```
* Crear un producer que inserte mensajes en el topic recién creado (topic_app).
```commandline
kafka-console-producer.sh --broker-list localhost:9092 --topic topic_app
```
* Crear 2 consumers que formen parte de un grupo de consumo llamado "my_app".
```commandline
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic topic_app --group my_app
```
* Interactuar con los 3 elementos creando mensajes en el producir y visualizar como
  estos son consumidos por los consumer:

```text 
Mensajes introducidos en el topic:
>mensaje 1
>mensaje 2
>mensaje 3
>mensaje 4
>mensaje 5
>mensaje 6
>mensaje 7
>mensaje 8
>mensaje 9
>
```
```text
Mensajes leídos por el primer consumer:
mensaje 1
mensaje 9
```
```text
Mensajes leídos por el segundo consumer:
mensaje 2
mensaje 3
mensaje 4
mensaje 5
mensaje 6
mensaje 7
mensaje 8

```
* Aplicar los comandos necesarios para listar los topics, grupos de consumo, así
  como describir cada uno de estos
```commandline
kafka-topics.sh --bootstrap-server localhost:9092 --list

first_topic
new_topic
topic_app
```
```commandline
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

my_app
my-first-application
my-second-application
```
```commandline 
kafka-topics.sh --bootstrap-server localhost:9092 --topic topic_app --describe

Topic: topic_app        TopicId: piwzH61eReiMTy2D3XtIsA PartitionCount: 3       ReplicationFactor: 1    Configs: segment.bytes=1073741824
        Topic: topic_app        Partition: 0    Leader: 2       Replicas: 2     Isr: 2
        Topic: topic_app        Partition: 1    Leader: 1       Replicas: 1     Isr: 1
        Topic: topic_app        Partition: 2    Leader: 0       Replicas: 0     Isr: 0
```
```commandline 
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe  --group my_app 

GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
my_app          topic_app       0          3               3               0               -               -               -
my_app          topic_app       1          4               4               0               -               -               -
my_app          topic_app       2          2               2               0               -               -               -
```
