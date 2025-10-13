# Migration Code Examples & Patterns

This document provides specific code examples for the Play 3.0.5 + Scala 2.13.12 + Pekko 1.0.3 migration.

---

## Table of Contents
1. [POM File Changes](#pom-file-changes)
2. [Import Statement Changes](#import-statement-changes)
3. [Module Configuration](#module-configuration)
4. [Controller Updates](#controller-updates)
5. [Actor System Changes](#actor-system-changes)
6. [Configuration File Changes](#configuration-file-changes)
7. [Test Updates](#test-updates)
8. [Common Issues and Solutions](#common-issues-and-solutions)

---

## POM File Changes

### Root POM (pom.xml)

**BEFORE:**
```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <scoverage.plugin.version>1.4.1</scoverage.plugin.version>
    <scala.maj.version>2.12</scala.maj.version>
    <scala.version>2.12.11</scala.version>
    <scalatest.version>3.0.8</scalatest.version>
    <fasterxml.jackson.version>2.9.8</fasterxml.jackson.version>
</properties>
```

**AFTER:**
```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <scoverage.plugin.version>1.4.1</scoverage.plugin.version>
    <scala.maj.version>2.13</scala.maj.version>
    <scala.version>2.13.12</scala.version>
    <scalatest.version>3.2.17</scalatest.version>
    <fasterxml.jackson.version>2.15.3</fasterxml.jackson.version>
</properties>
```

### Assessment Service POM (assessment-api/assessment-service/pom.xml)

**BEFORE:**
```xml
<properties>
    <play2.version>2.7.2</play2.version>
    <play2.plugin.version>1.0.0-rc5</play2.plugin.version>
    <sbt-compiler.plugin.version>1.0.0</sbt-compiler.plugin.version>
</properties>

<dependencies>
    <dependency>
        <groupId>com.typesafe.play</groupId>
        <artifactId>play_${scala.major.version}</artifactId>
        <version>${play2.version}</version>
    </dependency>
    <dependency>
        <groupId>com.typesafe.play</groupId>
        <artifactId>play-guice_${scala.major.version}</artifactId>
        <version>${play2.version}</version>
    </dependency>
</dependencies>
```

**AFTER:**
```xml
<properties>
    <play2.version>3.0.5</play2.version>
    <play2.plugin.version>1.0.0-rc5</play2.plugin.version>
    <sbt-compiler.plugin.version>1.0.0</sbt-compiler.plugin.version>
</properties>

<dependencies>
    <!-- Note: Some Play 3.x artifacts may use org.playframework group ID -->
    <dependency>
        <groupId>org.playframework</groupId>
        <artifactId>play_${scala.major.version}</artifactId>
        <version>${play2.version}</version>
    </dependency>
    <dependency>
        <groupId>org.playframework</groupId>
        <artifactId>play-guice_${scala.major.version}</artifactId>
        <version>${play2.version}</version>
    </dependency>
</dependencies>
```

### Assessment Actors POM (assessment-api/assessment-actors/pom.xml)

**BEFORE:**
```xml
<dependencies>
    <dependency>
        <groupId>org.sunbird</groupId>
        <artifactId>graph-engine_2.12</artifactId>
        <version>1.0-SNAPSHOT</version>
        <type>jar</type>
    </dependency>
    <dependency>
        <groupId>com.typesafe.akka</groupId>
        <artifactId>akka-testkit_${scala.maj.version}</artifactId>
        <version>2.5.22</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**AFTER:**
```xml
<dependencies>
    <dependency>
        <groupId>org.sunbird</groupId>
        <artifactId>graph-engine_2.13</artifactId>
        <version>1.0-SNAPSHOT</version>
        <type>jar</type>
    </dependency>
    <dependency>
        <groupId>org.apache.pekko</groupId>
        <artifactId>pekko-testkit_${scala.maj.version}</artifactId>
        <version>1.0.3</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Import Statement Changes

### Akka to Pekko Imports

**BEFORE (Akka):**
```scala
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.Patterns
import akka.testkit.{TestKit, TestProbe}
import akka.util.Timeout
```

**AFTER (Pekko):**
```scala
import org.apache.pekko.actor.{ActorRef, ActorSystem, Props}
import org.apache.pekko.pattern.Patterns
import org.apache.pekko.testkit.{TestKit, TestProbe}
import org.apache.pekko.util.Timeout
```

### Scala JavaConverters

**BEFORE (Scala 2.12):**
```scala
import scala.collection.JavaConverters._

// Usage
val javaList = scalaList.asJava
val scalaList = javaList.asScala
```

**AFTER (Scala 2.13):**
```scala
import scala.jdk.CollectionConverters._

// Usage (same)
val javaList = scalaList.asJava
val scalaList = javaList.asScala
```

### Play Framework Imports

**BEFORE (Play 2.7):**
```scala
import play.api.mvc._
import play.api.libs.json._
import play.libs.akka.AkkaGuiceSupport
```

**AFTER (Play 3.0):**
```scala
import play.api.mvc._
import play.api.libs.json._
import play.libs.pekko.PekkoGuiceSupport  // Changed!
```

---

## Module Configuration

### AssessmentModule.scala

**Location:** `assessment-api/assessment-service/app/modules/AssessmentModule.scala`

**BEFORE:**
```scala
package modules

import com.google.inject.AbstractModule
import org.sunbird.actors.{HealthActor, ItemSetActor, QuestionActor, QuestionSetActor}
import play.libs.akka.AkkaGuiceSupport
import utils.ActorNames

class AssessmentModule extends AbstractModule with AkkaGuiceSupport {

    override def configure() = {
        bindActor(classOf[HealthActor], ActorNames.HEALTH_ACTOR)
        bindActor(classOf[ItemSetActor], ActorNames.ITEM_SET_ACTOR)
        bindActor(classOf[QuestionActor], ActorNames.QUESTION_ACTOR)
        bindActor(classOf[QuestionSetActor], ActorNames.QUESTION_SET_ACTOR)
        bindActor(classOf[org.sunbird.v5.actors.QuestionActor], ActorNames.QUESTION_V5_ACTOR)
        bindActor(classOf[org.sunbird.v5.actors.QuestionSetActor], ActorNames.QUESTION_SET_V5_ACTOR)
        println("Initialized application actors for assessment-service")
    }
}
```

**AFTER:**
```scala
package modules

import com.google.inject.AbstractModule
import org.sunbird.actors.{HealthActor, ItemSetActor, QuestionActor, QuestionSetActor}
import play.libs.pekko.PekkoGuiceSupport  // CHANGED
import utils.ActorNames

class AssessmentModule extends AbstractModule with PekkoGuiceSupport {  // CHANGED

    override def configure() = {
        bindActor(classOf[HealthActor], ActorNames.HEALTH_ACTOR)
        bindActor(classOf[ItemSetActor], ActorNames.ITEM_SET_ACTOR)
        bindActor(classOf[QuestionActor], ActorNames.QUESTION_ACTOR)
        bindActor(classOf[QuestionSetActor], ActorNames.QUESTION_SET_ACTOR)
        bindActor(classOf[org.sunbird.v5.actors.QuestionActor], ActorNames.QUESTION_V5_ACTOR)
        bindActor(classOf[org.sunbird.v5.actors.QuestionSetActor], ActorNames.QUESTION_SET_V5_ACTOR)
        println("Initialized application actors for assessment-service")
    }
}
```

### TestModule.scala

**Location:** `assessment-api/assessment-service/test/modules/TestModule.scala`

**BEFORE:**
```scala
package modules

import com.google.inject.AbstractModule
import org.sunbird.actor.core.BaseActor
import org.sunbird.common.dto.{Request, Response, ResponseHandler}
import play.libs.akka.AkkaGuiceSupport
import utils.ActorNames

import scala.concurrent.{ExecutionContext, Future}

class TestModule extends AbstractModule with AkkaGuiceSupport {
    override def configure(): Unit = {
        bindActor(classOf[TestActor], ActorNames.HEALTH_ACTOR)
        bindActor(classOf[TestActor], ActorNames.ITEM_SET_ACTOR)
        bindActor(classOf[TestActor], ActorNames.QUESTION_ACTOR)
        bindActor(classOf[TestActor], ActorNames.QUESTION_SET_ACTOR)
        bindActor(classOf[TestActor], ActorNames.QUESTION_V5_ACTOR)
        bindActor(classOf[TestActor], ActorNames.QUESTION_SET_V5_ACTOR)
        println("Test Module is initialized...")
    }
}
```

**AFTER:**
```scala
package modules

import com.google.inject.AbstractModule
import org.sunbird.actor.core.BaseActor
import org.sunbird.common.dto.{Request, Response, ResponseHandler}
import play.libs.pekko.PekkoGuiceSupport  // CHANGED
import utils.ActorNames

import scala.concurrent.{ExecutionContext, Future}

class TestModule extends AbstractModule with PekkoGuiceSupport {  // CHANGED
    override def configure(): Unit = {
        bindActor(classOf[TestActor], ActorNames.HEALTH_ACTOR)
        bindActor(classOf[TestActor], ActorNames.ITEM_SET_ACTOR)
        bindActor(classOf[TestActor], ActorNames.QUESTION_ACTOR)
        bindActor(classOf[TestActor], ActorNames.QUESTION_SET_ACTOR)
        bindActor(classOf[TestActor], ActorNames.QUESTION_V5_ACTOR)
        bindActor(classOf[TestActor], ActorNames.QUESTION_SET_V5_ACTOR)
        println("Test Module is initialized...")
    }
}
```

---

## Controller Updates

### BaseController.scala

**Location:** `assessment-api/assessment-service/app/controllers/BaseController.scala`

**BEFORE:**
```scala
package controllers

import java.util.UUID
import akka.actor.ActorRef
import akka.pattern.Patterns
import org.sunbird.common.DateUtils
import org.sunbird.common.dto.{Response, ResponseHandler}
import org.sunbird.common.exception.ResponseCode
import org.sunbird.telemetry.logger.TelemetryManager
import play.api.mvc._
import utils.JavaJsonUtils

import collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

abstract class BaseController(protected val cc: ControllerComponents)(implicit exec: ExecutionContext) extends AbstractController(cc) {
    
    def getResult(apiId: String, actor: ActorRef, request: org.sunbird.common.dto.Request) : Future[Result] = {
        val future = Patterns.ask(actor, request, 30000) recoverWith {case e: Exception => Future(ResponseHandler.getErrorResponse(e))}
        // ... rest of method
    }
}
```

**AFTER:**
```scala
package controllers

import java.util.UUID
import org.apache.pekko.actor.ActorRef  // CHANGED
import org.apache.pekko.pattern.Patterns  // CHANGED
import org.sunbird.common.DateUtils
import org.sunbird.common.dto.{Response, ResponseHandler}
import org.sunbird.common.exception.ResponseCode
import org.sunbird.telemetry.logger.TelemetryManager
import play.api.mvc._
import utils.JavaJsonUtils

import scala.jdk.CollectionConverters._  // CHANGED
import scala.concurrent.{ExecutionContext, Future}

abstract class BaseController(protected val cc: ControllerComponents)(implicit exec: ExecutionContext) extends AbstractController(cc) {
    
    def getResult(apiId: String, actor: ActorRef, request: org.sunbird.common.dto.Request) : Future[Result] = {
        val future = Patterns.ask(actor, request, 30000) recoverWith {case e: Exception => Future(ResponseHandler.getErrorResponse(e))}
        // ... rest of method
    }
}
```

### QuestionController.scala (v4)

**Location:** `assessment-api/assessment-service/app/controllers/v4/QuestionController.scala`

**BEFORE:**
```scala
package controllers.v4

import akka.actor.{ActorRef, ActorSystem}
import controllers.BaseController
import javax.inject.{Inject, Named}
import play.api.mvc.ControllerComponents
import utils.{ActorNames, ApiId}

import scala.concurrent.ExecutionContext

class QuestionController @Inject()(@Named(ActorNames.QUESTION_ACTOR) questionActor: ActorRef,
                                   cc: ControllerComponents,
                                   actorSystem: ActorSystem)
                                  (implicit exec: ExecutionContext) extends BaseController(cc) {
    // Controller implementation
}
```

**AFTER:**
```scala
package controllers.v4

import org.apache.pekko.actor.{ActorRef, ActorSystem}  // CHANGED
import controllers.BaseController
import javax.inject.{Inject, Named}
import play.api.mvc.ControllerComponents
import utils.{ActorNames, ApiId}

import scala.concurrent.ExecutionContext

class QuestionController @Inject()(@Named(ActorNames.QUESTION_ACTOR) questionActor: ActorRef,
                                   cc: ControllerComponents,
                                   actorSystem: ActorSystem)
                                  (implicit exec: ExecutionContext) extends BaseController(cc) {
    // Controller implementation (no changes needed)
}
```

### SignalHandler.scala

**Location:** `assessment-api/assessment-service/app/handlers/SignalHandler.scala`

**BEFORE:**
```scala
package handlers

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future

@Singleton
class SignalHandler @Inject()(lifecycle: ApplicationLifecycle, actorSystem: ActorSystem) {
    // Handler implementation
}
```

**AFTER:**
```scala
package handlers

import org.apache.pekko.actor.ActorSystem  // CHANGED
import javax.inject.{Inject, Singleton}
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future

@Singleton
class SignalHandler @Inject()(lifecycle: ApplicationLifecycle, actorSystem: ActorSystem) {
    // Handler implementation (no changes needed)
}
```

---

## Actor System Changes

### Actor Implementation (No changes if using BaseActor from external library)

**Location:** `assessment-api/assessment-actors/src/main/scala/org/sunbird/actors/QuestionActor.scala`

**Note:** If the `BaseActor` comes from `actor-core` dependency and has been updated to use Pekko, no changes needed. Otherwise:

**BEFORE:**
```scala
package org.sunbird.actors

import akka.actor.Actor
import scala.concurrent.{ExecutionContext, Future}

class QuestionActor extends Actor {
    implicit val ec: ExecutionContext = context.dispatcher
    
    override def receive: Receive = {
        case msg => // handle message
    }
}
```

**AFTER:**
```scala
package org.sunbird.actors

import org.apache.pekko.actor.Actor  // CHANGED
import scala.concurrent.{ExecutionContext, Future}

class QuestionActor extends Actor {
    implicit val ec: ExecutionContext = context.dispatcher
    
    override def receive: Receive = {
        case msg => // handle message
    }
}
```

---

## Configuration File Changes

### application.conf

**Location:** `assessment-api/assessment-service/conf/application.conf`

**BEFORE:**
```hocon
## Akka
# https://www.playframework.com/documentation/latest/ScalaAkka#Configuration
akka {
  default-dispatcher {
    fork-join-executor {
      parallelism-min = 8
      parallelism-factor = 32.0
      parallelism-max = 64
      task-peeking-mode = "FIFO"
    }
  }
  actors-dispatcher {
    type = "Dispatcher"
    executor = "fork-join-executor"
    fork-join-executor {
      parallelism-min = 8
      parallelism-factor = 32.0
      parallelism-max = 64
    }
    throughput = 1
  }
  actor {
    deployment {
      /healthActor {
        router = smallest-mailbox-pool
        nr-of-instances = 5
        dispatcher = actors-dispatcher
      }
      /questionActor {
        router = smallest-mailbox-pool
        nr-of-instances = 5
        dispatcher = actors-dispatcher
      }
    }
  }
}

akka.http.parsing.max-content-length = 50MB
akka.request_timeout=30
```

**AFTER:**
```hocon
## Pekko (formerly Akka)
# https://pekko.apache.org/docs/pekko/current/
pekko {
  default-dispatcher {
    fork-join-executor {
      parallelism-min = 8
      parallelism-factor = 32.0
      parallelism-max = 64
      task-peeking-mode = "FIFO"
    }
  }
  actors-dispatcher {
    type = "Dispatcher"
    executor = "fork-join-executor"
    fork-join-executor {
      parallelism-min = 8
      parallelism-factor = 32.0
      parallelism-max = 64
    }
    throughput = 1
  }
  actor {
    deployment {
      /healthActor {
        router = smallest-mailbox-pool
        nr-of-instances = 5
        dispatcher = actors-dispatcher
      }
      /questionActor {
        router = smallest-mailbox-pool
        nr-of-instances = 5
        dispatcher = actors-dispatcher
      }
    }
  }
}

pekko.http.parsing.max-content-length = 50MB
pekko.request_timeout=30
```

### Test Configuration

**Location:** `assessment-api/assessment-actors/src/test/resources/application.conf`

**BEFORE:**
```hocon
akka {
  actor {
    provider = "local"
  }
  log-dead-letters = off
  log-dead-letters-during-shutdown = off
  loglevel = "WARNING"
}

akka.request_timeout=30
```

**AFTER:**
```hocon
pekko {
  actor {
    provider = "local"
  }
  log-dead-letters = off
  log-dead-letters-during-shutdown = off
  loglevel = "WARNING"
}

pekko.request_timeout=30
```

---

## Test Updates

### BaseSpec.scala

**Location:** `assessment-api/assessment-actors/src/test/scala/org/sunbird/actors/BaseSpec.scala`

**BEFORE:**
```scala
package org.sunbird.actors

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.duration._

class BaseSpec extends TestKit(ActorSystem("test-system"))
  with FlatSpec
  with Matchers
  with BeforeAndAfterAll {

  implicit val timeout: akka.util.Timeout = 30.seconds

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
```

**AFTER:**
```scala
package org.sunbird.actors

import org.apache.pekko.actor.{ActorSystem, Props}  // CHANGED
import org.apache.pekko.testkit.TestKit  // CHANGED
import org.scalatest.flatspec.AnyFlatSpecLike  // CHANGED for ScalaTest 3.2+
import org.scalatest.matchers.should.Matchers  // CHANGED for ScalaTest 3.2+
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._

class BaseSpec extends TestKit(ActorSystem("test-system"))
  with AnyFlatSpecLike  // CHANGED
  with Matchers
  with BeforeAndAfterAll {

  implicit val timeout: org.apache.pekko.util.Timeout = 30.seconds  // CHANGED

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
```

### QuestionActorTest.scala

**Location:** `assessment-api/assessment-actors/src/test/scala/org/sunbird/actors/QuestionActorTest.scala`

**BEFORE:**
```scala
package org.sunbird.actors

import akka.actor.Props
import org.sunbird.common.dto.Request
import org.scalatest.FlatSpec

class QuestionActorTest extends BaseSpec {

  "QuestionActor" should "create a question" in {
    val props = Props(new QuestionActor())
    val actor = system.actorOf(props)
    // Test implementation
  }
}
```

**AFTER:**
```scala
package org.sunbird.actors

import org.apache.pekko.actor.Props  // CHANGED
import org.sunbird.common.dto.Request

class QuestionActorTest extends BaseSpec {

  "QuestionActor" should "create a question" in {
    val props = Props(new QuestionActor())
    val actor = system.actorOf(props)
    // Test implementation (no changes needed)
  }
}
```

---

## Common Issues and Solutions

### Issue 1: Compilation Error - Cannot Find Akka Classes

**Error:**
```
[error] object actor is not a member of package akka
[error] import akka.actor.ActorRef
```

**Solution:**
Make sure all `akka` imports are changed to `org.apache.pekko`:
```scala
// Wrong
import akka.actor.ActorRef

// Correct
import org.apache.pekko.actor.ActorRef
```

### Issue 2: NoClassDefFoundError at Runtime

**Error:**
```
java.lang.NoClassDefFoundError: akka/actor/ActorSystem
```

**Solution:**
This means some dependency still references Akka. Check:
1. All POMs have been updated
2. External dependencies (`actor-core`, `graph-engine`, etc.) are Pekko-compatible versions
3. Run `mvn dependency:tree` to find conflicting dependencies

### Issue 3: Configuration Not Loading

**Error:**
```
No configuration setting found for key 'pekko.actor'
```

**Solution:**
Ensure all `akka` references in `application.conf` are changed to `pekko`:
```bash
# Search for remaining akka references
grep -r "akka\." conf/
grep -r "akka " conf/
```

### Issue 4: Actor Not Starting

**Error:**
```
Actor path not found: /user/questionActor
```

**Solution:**
1. Check actor binding in module: `bindActor(classOf[QuestionActor], ActorNames.QUESTION_ACTOR)`
2. Verify actor name matches configuration
3. Check Pekko configuration is loaded correctly
4. Enable debug logging: `pekko.loglevel = "DEBUG"`

### Issue 5: JavaConverters Deprecation Warning

**Warning:**
```
[warn] object JavaConverters in package collection is deprecated
```

**Solution:**
Replace with new import:
```scala
// Old
import scala.collection.JavaConverters._

// New
import scala.jdk.CollectionConverters._
```

### Issue 6: Play 3.0 API Changes

**Error:**
```
[error] value bodyParser is not a member of play.api.mvc.BodyParsers
```

**Solution:**
Play 3.0 has some API changes. Check Play 3.0 migration guide:
https://www.playframework.com/documentation/3.0.x/Migration30

Common changes:
- Some helper methods moved or renamed
- JSON library improvements
- Router API enhancements

### Issue 7: Scala 2.13 Collection Changes

**Error:**
```
[error] value breakOut is not a member of object scala.collection.breakOut
```

**Solution:**
Scala 2.13 removed `breakOut`. Use explicit `to` conversions:
```scala
// Old (2.12)
list.map(x => (x, x))(collection.breakOut): Map[Int, Int]

// New (2.13)
list.map(x => (x, x)).toMap
```

### Issue 8: ScalaTest 3.2 Changes

**Error:**
```
[error] not found: type FlatSpec
```

**Solution:**
ScalaTest 3.2 renamed some base classes:
```scala
// Old
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class MyTest extends FlatSpec with Matchers

// New
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MyTest extends AnyFlatSpec with Matchers
```

---

## Automated Search and Replace

### Using sed (Linux/Mac)

```bash
# Navigate to project root
cd /path/to/inquiry-api-service

# Backup first!
git checkout -b migration/play3-pekko

# Akka to Pekko imports
find . -name "*.scala" -type f -exec sed -i.bak 's/import akka\.actor/import org.apache.pekko.actor/g' {} +
find . -name "*.scala" -type f -exec sed -i.bak 's/import akka\.pattern/import org.apache.pekko.pattern/g' {} +
find . -name "*.scala" -type f -exec sed -i.bak 's/import akka\.testkit/import org.apache.pekko.testkit/g' {} +
find . -name "*.scala" -type f -exec sed -i.bak 's/import akka\.util/import org.apache.pekko.util/g' {} +

# JavaConverters
find . -name "*.scala" -type f -exec sed -i.bak 's/import scala\.collection\.JavaConverters/import scala.jdk.CollectionConverters/g' {} +

# Play Akka to Pekko
find . -name "*.scala" -type f -exec sed -i.bak 's/play\.libs\.akka/play.libs.pekko/g' {} +
find . -name "*.scala" -type f -exec sed -i.bak 's/AkkaGuiceSupport/PekkoGuiceSupport/g' {} +

# Configuration files
find . -name "application.conf" -type f -exec sed -i.bak 's/^akka {/pekko {/g' {} +
find . -name "application.conf" -type f -exec sed -i.bak 's/^akka\./pekko./g' {} +
find . -name "application.conf" -type f -exec sed -i.bak 's/ akka\./ pekko./g' {} +

# Remove backup files (only after verifying changes)
# find . -name "*.bak" -type f -delete
```

### Using IntelliJ IDEA

1. Open **Edit → Find → Replace in Files** (Ctrl+Shift+R)
2. Enable **Regex** option
3. Use these patterns:

**Pattern 1: Akka imports**
- Find: `import akka\.`
- Replace: `import org.apache.pekko.`

**Pattern 2: JavaConverters**
- Find: `import scala\.collection\.JavaConverters`
- Replace: `import scala.jdk.CollectionConverters`

**Pattern 3: Play Akka**
- Find: `play\.libs\.akka`
- Replace: `play.libs.pekko`

---

## Verification Commands

### Check for Remaining Akka References

```bash
# Search for akka in Scala files
grep -r "import akka\." --include="*.scala" .

# Search for akka in configuration
grep -r "akka\." --include="*.conf" .

# Search for akka in POMs
grep -r "akka" --include="pom.xml" .
```

### Verify Compilation

```bash
# Clean and compile
mvn clean compile

# Check for warnings
mvn compile 2>&1 | grep -i "warn"

# Run tests
mvn test
```

### Verify Dependencies

```bash
# Show dependency tree
mvn dependency:tree

# Look for akka dependencies (should be none)
mvn dependency:tree | grep -i akka

# Look for pekko dependencies (should see them)
mvn dependency:tree | grep -i pekko
```

---

## Additional Resources

### Official Documentation
- **Pekko Migration Guide**: https://pekko.apache.org/docs/pekko/current/project/migration-guide-1.0.x-akka-2.6.x.html
- **Play 3.0 Migration**: https://www.playframework.com/documentation/3.0.x/Migration30
- **Scala 2.13 Collections**: https://docs.scala-lang.org/overviews/core/collections-migration-213.html

### Automated Tools
- **Scalafix**: Can automate some migration steps
- **sbt-migration-manager**: Helps with dependency migrations

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-13  
**Status**: Ready for Use

---

**Remember**: Always test your changes after each modification. Don't make all changes at once!
