# Assessment Item API Migration Guide

## Overview
This document outlines the requirements, dependencies, and effort estimation for migrating the Assessment Item APIs from the Sunbird Learning Platform (`sunbird-learning-platform` repository, release-5.7.0_RC8 branch) to the inquiry-api-service repository.

## Source Repository
- **Repository**: https://github.com/Sunbird-Knowlg/sunbird-learning-platform
- **Branch**: release-5.7.0_RC8
- **Module**: platform-modules/assessment-manager
- **Controller**: `org.sunbird.assessment.controller.AssessmentItemV3Controller`
- **Base Path**: `/assessment/v3/items`

## Target Repository
- **Repository**: inquiry-api-service (current project)
- **Module**: assessment-api
- **Existing Similar Implementation**: ItemSet APIs (v3), Question APIs (v4/v5), QuestionSet APIs (v4/v5)

---

## End-to-End Flow Analysis

### 1. External Repository (Sunbird Learning Platform) - Assessment Item APIs

#### 1.1 Architecture Overview
The external repository follows a **Spring MVC + Graph Database** architecture:

```
Request Flow:
REST Controller (Spring)
    ↓
AssessmentManager (Business Logic)
    ↓
AssessmentValidator (Validation)
    ↓
Graph Engine (Neo4j Operations)
    ↓
AssessmentStore (Cassandra - External Properties)
```

#### 1.2 API Endpoints in External Repository

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/assessment/v3/items/create` | POST | Create a new assessment item (question) |
| `/assessment/v3/items/list` | POST | List all assessment items with pagination |
| `/assessment/v3/items/update/{id}` | PATCH | Update an existing assessment item |
| `/assessment/v3/items/read/{id}` | GET | Read/retrieve an assessment item by ID |
| `/assessment/v3/items/search` | POST | Search assessment items with criteria |
| `/assessment/v3/items/retire/{id}` | DELETE | Retire/delete an assessment item |

#### 1.3 Core Components

**A. Controller Layer**
- `AssessmentItemV3Controller.java` - REST endpoints handler
  - Uses Spring `@Controller` and `@RequestMapping` annotations
  - Handles request/response transformation
  - Delegates to `IAssessmentManager`

**B. Manager Layer**
- `IAssessmentManager.java` - Interface defining assessment operations
- `AssessmentManagerImpl.java` - Core business logic implementation
  - Creates/updates assessment items
  - Validates item structure
  - Manages item relationships
  - Handles external properties
  - Interacts with Graph Engine

**C. Validation Layer**
- `AssessmentValidator.java` - Validates assessment items
  - Validates item type (MCQ, MMCQ, FTB, MTF, etc.)
  - Validates item structure (options, answers, hints, responses)
  - Ensures data integrity

**D. Handler Layer**
- `AssessmentItemFactory.java` - Factory for item type handlers
- `IAssessmentHandler.java` - Handler interface
- Handler implementations:
  - `MCQHandler.java` - Multiple Choice Question handler
  - `DefaultHandler.java` - Default/Reference handler

**E. Storage Layer**
- `AssessmentStore.java` - Cassandra operations
  - Stores external properties (body, editorstate, question, solutions)
  - Handles BLOB storage for large data
  - Manages question_data table in Cassandra

**F. DTOs**
- `ItemDTO.java` - Assessment Item data transfer object
- `ItemSearchCriteria.java` - Search criteria for items
- `ItemSetDTO.java` - Item set data transfer object
- `ItemSetSearchCriteria.java` - Search criteria for item sets

**G. Enums**
- `AssessmentAPIParams.java` - API parameter constants
- `AssessmentErrorCodes.java` - Error code definitions
- `AssessmentItemType.java` - Item type enumeration (MCQ, MMCQ, FTB, MTF, etc.)
- `QuestionnaireType.java` - Questionnaire type enumeration

**H. Utilities**
- `BulkItemImport.java` - Bulk import functionality
- `QuestionTemplateHandler.java` - Template processing

#### 1.4 Data Flow for Create Operation

```
1. Client sends POST request to /assessment/v3/items/create
   ↓
2. AssessmentItemV3Controller.create() receives request
   - Extracts assessment_item from request body
   - Adds taxonomy_id ("domain")
   ↓
