# Play Framework, Scala and Pekko Upgrade

## Overview

This repository has been upgraded from Akka to Apache Pekko to address licensing changes. Akka changed its license from Apache 2.0 to Business Source License which has commercial restrictions. Apache Pekko is an open-source fork maintained by the Apache Software Foundation.

## Version Changes

- Play Framework: 2.7.2 to 3.0.5
- Scala: 2.12.11 to 2.13.12
- Actor Framework: Akka 2.5.22 to Apache Pekko 1.0.3
- Java: 11 (unchanged)

## Changes Made

### Dependency Updates

All Maven POM files have been updated to use:
- Scala 2.13.12
- Play Framework 3.0.5 with group ID org.playframework
- Apache Pekko 1.0.3 instead of Akka

External dependencies have been updated to use Scala 2.13 versions:
- graph-engine_2.13
- Other knowlg-core dependencies

### Code Changes

Package imports have been updated throughout the codebase:
- akka.actor to org.apache.pekko.actor
- akka.pattern to org.apache.pekko.pattern
- akka.testkit to org.apache.pekko.testkit
- play.libs.akka to play.libs.pekko
- scala.collection.JavaConverters to scala.jdk.CollectionConverters

Module configuration has been updated:
- AkkaGuiceSupport changed to PekkoGuiceSupport in AssessmentModule and TestModule

### Configuration Changes

All configuration files have been updated:
- akka configuration blocks changed to pekko
- akka.http settings changed to pekko.http
- akka.request_timeout changed to pekko.request_timeout

## Build Requirements

### Prerequisites

- Java 11
- Maven 3.x
- Access to knowlg-core dependencies compiled for Scala 2.13 with Pekko support

### Important Note

This upgrade requires that the knowlg-core repository dependencies (actor-core, graph-engine, platform-common, import-manager) are available in Scala 2.13 with Pekko 1.0.3 support. These must be built and installed before building this repository.

## Building

To build the project:

```
mvn clean install -DskipTests=true
```

To package the service:

```
cd assessment-api
mvn play2:dist -pl assessment-service
```

## Testing

Run unit tests:

```
mvn test
```

## Benefits of Upgrade

- License compliance with Apache 2.0
- Security updates and CVE fixes
- Modern framework features
- Performance improvements from Scala 2.13 and Play 3.x
- Long-term support from Apache Foundation

## Known Issues

The build requires knowlg-core dependencies to be upgraded to Scala 2.13 with Pekko 1.0.3 first. Without these dependencies, the build will fail with dependency resolution errors.

## Additional Information

For questions or issues related to this upgrade, please refer to the official documentation:
- Apache Pekko: https://pekko.apache.org/
- Play Framework 3.x: https://www.playframework.com/documentation/3.0.x/
- Scala 2.13: https://docs.scala-lang.org/
