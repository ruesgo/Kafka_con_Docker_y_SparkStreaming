# Práctica II

En esta práctica utilizaremos Presto para visualizar topics como tablas y realizar querys sencillas.

Se ha utilizado la imagen ```ahanaio/prestodb-sandbox```.

Lo primero es arrancar el cluster kafka. Una vez arrancado el cluster creamos un nuevo topic llamado ```text_topic``` y 
le insertamos unos cuantos mensajes con un producer:
```commandline
kafka-topics.sh --bootstrap-server localhost:9092 --topic text_topic --create --partitions 3 --replication-factor 2
Created topic text_topic.
```
```commandline 
kafka-console-producer.sh --broker-list localhost:9092 --topic text_topic
>Hola mundo
>Hola mundo de nuevo
>Tecrera frase de texto
>Cuarta y ultima
>
```
Revisamos el contenido del topic con un consumer
```commandline 
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic text_topic --from-beginning
Hola mundo
Hola mundo de nuevo
Tecrera frase de texto
Cuarta y ultima
```

Ahora abrimos un terminal en el servidor de Presto. Tendremos que crear un archivo kafka.properties en la ruta ```/etc/catalog/kafka.properties```.
En este fichero indicamos los nombres de tablas, el nombre de conexión y los nodos de kafka 
(en nuestro caso nuestro cluster consta de un único nodo) y si han de ocultarse las columnas internas. 

Este archivo tendrá este aspecto:
```properties
connector.name=kafka
kafka.nodes=k-0:9092,k-1:9092,k-2:9092
kafka.table-names=text_topic
kafka.hide-internal-columns=false
```
Vamos a iniciar el servidor de Presto. Para esto en mi caso me muevo al directorio ```/opt/presto-server```
```commandline
cd /opt/presto-server
```
y ejecuto el siguiente comando:
```commandline
bin/launcher start
```
nos saldrá un mensaje de que el servidor ha arrancado.

Para ver el estado der servidor:
```commandline
bin/launcher status
```
También tenemos las opciones stop y restart que nos serán útiles cuando cambiemos los archivos de configuración. Como nota, al ejecutar estos comandos, 
Los terminales utilizados desde IntelliJ se desvinculan del servidor. 
Lo que mejor me ha funcionado para que los cambios surtan efecto es reiniciar el servidor y abrir un nuevo terminal para empezar a trabajar con Presto.

Ahora lanzamos el cliente Presto utilizando el kafka catalog y con el esquema por defecto:
```commandline 
/opt/presto-cli --catalog kafka --schema default
```
```commandline 
presto:default>
```
Dentro del cliente de Presto listamos las tablas en el catálogo y pedimos que describa el topic text_topic:
```text
presto:default> SHOW TABLES;
   Table    
------------
 text_topic 
```
```text
presto:default> DESCRIBE text_topic;
      Column       |  Type   | Extra |                   Comment                   
-------------------+---------+-------+---------------------------------------------
 _partition_id     | bigint  |       | Partition Id                                
 _partition_offset | bigint  |       | Offset for the message within the partition 
 _message_corrupt  | boolean |       | Message data is corrupt                     
 _message          | varchar |       | Message text                                
 _message_length   | bigint  |       | Total number of message bytes  
 _key_corrupt      | boolean |       | Key data is corrupt                         
 _key              | varchar |       | Key text                                    
 _key_length       | bigint  |       | Total number of key bytes                   
 _timestamp        | bigint  |       | Offset Timestamp    
```
Al hacer el DESCRIBE únicamente se listan las columnas internas (relacionadas con kafka).