3. AssessmentManagerImpl.createAssessmentItem()
   - Extracts external properties (body, editorstate, question, solutions)
   - Sets default framework if not provided
   - Sets version = 1 if not provided
   - Validates item (unless skipValidation=true)
     ↓
   - AssessmentValidator.validateAssessmentItem()
     - Validates based on item type (MCQ, FTB, MTF, etc.)
     - Checks options, answers, hints, responses structure
   ↓
4. Replaces media items with variants
   ↓
5. Graph Engine creates data node in Neo4j
   - Stores core metadata in graph database
   ↓
6. AssessmentStore.updateAssessmentProperties()
   - Stores external properties in Cassandra (question_data table)
   - Stores as BLOB/text for large content
   ↓
7. Returns response with node_id (assessment item identifier)
```

#### 1.5 Data Flow for Read Operation

```
1. Client sends GET request to /assessment/v3/items/read/{id}
   ↓
2. AssessmentItemV3Controller.find() receives request
   - Extracts id, ifields (item fields), fields parameters
   ↓
3. AssessmentManagerImpl.getAssessmentItem()
   - Fetches node from Neo4j via Graph Engine
   - Retrieves external properties from Cassandra
   - Merges properties with node metadata
   ↓
4. Returns complete assessment item with all properties
```

#### 1.6 Dependencies in External Repository

From `assessment-manager/pom.xml`:

**Core Dependencies:**
- Spring Framework (spring-webmvc) - 4.2.x
- Graph Engine (taxonomy-manager, graph-engine) - 1.1-SNAPSHOT
- Neo4j (neo4j-bolt) - 3.3.0
- Cassandra (cassandra-driver-core) - 3.1.2
- Apache Commons (commons-lang3, commons-collections4, commons-io)
- Jackson (for JSON processing) - org.codehaus.jackson
- Apache HttpClient - 4.5.2
- Apache Velocity - 2.1
- Learning Actors - 1.1-SNAPSHOT

**Test Dependencies:**
- Cucumber (cucumber-spring, cucumber-junit) - 1.1.6
- Cassandra Unit - 3.1.1.0
- Neo4j Test dependencies
- Spring Test
- JUnit, Mockito, PowerMock

---

### 2. Current Repository (inquiry-api-service) - ItemSet APIs

#### 2.1 Architecture Overview
The current repository follows an **Akka Actor + Play Framework** architecture:

```
Request Flow:
Play Controller (Scala)
    ↓
Akka Actor System
    ↓
ItemSetActor / QuestionActor / QuestionSetActor
    ↓
DataNode Operations (Graph + Storage abstraction)
    ↓
Neo4j + Cassandra
```

#### 2.2 Existing ItemSet API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/itemset/v3/create` | POST | Create a new item set |
| `/itemset/v3/read/{identifier}` | GET | Read an item set |
| `/itemset/v3/update/{identifier}` | PATCH | Update an item set |
| `/itemset/v3/review/{identifier}` | POST | Review an item set |
| `/itemset/v3/retire/{identifier}` | DELETE | Retire an item set |

#### 2.3 Core Components

**A. Controller Layer**
- `controllers.v3.ItemSetController.scala`
  - Play Framework controller
  - Handles HTTP requests
  - Delegates to ItemSetActor via Akka

**B. Actor Layer**
- `org.sunbird.actors.ItemSetActor.scala`
  - Implements actor pattern for concurrency
  - Operations: createItemSet, readItemSet, updateItemSet, reviewItemSet, retireItemSet
  - Uses DataNode abstraction for persistence

**C. Data Layer**
- `DataNode` abstraction (from graph-engine)
  - create(), read(), update(), bulkUpdate()
  - Handles Neo4j operations
  - Abstracts database complexity

**D. Utilities**
- `utils.ItemSetOperations.scala` - Operation enumeration
- `utils.ActorNames.scala` - Actor name constants
- `utils.ApiId.scala` - API identifier constants

#### 2.4 Question APIs (v4/v5) - Similar Pattern

**Controllers:**
- `controllers.v4.QuestionController.scala`
- `controllers.v5.QuestionController.scala`

**Actors:**
- `org.sunbird.actors.QuestionActor.scala`
- `org.sunbird.v5.actors.QuestionActor.scala`

**Additional Operations:**
- create, read, privateRead, update, review, publish, retire
- import, systemUpdate, list, reject, copy

