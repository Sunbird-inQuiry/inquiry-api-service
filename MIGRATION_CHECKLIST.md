# Migration Checklist: Play 3.0.5 + Scala 2.13.12 + Pekko 1.0.3

**Use this checklist when performing the actual migration.**

---

## Pre-Migration Phase

### External Dependencies Verification
- [ ] Contact `knowlg-core` repository maintainers
- [ ] Confirm `actor-core` available for Scala 2.13 + Pekko 1.0.3
- [ ] Confirm `graph-engine_2.13` available for Scala 2.13 + Pekko 1.0.3
- [ ] Confirm `platform-common` available for Scala 2.13 + Pekko 1.0.3
- [ ] Confirm `import-manager` available for Scala 2.13 + Pekko 1.0.3
- [ ] Document exact version numbers to use
- [ ] Verify artifact names (e.g., `_2.12` → `_2.13`)

### Environment Setup
- [ ] Install Java 11 (if not already installed)
- [ ] Install Scala 2.13.12
- [ ] Update Maven to latest version
- [ ] Update IDE/Editor with Scala 2.13 support
- [ ] Set up local databases (Neo4j, Cassandra, Redis)
- [ ] Verify Maven can resolve Pekko dependencies

### Project Preparation
- [ ] Create migration branch: `git checkout -b migration/play3-scala2.13-pekko`
- [ ] Document current system behavior (baseline metrics)
- [ ] Run current test suite and record results
- [ ] Take performance baseline measurements
- [ ] Create rollback plan document
- [ ] Schedule team training session
- [ ] Block calendar for migration work (4-6 weeks)

---

## Phase 1: POM Files Update

### Root POM (pom.xml)
- [ ] Update `scala.maj.version` to `2.13`
- [ ] Update `scala.version` to `2.13.12`
- [ ] Update `scalatest.version` to `3.2.17`
- [ ] Update `fasterxml.jackson.version` to `2.15.3`
- [ ] Verify `scoverage.plugin.version` compatibility

### Assessment API POM (assessment-api/pom.xml)
- [ ] Update `scala.major.version` to `2.13`
- [ ] Verify plugin versions

### Assessment Service POM (assessment-api/assessment-service/pom.xml)
- [ ] Update `play2.version` to `3.0.5`
- [ ] Update Play dependencies group ID if needed (some may change to `org.playframework`)
- [ ] Update all `_2.12` to `_2.13` in artifact IDs
- [ ] Update `guava` version to `30.1.1-jre` or later
- [ ] Update `guice` version to `5.1.0` or later
- [ ] Verify `play2-maven-plugin` version
- [ ] Update test dependencies versions

### Assessment Actors POM (assessment-api/assessment-actors/pom.xml)
- [ ] Update external dependencies to Scala 2.13 versions:
  - [ ] `actor-core` → Scala 2.13 + Pekko version
  - [ ] `graph-engine_2.12` → `graph-engine_2.13`
  - [ ] `import-manager` → Scala 2.13 + Pekko version
- [ ] Replace Akka dependency with Pekko:
  - [ ] Remove: `akka-testkit_2.12` version 2.5.22
  - [ ] Add: `pekko-testkit_2.13` version 1.0.3
- [ ] Update ScalaTest version to 3.2.17

### QS Hierarchy Manager POM (assessment-api/qs-hierarchy-manager/pom.xml)
- [ ] Update `graph-engine_2.12` → `graph-engine_2.13`
- [ ] Update `platform-common` to Scala 2.13 version
- [ ] Update ScalaTest version to 3.2.17

### Verification
- [ ] Run `mvn clean compile` to check for dependency issues
- [ ] Resolve any dependency conflicts
- [ ] Verify all dependencies download successfully

---

## Phase 2: Code Migration - Imports

### Akka to Pekko Import Changes

**Search and Replace Pattern**:
```bash
# Run these from project root
find . -name "*.scala" -type f -exec sed -i 's/import akka\.actor/import org.apache.pekko.actor/g' {} +
find . -name "*.scala" -type f -exec sed -i 's/import akka\.pattern/import org.apache.pekko.pattern/g' {} +
find . -name "*.scala" -type f -exec sed -i 's/import akka\.testkit/import org.apache.pekko.testkit/g' {} +
```

### Manual Import Updates (Verify Each)

