# Performance comparison between HTTP and MQTT protocols for mobile applications

## Problem
Several mobile applications for industry usage requires to transmit data with a very high frequency. A typical use case are mobile applications used for movement monitoring, in which it is needed to trasmit the position of a device within intervals of 1-2 seconds. In this situtation, it is critical to optimize as much as possible data trasmission in order to prevent an excessive consumption in terms of bandwidth and power.
<p>The same problem occurs in the many IoT scenarios, in which the device may not be a smartphone or a tablet but an Arduino board or Raspberry PI. The problem is even more pronounced in cases in which several devices have to synchronize their status based on the data trasmitted by one of them to the server (e.g. when it is needed to know the position of all and show it on a real time map).</p>
<p>In this context the usage of the HTTP protocol (through REST services) results not optimal, for this reason, nowadays machine-2-machine protocols are becoming more popular in the IoT context.
One of the most promising protocols is MQTT, a protocol based on a publish/subscribe comunication, in which  it is possible to receive and send messages under a certain topic. In this way, it is possible to overcome the problems due to periodic polling that would have been needed for a HTTP comunication through REST services.</p>
<p>The goal of this thesis project is to verify the correct operation of the MQTT protocol through a real use case scenario, a mobile application that allows to visualize and monitor the position of different devices in a real time map and transmit its own position to a server.
<br>The architecture of the project consists on a Java server (that works as Web Server implemented with Spring Boot), a MQTT broker created with Mosquitto, an Android application that can comunicate with the HTTP server or MQTT broker. The MQTT comunication is based on the open source library Eclipse/PAHO and the HTTP comunication is based on a set of REST services that allow to send the own position and retreive the position of other devices.</p>

## Solution
This repository contains an android application that allows to connect to a HTTP server or a MQTT server.
The application is essentially based on 5 java classes and uses:
- MQTT Eclipse/PAHO
- GoogleMap API

The java classes are:
- MyClient: an abstract class that contains all the information common to MyHttpClient and MyMqttClient
- MyHttpClient: a class that allows to send the own position of the User to <i>http://server_ip:port/clients/positions</i> and requests every 1s the position of all the other clients to http://server_ip:port/get-all-position
- MyMqttClient: a class that uses Eclipse/PAHO library to send the own position to <i>tcp://server_ip:port/</i>, subscribes to the topic <i>test_posizione/#</i> and sends its own position to <i>test_posizione/client_id</i>
- MainActivity: an Activity that allows to select a protocol comunication (HTTP or MQTT), connect to the chosen protocol with a given id, server_ip and port. And this activity shows the battery used when comunication started and when the comunication was stopped.
- MapsActivity: this activity uses the GoogleMap API to show the current position of all the clients. The clients can be HTTP or MQTT. Furthermore, everytime the position of the current User changes this activity allows to call the method <i>sendPosition()</i> that sends to the server or broker the current position of the current user.
## Usage
1. Download the apk or generate the apk from the source code
2. Open the app
3. Fill the form as in the following images
<p align="center">
<img align="center" src="https://github.com/StevenSalazarM/Performance-comparison-http-mqtt/blob/master/Results/funzionamento_0.jpg" width=70% heigh=70%></img></p>
<br><br><br>
4. Change your current position by moving (you are the blue point)
<br><br><br>
<p align="center"><img align="center" src="https://github.com/StevenSalazarM/Performance-comparison-http-mqtt/blob/master/Results/funzionamento_3.jpg" width=35% heigh=35%></img></p>
<br><br><br>
5. Go back the Main Activity to see the battery used
<br><br><br>
<p align="center"><img align="center" src="https://github.com/StevenSalazarM/Performance-comparison-http-mqtt/blob/master/Results/funzionamento_4.jpg" width=35% heigh=35%></img></p>

## Results
A complete report of all the results obtained can be found in the [thesis pdf](https://github.com/StevenSalazarM/Performance-comparison-http-mqtt/blob/master/Results/Tesi.pdf) (currently in italian but may be translated soon)
### Bandwidth
The following image shows the result obtained measuring the Input/Ouput packets rate of the application for each protocol. The blue graph is related to the HTTP protocol and the red graph to MQTT protocol.

<p align="center"><img align="center" src="https://github.com/StevenSalazarM/Performance-comparison-http-mqtt/blob/master/Results/banda.png" width=90% heigh=90%></img></p>

### Battery
The following image was obtained with a python script that considered the battery usage of 16 tests (5 hours per test) for each protocol. The tests results can be found in the Results directory.
<p align="center"><img align="center" src="https://github.com/StevenSalazarM/Performance-comparison-http-mqtt/blob/master/Results/confronto_batteria.png" width=70% heigh=70%></img></p>

It may look that there is not a big difference in terms of battery usage, for this reason another graph was considered. The following image contains the difference of battery usage between HTTP and MQTT.
<p align="center"><img align="center" src="https://github.com/StevenSalazarM/Performance-comparison-http-mqtt/blob/master/Results/differenza_http_mqtt_batteria.png" width=70% heigh=70%></img></p>

### Data usage
This is the most important metric because in a real world scenario, data usage is what will cost more for companies. For example some companies that provides internet for IoT devices charge costs depending on the quantity of data used in terms of Bytes (or Gigabytes).
<p align="center"><img align="center" src="https://github.com/StevenSalazarM/Performance-comparison-http-mqtt/blob/master/Results/confronto_dati.png" width=70% heigh=70%></img></p>


## Acknowledgements
I express my sincere gratitude to Walter Nunziati (Co-Founder of Magenta Software Lab) and Alessandro Fantechi (President of the School of Engineering  at Università degli Studi di Firenze). Without their supervision and help I wouldn't have been able to complete this project.


## Author
Steven Alexander Salazar Molina.
