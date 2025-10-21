# Assessment Item API Migration - Developer Checklist

## Pre-Migration Checklist

### Environment Setup
- [ ] Development environment with Neo4j 3.3.0
- [ ] Cassandra 3.11.8 configured
- [ ] Redis 6.0.8 running
- [ ] Kafka setup complete
- [ ] JDK 11 installed
- [ ] Scala 2.12 configured
- [ ] Maven/SBT available
- [ ] IDE setup (IntelliJ IDEA recommended)

### Knowledge Prerequisites
- [ ] Reviewed ItemSet implementation (`ItemSetController.scala`, `ItemSetActor.scala`)
- [ ] Reviewed Question implementation (`QuestionController.scala`, `QuestionActor.scala`)
- [ ] Understood routes configuration in `conf/routes`
- [ ] Familiar with Play Framework actions
- [ ] Understood Akka Actor pattern
- [ ] Read Neo4j documentation
- [ ] Read Cassandra CQL basics

### Repository Access
- [ ] Access to source repository (sunbird-learning-platform)
- [ ] Access to target repository (inquiry-api-service)
- [ ] Git branch created for development
- [ ] Development database access

---

## Phase 1: Core Implementation (Week 1-2)

### Controller Implementation
- [ ] Create `assessment-api/assessment-service/app/controllers/v3/AssessmentItemController.scala`
- [ ] Extend `BaseController`
- [ ] Define `objectType = "AssessmentItem"`
- [ ] Define `schemaName = "assessmentitem"`
- [ ] Define `version = "1.0"`
- [ ] Implement `create()` action
  - [ ] Extract assessment_item from request body
  - [ ] Call actor with "createItem" operation
  - [ ] Return response with identifier
- [ ] Implement `read()` action
  - [ ] Accept identifier, ifields, fields parameters
  - [ ] Call actor with "readItem" operation
  - [ ] Return assessment item data
- [ ] Implement `update()` action
  - [ ] Accept identifier parameter
  - [ ] Extract assessment_item from request body
  - [ ] Call actor with "updateItem" operation
  - [ ] Return response with identifier
- [ ] Add proper error handling for all actions
- [ ] Add telemetry logging

### Actor Implementation
- [ ] Create `assessment-api/assessment-actors/src/main/scala/org/sunbird/actors/AssessmentItemActor.scala`
- [ ] Extend `BaseActor`
- [ ] Inject `OntologyEngineContext`
- [ ] Implement `onReceive()` method with pattern matching
- [ ] Implement `create()` method
  - [ ] Validate request
  - [ ] Call `DataNode.create(request)`
  - [ ] Return response with identifier
- [ ] Implement `read()` method
  - [ ] Extract identifier and fields from request
  - [ ] Call `DataNode.read(request)`
  - [ ] Serialize node metadata
  - [ ] Return response with assessment item data
- [ ] Implement `update()` method
  - [ ] Validate request
  - [ ] Call `DataNode.update(request)`
  - [ ] Return response with identifier
- [ ] Add error handling for each operation

### Operations Enum
- [ ] Create `assessment-api/assessment-service/app/utils/AssessmentItemOperations.scala`
- [ ] Define enumeration values:
  - [ ] `createItem`
  - [ ] `readItem`
  - [ ] `updateItem`
  - [ ] `searchItem`
  - [ ] `listItem`
  - [ ] `retireItem`

### Constants Updates
- [ ] Update `assessment-api/assessment-service/app/utils/ActorNames.scala`
  - [ ] Add `val ASSESSMENT_ITEM_ACTOR = "AssessmentItemActor"`
- [ ] Update `assessment-api/assessment-service/app/utils/ApiId.scala`
  - [ ] Add `CREATE_ASSESSMENT_ITEM`
  - [ ] Add `READ_ASSESSMENT_ITEM`
  - [ ] Add `UPDATE_ASSESSMENT_ITEM`
  - [ ] Add `SEARCH_ASSESSMENT_ITEM`
  - [ ] Add `LIST_ASSESSMENT_ITEM`
  - [ ] Add `RETIRE_ASSESSMENT_ITEM`