#### Assessment Module
- [ ] `assessment-api/assessment-service/app/modules/AssessmentModule.scala`
  - [ ] `play.libs.akka.AkkaGuiceSupport` → `play.libs.pekko.PekkoGuiceSupport`
  - [ ] Class extends `PekkoGuiceSupport` instead of `AkkaGuiceSupport`

- [ ] `assessment-api/assessment-service/test/modules/TestModule.scala`
  - [ ] Same changes as AssessmentModule

#### Controllers
- [ ] `app/controllers/BaseController.scala`
  - [ ] `akka.actor.ActorRef` → `org.apache.pekko.actor.ActorRef`
  - [ ] `akka.pattern.Patterns` → `org.apache.pekko.pattern.Patterns`

- [ ] `app/controllers/v4/QuestionSetController.scala`
  - [ ] Update Pekko imports

- [ ] `app/controllers/v4/QuestionController.scala`
  - [ ] Update Pekko imports

- [ ] `app/controllers/v3/ItemSetController.scala`
  - [ ] Update Pekko imports

- [ ] `app/controllers/v5/QuestionSetController.scala`
  - [ ] Update Pekko imports

- [ ] `app/controllers/v5/BaseController.scala`
  - [ ] Update Pekko imports

- [ ] `app/controllers/v5/QuestionController.scala`
  - [ ] Update Pekko imports

- [ ] `app/controllers/HealthController.scala`
  - [ ] Update Pekko imports

#### Handlers
- [ ] `app/handlers/SignalHandler.scala`
  - [ ] `akka.actor.ActorSystem` → `org.apache.pekko.actor.ActorSystem`

#### Test Files
- [ ] `assessment-actors/src/test/scala/org/sunbird/actors/BaseSpec.scala`
  - [ ] Update Pekko actor imports
  - [ ] Update Pekko testkit imports

- [ ] `assessment-actors/src/test/scala/org/sunbird/actors/QuestionActorTest.scala`
  - [ ] Update Pekko imports

- [ ] `assessment-actors/src/test/scala/org/sunbird/actors/QuestionSetActorTest.scala`
  - [ ] Update Pekko imports

- [ ] `assessment-actors/src/test/scala/org/sunbird/actors/TestItemSetActor.scala`
  - [ ] Update Pekko imports

- [ ] `assessment-actors/src/test/scala/org/sunbird/actors/v5/QuestionSetActorTest.scala`
  - [ ] Update Pekko imports

### Scala 2.13 Import Changes

**Search and Replace Pattern**:
```bash
find . -name "*.scala" -type f -exec sed -i 's/import scala\.collection\.JavaConverters/import scala.jdk.CollectionConverters/g' {} +
```

#### Manual Verification Needed
- [ ] Review all files using `JavaConverters._`
- [ ] Update to `CollectionConverters._`
- [ ] Check collection operations for Scala 2.13 changes

### Compilation Check
- [ ] Run `mvn clean compile` on assessment-actors
- [ ] Run `mvn clean compile` on qs-hierarchy-manager
- [ ] Run `mvn clean compile` on assessment-service
- [ ] Fix any remaining compilation errors
- [ ] Document any tricky issues encountered

---

## Phase 3: Configuration Files

### Application Configuration

#### Main Config (assessment-api/assessment-service/conf/application.conf)
- [ ] Replace `akka {` with `pekko {`
- [ ] Update all `akka.` references to `pekko.`
- [ ] Update dispatcher configurations
- [ ] Update actor deployment configurations
- [ ] Update HTTP configuration: `akka.http.` → `pekko.http.`
- [ ] Update request timeout: `akka.request_timeout` → `pekko.request_timeout`
- [ ] Verify all Pekko configuration is correct

#### Test Configurations
- [ ] `assessment-actors/src/test/resources/application.conf`
  - [ ] Update all Akka references to Pekko

- [ ] `qs-hierarchy-manager/src/test/resources/application.conf`
  - [ ] Update all Akka references to Pekko

### Verification
- [ ] Validate configuration syntax
- [ ] Check for any missed `akka` references: `grep -r "akka" conf/`
- [ ] Review dispatcher settings
- [ ] Review router configurations

---

## Phase 4: Code Quality and Compilation