Consultamos el topic y vemos los mensajes
```text
presto:default> SELECT _message FROM text_topic;
        _message        
------------------------
 Cuarta y ultima        
 Hola mundo             
 Tecrera frase de texto 
 Hola mundo de nuevo    
```
Pulsamos enter para ver los resultados completos, pulsamos q para salir de la consulta.
Una vez hemos comprobado que se ha listado todo el contenido del topic, 
desde el terminal donde tenemos el producer introduciremos más texto
```commandline 
kafka-console-producer.sh --broker-list localhost:9092 --topic text_topic
>Hola mundo
>Hola mundo de nuevo
>Tecrera frase de texto
>Cuarta y ultima
>Quinta
>Sexta a ver si aparece en presto
>
```
y posteriormente desde la otra consola donde tenemos el cliente de consulta de presto
volveremos a consultar el topic
```text
presto:default> SELECT _message FROM text_topic;
             _message             
----------------------------------
 Hola mundo de nuevo              
 Quinta                           
 Hola mundo                       
 Tecrera frase de texto           
 Cuarta y ultima                  
 Sexta a ver si aparece en presto 
(6 rows)
(END)
```
## Leer CSV
Crearemos un nuevo topic llamado csv_topic cuyos datos serán transmitidos en formato csv.
```commandline
kafka-topics.sh --bootstrap-server localhost:9092 --topic csv_topic --create --partitions 3 --replication-factor 2
```
Añadimos un elemento al topic. Hay que tener muchísimo cuidado con las comillas.
```commandline 
kafka-console-producer.sh --broker-list localhost:9092 --topic csv_topic
>"JOSE ANTONIO","38","75.5","C/ LIMONERO 39, 1-C"
```
En la consola donde hemos arrancado el servidor presto, detendremos el servicio. Ahora tenemos que actualizar kafka.properties
para que incluya el topic recién creado, tendrá que quedar más o menos de la siguente manera:
```properties
connector.name=kafka
kafka.nodes=k-0:9092,k-1:9092,k-2:9092
kafka.table-names=text_topic,csv_topic
kafka.hide-internal-columns=true
```
A partir de ahora no mostraremos las columnas ocultas.

Ahora bien, lo interesante será poder consultar por el valor de alguno de los campos
delimitados, por ende el siguiente paso será llevar a cabo la definición de la tabla, para ello
seguiremos los siguientes pasos:

1.- Detener el servidor presto

2.- Crear un nuevo fichero JSON con la estructura de la tabla

3.- Reiniciar el servidor presto para que tome los cambios

El fichero JSON debe ir en la ruta del servidor presto ```/etc/kafka```. 
Por comodidad se crea un volumen que apunte a esa ruta, al igual que con la carpeta ```/etc/catalog```. 
Esto nos permite, con el contenedor presto apagado, configurar las propiedades de la conexión 
o inducir estructuras de tablas para que estén al iniciar el servicio.

Creamos el archivo csv_topic.json en la carpeta anterior. Debería tener el siguiente aspecto:
```json
{
  "tableName": "csv_topic",
  "topicName": "csv_topic",
  "schemaName": "default",
  "message": {
    "dataFormat":"csv",
    "fields": [
      {
        "name": "nombre",
        "mapping": 0,
        "type": "VARCHAR"
      },
      {
        "name": "edad",
        "mapping": 1,
        "type": "INT"
      },
      {
        "name": "peso",
        "mapping": 2,
        "type": "DOUBLE"
      },
      {
        "name": "direccion",
        "mapping": 3,
        "type": "VARCHAR"
      }
    ]
  }
}
```
Desde el cliente presto de consulta por línea de comandos veremos las características
de la tabla y deberíamos observar los nuevos campos definidos en el fichero json_topic.json

```text
presto:default> DESCRIBE csv_topic;
  Column   |  Type   | Extra | Comment 
-----------+---------+-------+---------
 nombre    | varchar |       |         
 edad      | integer |       |         
 peso      | double  |       |         
 direccion | varchar |       |     
 ```
Realizamos un select:
```text
presto:default> select * from csv_topic;
    nombre    | edad | peso |      direccion      
--------------+------+------+---------------------
 JOSE ANTONIO |   38 | 75.5 | C/ LIMONERO 39, 1-C 
```
Añadimos alguna fila más a través del producer y volvemos a hacer el select:
```text
presto:default> select * from csv_topic;
    nombre    | edad | peso |          direccion          
--------------+------+------+-----------------------------
 CARLOS       |   26 | 80.0 | AVDA DE LA DIRECCION, 2-I 
 JOSE ANTONIO |   38 | 75.5 | C/ LIMONERO 39, 1-C         
 RUBEN        |   25 | 95.0 | AVDA DE LA DIRECCION, 2-I 
```
Ahora podemos hacer una query más elaborada 
```text
presto:default> select * from csv_topic where peso > 79;
 nombre | edad | peso |          direccion          
--------+------+------+-----------------------------
 RUBEN  |   25 | 95.0 | AVDA DE LA DIRECCION, 2-I   
 CARLOS |   26 | 80.0 | AVDA DE LA DIRECCION, 2-I
```

## Leer JSON

Ahora haremos lo mismo pero insertando elementos en formato json.