### Routes Configuration
- [ ] Update `assessment-api/assessment-service/conf/routes`
- [ ] Add route: `POST /assessment/v3/items/create`
- [ ] Add route: `GET /assessment/v3/items/read/:id`
- [ ] Add route: `PATCH /assessment/v3/items/update/:id`
- [ ] Verify route ordering (most specific first)

### Module Configuration
- [ ] Update `assessment-api/assessment-service/app/modules/AssessmentServiceModule.scala`
- [ ] Bind AssessmentItemActor to actor system
- [ ] Verify actor name matches `ActorNames.ASSESSMENT_ITEM_ACTOR`

### Testing (Phase 1)
- [ ] Create `assessment-api/assessment-service/test/controllers/v3/AssessmentItemControllerSpec.scala`
- [ ] Test create endpoint
  - [ ] Test successful creation
  - [ ] Test with missing required fields
  - [ ] Test with invalid data
- [ ] Test read endpoint
  - [ ] Test successful read
  - [ ] Test with non-existent ID
  - [ ] Test with field filtering
- [ ] Test update endpoint
  - [ ] Test successful update
  - [ ] Test with non-existent ID
  - [ ] Test with invalid data
- [ ] Create `assessment-api/assessment-actors/src/test/scala/org/sunbird/actors/AssessmentItemActorTest.scala`
- [ ] Test all actor operations
- [ ] Verify database interactions

### Phase 1 Verification
- [ ] All Phase 1 tests passing
- [ ] Create API working end-to-end
- [ ] Read API working end-to-end
- [ ] Update API working end-to-end
- [ ] No compilation errors
- [ ] No memory leaks
- [ ] Proper logging in place

---

## Phase 2: Validation & Business Logic (Week 3-4)

### Validator Implementation
- [ ] Create `assessment-api/assessment-actors/src/main/scala/org/sunbird/validators/AssessmentItemValidator.scala`
- [ ] Implement validation for MCQ (Multiple Choice Question)
  - [ ] Validate options structure
  - [ ] Validate answer structure
  - [ ] Validate num_answers
- [ ] Implement validation for MMCQ (Multiple Multiple Choice Question)
  - [ ] Validate multiple correct answers
- [ ] Implement validation for FTB (Fill in the Blanks)
  - [ ] Validate answer presence
  - [ ] Validate answer format
- [ ] Implement validation for MTF (Match the Following)
  - [ ] Validate pairs structure
- [ ] Implement validation for hints
  - [ ] Validate hint structure (order, content, timing, etc.)
- [ ] Implement validation for responses
  - [ ] Validate response structure
- [ ] Add validation to actor create/update methods
- [ ] Create validation tests

### External Property Storage (if needed)
- [ ] Analyze if external properties are needed (body, editorstate, question, solutions)
- [ ] Create `assessment-api/assessment-actors/src/main/scala/org/sunbird/store/AssessmentItemStore.scala`
- [ ] Implement Cassandra connection
- [ ] Create `question_data` table in Cassandra (if not exists)
  ```sql
  CREATE TABLE IF NOT EXISTS question_data (
    question_id text PRIMARY KEY,
    body__txt text,
    editorstate__txt text,
    question__txt text,
    solutions__txt text
  );
  ```
- [ ] Implement save method for external properties
- [ ] Implement read method for external properties
- [ ] Implement update method for external properties
- [ ] Integrate with actor create/read/update operations
- [ ] Test storage operations

### Manager Implementation (if complex logic needed)
- [ ] Create `assessment-api/assessment-actors/src/main/scala/org/sunbird/managers/AssessmentItemManager.scala`
- [ ] Implement business logic methods
- [ ] Handle media item variants
- [ ] Handle framework defaults
- [ ] Integrate with actor

### Phase 2 Testing
- [ ] Test all validation rules
- [ ] Test external property storage
- [ ] Test with different item types
- [ ] Test edge cases
- [ ] Performance testing for storage operations

### Phase 2 Verification
- [ ] All Phase 2 tests passing
- [ ] Validation working correctly
- [ ] External properties stored and retrieved correctly
- [ ] No data loss
- [ ] Performance acceptable