### Compilation
- [ ] Clean build: `mvn clean`
- [ ] Compile all modules: `mvn compile`
- [ ] Fix any compilation errors
- [ ] Ensure zero warnings (if possible)

### Code Review
- [ ] Review all changed files for correctness
- [ ] Check for deprecated API usage
- [ ] Verify pattern matching updates
- [ ] Check Future/Promise usage
- [ ] Review collection operations

### Static Analysis
- [ ] Run Scoverage (if configured)
- [ ] Review code coverage reports
- [ ] Address any critical issues

---

## Phase 5: Testing

### Unit Tests
- [ ] Run all unit tests: `mvn test`
- [ ] Fix failing tests
- [ ] Update test expectations if behavior changed
- [ ] Verify all tests pass
- [ ] Check test coverage maintained or improved

### Test-Specific Issues
- [ ] Update any Akka TestKit usage to Pekko TestKit
- [ ] Verify actor testing works correctly
- [ ] Check mock/stub implementations
- [ ] Validate async test behavior

### Integration Tests
- [ ] Build the service: `mvn clean install -DskipTests=true`
- [ ] Package the service: `mvn play2:dist -pl assessment-service`
- [ ] Run the service locally
- [ ] Verify service starts successfully
- [ ] Check actor system initialization
- [ ] Test health endpoint: `curl http://localhost:9000/health`

### API Testing
- [ ] Test Question API endpoints (v3, v4, v5)
- [ ] Test QuestionSet API endpoints (v4, v5)
- [ ] Test ItemSet API endpoints (v3)
- [ ] Verify all CRUD operations work
- [ ] Test import functionality
- [ ] Test publish functionality
- [ ] Verify error handling

### Functional Tests
- [ ] Run functional test suite (if exists)
- [ ] Verify end-to-end workflows
- [ ] Test with real database connections
- [ ] Validate Kafka integration

---

## Phase 6: Performance Testing

### Baseline Comparison
- [ ] Run performance tests
- [ ] Compare with baseline metrics
- [ ] Check response times (should be within ±5%)
- [ ] Verify throughput maintained
- [ ] Check resource usage (CPU, memory)
- [ ] Monitor actor system performance

### Load Testing
- [ ] Run load tests with typical traffic
- [ ] Run load tests with peak traffic
- [ ] Check for any bottlenecks
- [ ] Verify system stability under load

### Optimization (if needed)
- [ ] Profile slow operations
- [ ] Optimize critical paths
- [ ] Tune Pekko configuration if needed
- [ ] Re-test after optimizations

---

## Phase 7: Documentation

### Code Documentation
- [ ] Update inline comments referencing Akka → Pekko
- [ ] Update ScalaDoc if needed
- [ ] Document any workarounds or special considerations

### Project Documentation
- [ ] Update README.md with new versions
- [ ] Update prerequisites section
- [ ] Update setup instructions
- [ ] Document any new configuration requirements

### Migration Documentation
- [ ] Document issues encountered and solutions
- [ ] Create "lessons learned" document
- [ ] Update UPGRADE_COMPATIBILITY_REPORT.md with actual experience
- [ ] Document any deviations from plan

### Deployment Documentation
- [ ] Update deployment guide
- [ ] Document any infrastructure changes
- [ ] Update rollback procedures
- [ ] Create deployment checklist

---

## Phase 8: Pre-Deployment

### Build Artifacts
- [ ] Create final build: `mvn clean install`
- [ ] Create distribution: `mvn play2:dist -pl assessment-service`
- [ ] Verify artifact size is reasonable
- [ ] Test artifact on clean environment

### Docker
- [ ] Build Docker image
- [ ] Verify Docker image works
- [ ] Test container startup
- [ ] Check container health
- [ ] Validate environment variables

### Deployment Preparation
- [ ] Update Jenkins pipeline if needed
- [ ] Prepare deployment scripts
- [ ] Create deployment runbook
- [ ] Set up monitoring alerts
- [ ] Prepare communication plan

---

## Phase 9: Staging Deployment

### Pre-Deployment
- [ ] Backup staging database
- [ ] Backup staging configuration
- [ ] Document rollback procedure
- [ ] Notify team of deployment

### Deployment
- [ ] Deploy to staging environment
- [ ] Verify deployment successful
- [ ] Check logs for errors
- [ ] Verify actor system started
- [ ] Check all actors are running

