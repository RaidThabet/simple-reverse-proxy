# Simple Reverse Proxy

A very simple reverse proxy built with **Java** and **Netty**.

## Main job
This server listens on a specific port 8080 (currently hardcoded) and acts as a traffic cop for APIs.
Currently it routes traffic based on 2 hardcoded paths.

## Tech Stack
* **Java 25** as the programming language
* **Netty 4.2** as the async event-driven network application framework
* **Maven** for dependency management and building
* **SLF4J** for logging