---

## Phase 3: Query APIs (Week 5)

### Search Implementation
- [ ] Implement `search()` action in controller
  - [ ] Accept search criteria in request body
  - [ ] Call actor with "searchItem" operation
  - [ ] Return list of assessment items
- [ ] Implement `search()` method in actor
  - [ ] Parse search criteria
  - [ ] Build search query
  - [ ] Execute search via DataNode or Graph Engine
  - [ ] Return paginated results
- [ ] Add search parameters support:
  - [ ] Filter by item type
  - [ ] Filter by framework
  - [ ] Filter by status
  - [ ] Filter by metadata fields
- [ ] Add route: `POST /assessment/v3/items/search`

### List Implementation
- [ ] Implement `list()` action in controller
  - [ ] Accept limit and offset parameters
  - [ ] Call actor with "listItem" operation
  - [ ] Return paginated list
- [ ] Implement `list()` method in actor
  - [ ] Default limit = 200, offset = 0
  - [ ] Build list query
  - [ ] Execute via DataNode
  - [ ] Return paginated results
- [ ] Add route: `POST /assessment/v3/items/list`

### Retire Implementation
- [ ] Implement `retire()` action in controller
  - [ ] Accept identifier parameter
  - [ ] Call actor with "retireItem" operation
  - [ ] Return success response
- [ ] Implement `retire()` method in actor
  - [ ] Set status = "Retired"
  - [ ] Update node via DataNode
  - [ ] Return success response
- [ ] Add route: `DELETE /assessment/v3/items/retire/:id`

### Phase 3 Testing
- [ ] Test search with various criteria
- [ ] Test search pagination
- [ ] Test list with pagination
- [ ] Test retire operation
- [ ] Test retired items are excluded from search/list

### Phase 3 Verification
- [ ] All 6 APIs implemented and working
- [ ] Search working with all filters
- [ ] List working with pagination
- [ ] Retire working correctly
- [ ] All tests passing

---

## Phase 4: Schema & Configuration (Week 6)

### Schema Definition
- [ ] Create schema file for AssessmentItem
- [ ] Define required fields:
  - [ ] name
  - [ ] code
  - [ ] type (MCQ, MMCQ, FTB, MTF, etc.)
  - [ ] status
  - [ ] framework
- [ ] Define optional fields
- [ ] Define metadata structure
- [ ] Define validation rules in schema
- [ ] Add to schemas directory

### Configuration
- [ ] Update application.conf
- [ ] Add assessment item specific configurations
- [ ] Configure Cassandra connection (if using external storage)
- [ ] Add error codes for assessment items
- [ ] Add logging configuration

### Database Verification
- [ ] Neo4j schema created correctly
- [ ] Cassandra table created (if needed)
- [ ] Test database connectivity
- [ ] Verify indexes
- [ ] Test data migration (if applicable)

### Phase 4 Verification
- [ ] Schema validated
- [ ] Configuration working
- [ ] Database setup complete
- [ ] No configuration errors

---

## Phase 5: Documentation & Polish (Week 7)

### API Documentation
- [ ] Document Create API
  - [ ] Endpoint details
  - [ ] Request format
  - [ ] Response format
  - [ ] Error codes
  - [ ] Examples
- [ ] Document Read API
- [ ] Document Update API
- [ ] Document Search API
- [ ] Document List API
- [ ] Document Retire API
- [ ] Add Postman collection
- [ ] Add curl examples

### Developer Documentation
- [ ] Document architecture
- [ ] Document data flow
- [ ] Document validation rules
- [ ] Add setup guide
- [ ] Add troubleshooting guide

### Code Quality
- [ ] Code review completed
- [ ] All compiler warnings resolved
- [ ] Code formatted consistently
- [ ] Comments added where needed
- [ ] TODOs resolved or documented

### Testing
- [ ] Unit test coverage > 80%
- [ ] Integration tests complete
- [ ] Load testing done
- [ ] Security testing done
- [ ] All tests passing

### Phase 5 Verification
- [ ] Documentation complete
- [ ] Code review approved
- [ ] Test coverage adequate
- [ ] Ready for deployment