Primero crearemos un nuevo topic json_topic:
```commandline
kafka-topics.sh --bootstrap-server localhost:9092 --topic json_topic --create --partitions 3 --replication-factor 2
```
Añadimos un elemento al topic. Hay que tener muchísimo cuidado con las comillas.
```commandline 
kafka-console-producer.sh --broker-list localhost:9092 --topic json_topic
>{"nombre":"JOSE ANTONIO","edad":38,"peso":75.5,"direccion":"C/ DEL LIMONERO 39, PISO 1-A"}
```
Antes de iniciar el servidor de presto tendremos que hacer un par de cosas:
* Tenemos que actualizar el archivo kafka.properties para añadir el nuevo topic
```properties
connector.name=kafka
kafka.nodes=k-0:9092,k-1:9092,k-2:9092
kafka.table-names=text_topic,csv_topic,json_topic
kafka.hide-internal-columns=true
```
* Tenemos que añadir el archivo ```json_topic.json``` con la estructura de la tabla en la ruta 
```/etc/kafka``` en el servidor de presto. Tendrá el siguiente aspecto:
```json
{
  "tableName": "json_topic",
  "topicName": "json_topic",
  "schemaName": "default",
  "message": {
    "dataFormat":"json",
    "fields": [
      {
        "name": "nombre",
        "mapping": "nombre",
        "type": "VARCHAR"
      },
      {
        "name": "edad",
        "mapping": "edad",
        "type": "INT"
      },
      {
        "name": "peso",
        "mapping": "peso",
        "type": "DOUBLE"
      },
      {
        "name": "direccion",
        "mapping": "direccion",
        "type": "VARCHAR"
      }
    ]
  }
}
```
Ahora si reiniciamos el servidor presto ya tendremos listo nuestro topic para trabajar con el:
```text
presto:default> SHOW TABLES;
   Table    
------------
 csv_topic  
 json_topic 
 text_topic 
 
presto:default> DESCRIBE json_topic;
  Column   |  Type   | Extra | Comment 
-----------+---------+-------+---------
 nombre    | varchar |       |         
 edad      | integer |       |         
 peso      | double  |       |         
 direccion | varchar |       |         
 
 presto:default> select * from json_topic;
    nombre    | edad | peso |          direccion           
--------------+------+------+------------------------------
 JOSE ANTONIO |   38 | 75.5 | C/ DEL LIMONERO 39, PISO 1-A 
```
## Ejercicio
Como ejercicio crearemos un nuevo topic que maneje datos con formato JSON con variedad de
atributos (que tengan cadenas de caracteres, enteros, y tipos de datos reales/flotantes).
Insertaremos varios registros (al menos unos 6-10 registros), generaremos una tabla en presto asociada a ese topic y
lanzaremos consultas sobre el topic (que incluso podría estar en una continua evolución, asumiendo
que sigan insertando elementos) con condiciones (cláusulas WHERE) basadas en algunos de los
atributos que contienen dichos JSON.