### Smoke Tests
- [ ] Test health endpoint
- [ ] Test basic API operations
- [ ] Verify database connectivity
- [ ] Check Kafka integration
- [ ] Verify Neo4j connection

### Staging Validation (2-3 days)
- [ ] Run full test suite in staging
- [ ] Monitor application logs
- [ ] Check error rates
- [ ] Validate performance metrics
- [ ] Get team sign-off

---

## Phase 10: Production Deployment

### Pre-Deployment Checklist
- [ ] Staging validation complete and successful
- [ ] Team trained on changes
- [ ] Monitoring dashboards ready
- [ ] Rollback plan documented and tested
- [ ] Communication sent to stakeholders
- [ ] On-call team briefed

### Deployment Strategy
Choose one:
- [ ] Blue-Green Deployment
- [ ] Canary Deployment
- [ ] Rolling Deployment

### Deployment Execution
- [ ] Backup production database
- [ ] Deploy new version
- [ ] Monitor deployment progress
- [ ] Verify health checks pass
- [ ] Check logs for errors
- [ ] Verify actor system initialization

### Post-Deployment Monitoring (Critical: First 2 hours)
- [ ] Monitor error rates
- [ ] Check response times
- [ ] Monitor resource usage
- [ ] Check actor mailbox sizes
- [ ] Verify no memory leaks
- [ ] Monitor database connections

### Extended Monitoring (First 2 weeks)
- [ ] Daily log reviews
- [ ] Weekly performance reports
- [ ] Monitor for any issues
- [ ] Address any problems immediately

---

## Phase 11: Post-Migration

### Validation
- [ ] All functionality working as expected
- [ ] Performance within acceptable range
- [ ] No critical bugs reported
- [ ] Team comfortable with changes

### Cleanup
- [ ] Remove old Akka references in docs
- [ ] Clean up commented code
- [ ] Update version tags
- [ ] Archive migration branch

### Knowledge Transfer
- [ ] Conduct team retrospective
- [ ] Document lessons learned
- [ ] Update team wiki/documentation
- [ ] Share migration experience with community

### Future Planning
- [ ] Plan for future updates
- [ ] Set up dependency monitoring
- [ ] Schedule regular upgrades
- [ ] Document upgrade process

---

## Rollback Procedure

**If issues are encountered:**

### Immediate Rollback (< 1 hour post-deployment)
- [ ] Stop new version
- [ ] Deploy previous version
- [ ] Verify rollback successful
- [ ] Communicate status to team

### Database Rollback (if needed)
- [ ] Restore database from backup
- [ ] Verify data integrity
- [ ] Test application with restored data

### Post-Rollback
- [ ] Document rollback reason
- [ ] Analyze root cause
- [ ] Plan fixes
- [ ] Schedule re-deployment

---

## Success Criteria

**Migration is complete when ALL of the following are true:**

- ✅ All tests passing (unit, integration, functional)
- ✅ No compilation errors or warnings
- ✅ Performance within ±5% of baseline
- ✅ All APIs functioning correctly
- ✅ Actor system working properly
- ✅ Configuration correctly migrated
- ✅ Documentation updated
- ✅ Team trained and comfortable with changes
- ✅ Production running stable for 2 weeks
- ✅ No critical bugs reported

---

## Notes and Issues Log

Use this section to track issues encountered during migration:

### Issue 1: [Date]
- **Problem**: [Description]
- **Solution**: [How it was resolved]
- **Impact**: [Any impact on timeline/scope]

### Issue 2: [Date]
- **Problem**: [Description]
- **Solution**: [How it was resolved]
- **Impact**: [Any impact on timeline/scope]

*(Add more as needed)*

---

## Sign-Off

### Migration Team Sign-Off
- [ ] Technical Lead: _________________ Date: _______
- [ ] Developer 1: _________________ Date: _______
- [ ] Developer 2: _________________ Date: _______
- [ ] QA Lead: _________________ Date: _______

### Stakeholder Sign-Off
- [ ] Product Owner: _________________ Date: _______
- [ ] Architect: _________________ Date: _______
- [ ] DevOps Lead: _________________ Date: _______

---

**Migration Checklist Version**: 1.0  
**Last Updated**: 2025-10-13  
**Status**: Ready for Use

---

**Remember**: Take your time, test thoroughly, and don't skip steps!