**Managers:**
- `org.sunbird.managers.AssessmentManager.scala` (v4)
- `org.sunbird.v5.managers.AssessmentV5Manager.scala` (v5)

#### 2.5 Technology Stack

**Current Project Stack:**
- Scala 2.12.11
- Play Framework 2.7.2
- Akka Actor System
- Neo4j 3.3.0
- Cassandra 3.11.8
- Redis 6.0.8
- Maven build system

---

## Migration Requirements

### 1. New Components to Develop

#### A. Controller
- **File**: `assessment-api/assessment-service/app/controllers/v3/AssessmentItemController.scala`
- **Pattern**: Follow existing ItemSetController pattern
- **Endpoints to implement**: All 6 endpoints listed above
- **Effort**: 2-3 days

#### B. Actor
- **File**: `assessment-api/assessment-actors/src/main/scala/org/sunbird/actors/AssessmentItemActor.scala`
- **Operations**: createItem, readItem, updateItem, searchItem, listItem, retireItem
- **Pattern**: Follow existing ItemSetActor pattern
- **Effort**: 3-4 days

#### C. Operations Enum
- **File**: `assessment-api/assessment-service/app/utils/AssessmentItemOperations.scala`
- **Content**: Enumeration of all operations
- **Effort**: 0.5 day

#### D. Validator (Optional but Recommended)
- **File**: `assessment-api/assessment-actors/src/main/scala/org/sunbird/validators/AssessmentItemValidator.scala`
- **Purpose**: Port validation logic from Java to Scala
- **Validations**: Item type validation, structure validation, answer validation
- **Effort**: 2-3 days

#### E. Manager (if complex logic needed)
- **File**: `assessment-api/assessment-actors/src/main/scala/org/sunbird/managers/AssessmentItemManager.scala`
- **Purpose**: Complex business logic, external property handling
- **Effort**: 2-3 days

### 2. Components to Modify

#### A. Routes Configuration
- **File**: `assessment-api/assessment-service/conf/routes`
- **Changes**: Add 6 new route definitions
- **Effort**: 0.5 day

#### B. Actor Names
- **File**: `assessment-api/assessment-service/app/utils/ActorNames.scala`
- **Changes**: Add ASSESSMENT_ITEM_ACTOR constant
- **Effort**: 0.5 day

#### C. API ID Constants
- **File**: `assessment-api/assessment-service/app/utils/ApiId.scala`
- **Changes**: Add API IDs for all 6 operations
- **Effort**: 0.5 day

#### D. Module Configuration
- **File**: `assessment-api/assessment-service/app/modules/AssessmentServiceModule.scala`
- **Changes**: Register AssessmentItemActor with Akka
- **Effort**: 0.5 day

### 3. Testing Components

#### A. Unit Tests
- **Controller Test**: `assessment-api/assessment-service/test/controllers/v3/AssessmentItemControllerSpec.scala`
- **Actor Test**: `assessment-api/assessment-actors/src/test/scala/org/sunbird/actors/AssessmentItemActorTest.scala`
- **Effort**: 3-4 days

#### B. Integration Tests (Optional)
- End-to-end API testing
- **Effort**: 2-3 days

### 4. Configuration Changes

#### A. Schema Definition
- **File**: Add schema for AssessmentItem in `schemas/` directory
- **Content**: Define metadata structure, required fields, data types
- **Effort**: 1 day

#### B. Database Schema
- **Neo4j**: Use existing graph database setup
- **Cassandra**: May need to create `question_data` table if external properties are used
- **Effort**: 1 day

### 5. Documentation

#### A. API Documentation
- Document all 6 API endpoints
- Request/response examples
- Error codes
- **Effort**: 1-2 days

#### B. Developer Guide
- Setup instructions
- Architecture overview
- **Effort**: 1 day

---

## Key Differences and Migration Challenges

### 1. Framework Differences

| Aspect | External Repo | Current Repo | Migration Strategy |
|--------|---------------|--------------|-------------------|
| Framework | Spring MVC | Play Framework | Rewrite controllers in Scala/Play |
| Concurrency | Spring thread model | Akka Actors | Implement actor-based pattern |
| Language | Java | Scala | Port logic to Scala |
| DI | Spring DI | Guice (Play) | Use Guice annotations |
| Request Handling | Synchronous | Asynchronous (Futures) | Use Play async actions |