En mi caso crearé un topic con la información sobre los goleadores de la liga ASOBAL de balonmano.
Lo primero es crear el topic:
```commandline
kafka-topics.sh --bootstrap-server localhost:9092 --topic goleadores --create --partitions 3 --replication-factor 2
```
Añadimos el topic al archivo ```kafka.properties```:
```properties
connector.name=kafka
kafka.nodes=k-0:9092,k-1:9092,k-2:9092
kafka.table-names=text_topic,csv_topic,json_topic,goleadores
kafka.hide-internal-columns=true
```
Creamos el archivo ```goleadores.json``` en el que se guarda el esquema con el que presto lee el topic:
```json
{
  "tableName": "goleadores",
  "topicName": "goleadores",
  "schemaName": "default",
  "message": {
    "dataFormat":"json",
    "fields": [
      {
        "name": "jugador",
        "mapping": "jugador",
        "type": "VARCHAR"
      },
      {
        "name": "equipo",
        "mapping": "equipo",
        "type": "VARCHAR"
      },
      {
        "name": "goles",
        "mapping": "goles",
        "type": "INT"
      },
      {
        "name": "goles_por_partido",
        "mapping": "goles_por_partido",
        "type": "DOUBLE"
      }
    ]
  }
}
```
Ahora introduciremos algunos registros:
```commandline 
kafka-console-producer.sh --broker-list localhost:9092 --topic goleadores
>{"jugador":"JOSE MARIA MARQUEZ COLOMA","equipo":"FRAIKIN BM. GRANOLLERS","goles":207,"goles_por_partido":6.9}
>{"jugador":"GONZALO PEREZ ARCE","equipo":"ABANCA ADEMAR LEON","goles":200,"goles_por_partido":6.67}
>{"jugador":"THIAGO ALVES PONCIANO","equipo":"INCARLOPSA CUENCA","goles":169,"goles_por_partido":5.63}
>{"jugador":"DAVID IGLESIAS ESTEVEZ","equipo":"CLUB BALONMAN CANGAS","goles":156,"goles_por_partido":5.2}
>{"jugador":"FRANCISCO JAVIER CASTRO PENA","equipo":"UNICAJA BANCO SINFIN","goles":155,"goles_por_partido":5.17}
>{"jugador":"POL VALERA ROVIRA","equipo":"FRAIKIN BM. GRANOLLERS","goles":154,"goles_por_partido":5.31}
>{"jugador":"JORGE SERRANO VILLALOBOS","equipo":"RECOLETAS ATLETICO VALLADOLID","goles":154,"goles_por_partido":7}
>{"jugador":"AGUSTIN CASADO MARCELO","equipo":"BM LOGRONO LA RIOJA","goles":151,"goles_por_partido":5.59}
```
Por último reiniciamos el servidor presto y realizamos alguna query:
```text
bash-4.2# /opt/presto-cli --catalog kafka --schema default
presto:default> show tables;
   Table    
------------
 csv_topic  
 goleadores 
 json_topic 
 text_topic 
(4 rows)

Query 20220607_184712_00002_x936d, FINISHED, 1 node
Splits: 19 total, 19 done (100.00%)
278ms [4 rows, 107B] [14 rows/s, 385B/s]

presto:default> describe goleadores;
      Column       |  Type   | Extra | Comment 
-------------------+---------+-------+---------
 jugador           | varchar |       |         
 equipo            | varchar |       |         
 goles             | integer |       |         
 goles_por_partido | double  |       |         
(4 rows)

Query 20220607_184727_00003_x936d, FINISHED, 1 node
Splits: 19 total, 19 done (100.00%)
351ms [4 rows, 286B] [11 rows/s, 814B/s]

presto:default> select * from goleadores;
           jugador            |            equipo             | goles | goles_por_partido 
------------------------------+-------------------------------+-------+-------------------
 GONZALO PEREZ ARCE           | ABANCA ADEMAR LEON            |   200 |              6.67 
 POL VALERA ROVIRA            | FRAIKIN BM. GRANOLLERS        |   154 |              5.31 
 DAVID IGLESIAS ESTEVEZ       | CLUB BALONMAN CANGAS          |   156 |               5.2 
 AGUSTIN CASADO MARCELO       | BM LOGRONO LA RIOJA           |   151 |              5.59 
 JOSE MARIA MARQUEZ COLOMA    | FRAIKIN BM. GRANOLLERS        |   207 |               6.9 
 THIAGO ALVES PONCIANO        | INCARLOPSA CUENCA             |   169 |              5.63 
 FRANCISCO JAVIER CASTRO PENA | UNICAJA BANCO SINFIN          |   155 |              5.17 
 JORGE SERRANO VILLALOBOS     | RECOLETAS ATLETICO VALLADOLID |   154 |               7.0 
(8 rows)

Query 20220607_184753_00004_x936d, FINISHED, 1 node
Splits: 19 total, 19 done (100.00%)
0:01 [8 rows, 847B] [12 rows/s, 1.33KB/s]

presto:default> select * from goleadores where goles<200 and goles_por_partido>5.5;
         jugador          |            equipo             | goles | goles_por_partido 
--------------------------+-------------------------------+-------+-------------------
 AGUSTIN CASADO MARCELO   | BM LOGRONO LA RIOJA           |   151 |              5.59 
 THIAGO ALVES PONCIANO    | INCARLOPSA CUENCA             |   169 |              5.63 
 JORGE SERRANO VILLALOBOS | RECOLETAS ATLETICO VALLADOLID |   154 |               7.0 
(3 rows)

Query 20220607_185040_00005_x936d, FINISHED, 1 node
Splits: 19 total, 19 done (100.00%)
347ms [8 rows, 847B] [23 rows/s, 2.38KB/s]

presto:default> select * from goleadores where equipo like '%%GRANOLLERS';
          jugador          |         equipo         | goles | goles_por_partido 
---------------------------+------------------------+-------+-------------------
 POL VALERA ROVIRA         | FRAIKIN BM. GRANOLLERS |   154 |              5.31 
 JOSE MARIA MARQUEZ COLOMA | FRAIKIN BM. GRANOLLERS |   207 |               6.9 
(2 rows)

Query 20220607_185556_00010_x936d, FINISHED, 1 node
Splits: 19 total, 19 done (100.00%)
351ms [8 rows, 847B] [22 rows/s, 2.35KB/s]

presto:default> 
```