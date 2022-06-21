Esta práctica consiste en la gestión y el procesamiento de flujos de datos
por medio de Kafka. 

Se ha creado un cluster Kafka con tres brokers y un servidor Zookeeper mediante un docker compose. 
A esta network se le ha añadido un contenedor con una imagen de Presto para realizar la segunda práctica.

Una vez creado el docker compose se ha trabajado en IntelliJ. Para la conexión con el cluster se ha configurado un listener 
hacia el exterior en cada broker.