### 2. Architecture Differences

| Component | External | Current | Impact |
|-----------|----------|---------|--------|
| Business Logic Layer | Manager classes | Actor + Manager | Need to split logic appropriately |
| Validation | Separate Validator class | Can be in Actor or separate | Port validation logic |
| Storage Abstraction | Direct Graph Engine calls | DataNode abstraction | Use DataNode or implement direct calls |
| External Properties | Direct Cassandra calls via AssessmentStore | Need to implement | Implement storage layer for BLOB data |

### 3. Data Storage Differences

**External Repository:**
- Core metadata → Neo4j (via Graph Engine)
- External properties (body, editorstate, question, solutions) → Cassandra

**Current Repository:**
- Uses DataNode abstraction
- May need to implement external property storage separately

**Migration Action:**
- Decide whether to use DataNode for all properties or implement separate storage for large BLOBs
- If using Cassandra for external properties, need to implement AssessmentStore equivalent in Scala

### 4. Validation Logic

**External Repository:**
- Rich validation for different item types (MCQ, MMCQ, FTB, MTF, etc.)
- Validates structure of options, answers, hints, responses
- Validation is synchronous

**Current Repository:**
- Validation is typically done in actors or managers
- Need to port validation logic to Scala

**Migration Action:**
- Create validator similar to AssessmentValidator
- Support all item types with their specific validation rules

### 5. Search and List Functionality

**External Repository:**
- Uses ItemSearchCriteria with complex search conditions
- Search API with POST method
- List API with pagination (limit, offset)

**Current Repository:**
- QuestionSet and Question have list operations
- Need to implement similar search/list for AssessmentItem

**Migration Action:**
- Implement search criteria handling
- Implement pagination
- May need to use existing search infrastructure

---

## Detailed Component Breakdown

### 1. Controller Implementation

```scala
// assessment-api/assessment-service/app/controllers/v3/AssessmentItemController.scala
package controllers.v3

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.Singleton
import controllers.BaseController
import javax.inject.{Inject, Named}
import play.api.mvc.ControllerComponents
import utils.{ActorNames, ApiId, AssessmentItemOperations}

@Singleton
class AssessmentItemController @Inject()(
  @Named(ActorNames.ASSESSMENT_ITEM_ACTOR) assessmentItemActor: ActorRef,
  cc: ControllerComponents,
  actorSystem: ActorSystem
)(implicit exec: ExecutionContext) extends BaseController(cc) {

  val objectType = "AssessmentItem"
  val schemaName: String = "assessmentitem"
  val version = "1.0"

  // Implement 6 methods: create, read, update, search, list, retire
}
```

### 2. Actor Implementation

```scala
// assessment-api/assessment-actors/src/main/scala/org/sunbird/actors/AssessmentItemActor.scala
package org.sunbird.actors

import javax.inject.Inject
import org.sunbird.actor.core.BaseActor
import org.sunbird.common.dto.{Request, Response}
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.nodes.DataNode

import scala.concurrent.{ExecutionContext, Future}

class AssessmentItemActor @Inject()(implicit oec: OntologyEngineContext) extends BaseActor {

  implicit val ec: ExecutionContext = getContext().dispatcher

  override def onReceive(request: Request): Future[Response] = request.getOperation match {
    case "createItem" => create(request)
    case "readItem" => read(request)
    case "updateItem" => update(request)
    case "searchItem" => search(request)
    case "listItem" => list(request)
    case "retireItem" => retire(request)
    case _ => ERROR(request.getOperation)
  }

  // Implement each operation
}
```

### 3. Routes Configuration

```
# assessment-api/assessment-service/conf/routes

# Assessment Item APIs
POST      /assessment/v3/items/create         controllers.v3.AssessmentItemController.create
GET       /assessment/v3/items/read/:id       controllers.v3.AssessmentItemController.read(id:String, ifields:Option[String], fields:Option[String])
PATCH     /assessment/v3/items/update/:id     controllers.v3.AssessmentItemController.update(id:String)
POST      /assessment/v3/items/search         controllers.v3.AssessmentItemController.search
POST      /assessment/v3/items/list           controllers.v3.AssessmentItemController.list(limit:Option[Int], offset:Option[Int])
DELETE    /assessment/v3/items/retire/:id     controllers.v3.AssessmentItemController.retire(id:String)
```

