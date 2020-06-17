# Overview

This project contains all the implementations in Java for the newly designed Metal Rentals. 
It is built by Maven and implemented using JDK 11. 

The original implementation was done using IDE IntelliJ IDEA, 
however any other Java IDE can be used except some comments in the code are only supported by IntelliJ IDEA.

**Please noticed that in order to fit into the existing code repository layout,
files might be duplicated from this project in other folders in the repository.
However, this project is the only golden source when coming to Metal Rentals implementation.**

# Project Structure

This project contains the following modules:

* **endur-connector**: 
Component "Endur Connector" which publish all the needed data within Endur via REST API to external applications.

* **endur-scripts**:
This module contains all the Endur scripts to be imported, including the one starting "Endur Connector"

* **metal-rentals-service**:
Component "Metal Rental Service" which offers REST APIs (where all the business logic lies) for the frontend.
This service will be run as Windows Service and the extra wrapper is under "deployment" folder.

* **mock-endur-connector**:
A dummy implementation of "Endur Connector" which doesn't need Endur connection, 
so it can be used for developing "Metal Rentals Service" without Endur access needed

* **metal-rentals-common**:
Common code needed among the above modules, especially the data models

* **distribution**:
This module doesn't contain any code - it is designated for building the distribution

Please noted that any code to be used within Endur must be complied to Java 7 level due to Endur v14 at the moment.
This includes "metal-rentals-common", "endur-connector", and "endur-scripts".

Under each module folder, if there is a "deployment" folder, then it is the extra files needed for distribution.

Other than the above modules, there are extra folders as well:

* **endur_configurations**:
The Endur CMM package needed for this design

* **sql**:
User table creation

# Build & Deployment

To build the final distribution, simply run "mvn clean package".
The distribution can be found under "distribution -> target -> distribution-1.0.0-dist".

Please noted that for the final distribution to be used by emdash for deployment, 
there are two extra bits not belong to this project but needed:

* **JDK binary under "metal-rental-service"**: 
It must be JDK 11+, the folder name is referred in "metal-rentals-service -> deployment -> MetalRentalsService.xml"
* **Metal Rentals UI distribution ("metal-rentals-service -> static")**: 
Metal Rentals UI can be hosted separately.
However, in the current configurations, it is hosted together within "Metal Rentals Service".