---

## Final Verification Checklist

### Functional Testing
- [ ] Create assessment item with all item types (MCQ, MMCQ, FTB, MTF, etc.)
- [ ] Read assessment item with and without field filtering
- [ ] Update assessment item (partial and full updates)
- [ ] Search assessment items with various criteria
- [ ] List assessment items with pagination
- [ ] Retire assessment item
- [ ] Verify retired items don't appear in search/list
- [ ] Test with invalid data
- [ ] Test with edge cases
- [ ] Test error scenarios

### Non-Functional Testing
- [ ] Performance testing
  - [ ] Read < 500ms
  - [ ] Write < 1s
  - [ ] Search with pagination < 2s
- [ ] Load testing
  - [ ] 100 concurrent users
  - [ ] No memory leaks
  - [ ] No connection pool exhaustion
- [ ] Security testing
  - [ ] Input validation
  - [ ] SQL injection prevention
  - [ ] Authorization checks
- [ ] Monitoring
  - [ ] Logging working
  - [ ] Metrics captured
  - [ ] Alerts configured

### Integration Testing
- [ ] Test with ItemSet APIs
- [ ] Test with Question APIs
- [ ] Test with QuestionSet APIs
- [ ] Test data consistency across APIs
- [ ] Test with external systems

### Documentation Verification
- [ ] API documentation complete
- [ ] Developer guide complete
- [ ] Schema documentation complete
- [ ] Configuration guide complete
- [ ] Migration guide complete

### Deployment Readiness
- [ ] All tests passing
- [ ] Code review approved
- [ ] Documentation complete
- [ ] Database migrations ready
- [ ] Configuration files ready
- [ ] Rollback plan documented
- [ ] Monitoring setup
- [ ] Support team trained

---

## Post-Migration Checklist

### Deployment
- [ ] Deploy to staging environment
- [ ] Smoke tests in staging
- [ ] Performance tests in staging
- [ ] Deploy to production
- [ ] Smoke tests in production

### Monitoring
- [ ] Monitor API response times
- [ ] Monitor error rates
- [ ] Monitor database performance
- [ ] Monitor memory usage
- [ ] Set up alerts

### Support
- [ ] Document known issues
- [ ] Create troubleshooting guide
- [ ] Train support team
- [ ] Monitor support tickets

### Handover
- [ ] Knowledge transfer session
- [ ] Code walkthrough
- [ ] Documentation review
- [ ] Support handover

---

## Common Issues & Solutions

### Issue: Actor not receiving messages
**Solution**: 
- Verify actor binding in Module configuration
- Check actor name matches in controller
- Verify actor is registered with Akka system

### Issue: Validation not working
**Solution**:
- Check validator is called in actor
- Verify validation rules are correct
- Check error responses are properly formatted

### Issue: External properties not saving
**Solution**:
- Verify Cassandra connection
- Check table schema
- Verify property names match

### Issue: Search returning no results
**Solution**:
- Check Neo4j query syntax
- Verify search criteria parsing
- Check node labels and properties

### Issue: Performance degradation
**Solution**:
- Check database indexes
- Verify connection pool settings
- Review query optimization
- Check for N+1 query issues

---

## Resources

- Full Migration Guide: [ASSESSMENT_ITEM_API_MIGRATION.md](./ASSESSMENT_ITEM_API_MIGRATION.md)
- Executive Summary: [ASSESSMENT_ITEM_API_MIGRATION_SUMMARY.md](./ASSESSMENT_ITEM_API_MIGRATION_SUMMARY.md)
- Source Repository: https://github.com/Sunbird-Knowlg/sunbird-learning-platform
- Play Framework Documentation: https://www.playframework.com/documentation/2.7.x/Home
- Akka Documentation: https://doc.akka.io/docs/akka/current/
- Neo4j Documentation: https://neo4j.com/docs/
- Cassandra Documentation: https://cassandra.apache.org/doc/

---

**Checklist Version**: 1.0  
**Last Updated**: 2025-10-21  
**Use this checklist**: To track progress during migration