---

## Dependencies Analysis

### 1. Already Available in Current Project
- Neo4j 3.3.0 ✓
- Cassandra 3.11.8 ✓
- Scala/Play Framework ✓
- Akka Actor System ✓
- Graph engine abstractions ✓

### 2. May Need to Add
- Jackson JSON processing (if not using Play JSON)
- Specific validation libraries
- Item type handler implementations

### 3. Database Setup
- Neo4j: Already configured ✓
- Cassandra: Need to verify if `question_data` table exists or create it
- Redis: Already configured ✓

---

## Effort Estimation

### Phase 1: Core Implementation (API + Actor)
| Task | Effort | Details |
|------|--------|---------|
| Controller implementation | 2-3 days | 6 endpoints with proper error handling |
| Actor implementation | 3-4 days | 6 operations with DataNode integration |
| Operations/Constants | 0.5 day | Enums and constants |
| Routes configuration | 0.5 day | Add routes |
| Module/DI setup | 0.5 day | Actor registration |
| **Subtotal** | **7-9 days** | |

### Phase 2: Validation & Business Logic
| Task | Effort | Details |
|------|--------|---------|
| Validator implementation | 2-3 days | Port validation logic for all item types |
| Item type handlers | 2-3 days | MCQ, FTB, MTF, etc. handlers |
| Manager (if needed) | 2-3 days | Complex business logic |
| External property storage | 2-3 days | Cassandra storage for BLOBs |
| **Subtotal** | **8-12 days** | |

### Phase 3: Testing
| Task | Effort | Details |
|------|--------|---------|
| Unit tests (Controller) | 2 days | Test all endpoints |
| Unit tests (Actor) | 2 days | Test all operations |
| Integration tests | 2-3 days | End-to-end API testing |
| **Subtotal** | **6-7 days** | |

### Phase 4: Schema & Configuration
| Task | Effort | Details |
|------|--------|---------|
| Schema definition | 1 day | Define AssessmentItem schema |
| Database setup | 1 day | Cassandra tables if needed |
| Configuration | 1 day | Application config, error codes |
| **Subtotal** | **3 days** | |

### Phase 5: Documentation & Review
| Task | Effort | Details |
|------|--------|---------|
| API documentation | 1-2 days | Swagger/OpenAPI specs, examples |
| Developer guide | 1 day | Setup and architecture docs |
| Code review & fixes | 2-3 days | Address review feedback |
| **Subtotal** | **4-6 days** | |

### **Total Effort Estimate: 28-37 days (5.6-7.4 weeks)**

---

## Effort Breakdown by Developer Level

### Senior Developer (5+ years experience)
- **Optimistic**: 28-30 days (5.6-6 weeks)
- **Realistic**: 32-35 days (6.4-7 weeks)
- **Pessimistic**: 37-40 days (7.4-8 weeks)

### Mid-Level Developer (3-5 years experience)
- **Optimistic**: 35-40 days (7-8 weeks)
- **Realistic**: 42-47 days (8.4-9.4 weeks)
- **Pessimistic**: 50-55 days (10-11 weeks)

### Junior Developer (1-3 years experience)
- **Optimistic**: 45-50 days (9-10 weeks)
- **Realistic**: 55-60 days (11-12 weeks)
- **Pessimistic**: 65-70 days (13-14 weeks)

**Note**: Estimates assume:
- Full-time dedicated developer
- Familiarity with Scala, Play Framework, and Akka
- Understanding of Neo4j and Cassandra
- Access to proper development environment
- Timely code reviews and feedback

---

## Risk Factors and Mitigation

### 1. High Risk
| Risk | Impact | Mitigation |
|------|--------|------------|
| Validation logic complexity | High | Start with basic validation, iterate |
| External property storage | High | Use existing patterns, consult team |
| Performance issues | Medium | Load testing, optimization |

### 2. Medium Risk
| Risk | Impact | Mitigation |
|------|--------|------------|
| Framework differences | Medium | Follow existing patterns closely |
| Search implementation | Medium | Reuse existing search infrastructure |
| Testing coverage | Medium | Incremental testing approach |

### 3. Low Risk
| Risk | Impact | Mitigation |
|------|--------|------------|
| Route configuration | Low | Simple configuration |
| Constants/Enums | Low | Straightforward implementation |

---

## Migration Strategy Recommendations

### Option 1: Complete Migration (Recommended)
- Migrate all 6 APIs together
- Ensures consistency
- **Effort**: 28-37 days
- **Risk**: Medium

### Option 2: Phased Migration
- **Phase 1**: Create, Read, Update (Core APIs) - 15-20 days
- **Phase 2**: Search, List (Query APIs) - 8-10 days
- **Phase 3**: Retire - 2-3 days
- **Total**: 25-33 days + overhead for phase transitions
- **Risk**: Low

### Option 3: Minimal Viable Product (MVP)
- Implement Create, Read, Update only
- **Effort**: 15-20 days
- **Risk**: Low
- Add Search, List, Retire later based on need

---

## Prerequisites

### 1. Technical Skills Required
- Scala programming
- Play Framework
- Akka Actor System
- Neo4j (Cypher queries)
- Cassandra (CQL)
- RESTful API design
- Unit testing (ScalaTest)

### 2. Infrastructure Requirements
- Development environment with:
  - Neo4j 3.3.0
  - Cassandra 3.11.8
  - Redis 6.0.8
  - Kafka
  - JDK 11
  - Scala 2.12
  - SBT/Maven

### 3. Access Requirements
- Source code access to both repositories
- Access to development databases
- Access to test environments

---

## Success Criteria

### 1. Functional Requirements
- All 6 APIs working as per specification
- Data consistency between Neo4j and Cassandra
- Proper error handling
- Backward compatibility with existing data

### 2. Non-Functional Requirements
- Response time < 500ms for read operations
- Response time < 1s for write operations
- 95% test coverage
- No memory leaks
- Proper logging and monitoring

### 3. Quality Requirements
- Code review approved
- All tests passing
- Documentation complete
- Security review passed

---

## Conclusion

Migrating the Assessment Item APIs from the Sunbird Learning Platform to inquiry-api-service is a **medium-complexity** task that requires:

1. **Understanding** both Spring MVC (Java) and Play Framework (Scala) architectures
2. **Porting** business logic and validation from Java to Scala
3. **Adapting** to actor-based concurrency model
4. **Implementing** external property storage (Cassandra)
5. **Testing** thoroughly to ensure data integrity

**Recommended Approach**: 
- Start with Option 2 (Phased Migration) to reduce risk
- Allocate a **senior developer** for 6-7 weeks
- Plan for 2-3 weeks of buffer for unforeseen issues
- Total project timeline: **8-10 weeks**

**Key Success Factors**:
- Deep understanding of existing ItemSet/Question/QuestionSet implementations
- Following established patterns in current codebase
- Incremental development with regular testing
- Proper documentation at each phase
- Regular code reviews and knowledge sharing

---

## Appendix

### A. Reference Files

**External Repository (sunbird-learning-platform):**
- Controller: `/platform-modules/assessment-manager/src/main/java/org/sunbird/assessment/controller/AssessmentItemV3Controller.java`
- Manager: `/platform-modules/assessment-manager/src/main/java/org/sunbird/assessment/mgr/AssessmentManagerImpl.java`
- Validator: `/platform-modules/assessment-manager/src/main/java/org/sunbird/assessment/util/AssessmentValidator.java`
- Store: `/platform-modules/assessment-manager/src/main/java/org/sunbird/assessment/store/AssessmentStore.java`

**Current Repository (inquiry-api-service):**
- ItemSet Controller: `/assessment-api/assessment-service/app/controllers/v3/ItemSetController.scala`
- ItemSet Actor: `/assessment-api/assessment-actors/src/main/scala/org/sunbird/actors/ItemSetActor.scala`
- Question Controller: `/assessment-api/assessment-service/app/controllers/v4/QuestionController.scala`
- Question Actor: `/assessment-api/assessment-actors/src/main/scala/org/sunbird/actors/QuestionActor.scala`

### B. Contact Points

For clarifications or questions during migration:
- Architecture decisions: Refer to existing Question/QuestionSet implementations
- Data model: Review schema definitions in `/schemas` directory
- Infrastructure: Check `/README.md` for setup instructions

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-21  
**Author**: AI Analysis based on repository exploration
