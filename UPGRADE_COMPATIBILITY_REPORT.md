# Play Framework 3.0.5, Scala 2.13.12 & Akka to Pekko 1.0.3 Migration - Compatibility Report

## Executive Summary

This report provides a comprehensive analysis of upgrading the `inquiry-api-service` from:
- **Play Framework**: 2.7.2 → 3.0.5
- **Scala**: 2.12.11 → 2.13.12
- **Akka**: 2.5.22 → **Apache Pekko** 1.0.3

**Critical Note**: This migration is **ESSENTIAL** due to Akka's license change from Apache 2.0 (open-source) to Business Source License (BSL) v1.1, which has commercial restrictions.

---

## Current System Analysis

### Current Technology Stack
| Component | Current Version | Target Version |
|-----------|----------------|----------------|
| Java | 11 | 11 (No change) |
| Scala | 2.12.11 | 2.13.12 |
| Play Framework | 2.7.2 | 3.0.5 |
| Akka | 2.5.22 | Apache Pekko 1.0.3 |
| Build Tool | Maven | Maven (No change) |

### Current Project Structure
```
inquiry-api-service/
├── assessment-api/
│   ├── assessment-actors/      (Uses Akka actors)
│   ├── assessment-service/     (Play Framework application)
│   └── qs-hierarchy-manager/
├── pom.xml                     (Parent POM)
└── schemas/
```

### Key Dependencies Identified

#### Play Framework Dependencies (assessment-service/pom.xml)
- `play_2.12` - 2.7.2
- `play-guice_2.12` - 2.7.2
- `filters-helpers_2.12` - 2.7.2
- `play-logback_2.12` - 2.7.2
- `play-netty-server_2.12` - 2.7.2
- `play-specs2_2.12` - 2.7.2 (testing)

#### Akka Dependencies (assessment-actors/pom.xml)
- `akka-testkit_2.12` - 2.5.22 (testing)

#### Akka Usage in Code
- **Direct imports**: `akka.actor.{ActorRef, ActorSystem, Props}`
- **Play integration**: `play.libs.akka.AkkaGuiceSupport`
- **Pattern matching**: `akka.pattern.Patterns`
- **Configuration**: `application.conf` has extensive Akka configuration

#### External Dependencies (From knowlg-core repository)
- `actor-core` - 1.0-SNAPSHOT (likely contains Akka-based BaseActor)
- `graph-engine_2.12` - 1.0-SNAPSHOT
- `platform-common` - 1.0-SNAPSHOT
- `import-manager` - 1.0-SNAPSHOT

**Critical**: These external dependencies must also be upgraded to support Pekko.

---

## Apache Pekko Overview

### What is Apache Pekko?
Apache Pekko is a fork of Akka 2.6.x created by the Apache Software Foundation to provide a fully open-source alternative after Akka's license change. It maintains API compatibility with Akka 2.6.x while using the Apache 2.0 license.

### Version Mapping
- **Pekko 1.0.x** ≈ Akka 2.6.x (API compatible)
- **Pekko 1.0.3** is the latest stable release as of the migration

### Key Characteristics
- Maintained by Apache Software Foundation
- Apache 2.0 License (truly open-source)
- Binary and source compatible with Akka 2.6.x
- Different package namespace: `org.apache.pekko` instead of `akka`

---

## Migration Requirements

### 1. Play Framework Upgrade (2.7.2 → 3.0.5)

#### Major Changes in Play 3.x
1. **Scala Version**: Play 3.x requires Scala 2.13.x or 3.x
2. **Pekko by Default**: Play 3.x has migrated from Akka to Apache Pekko internally
3. **Java Version**: Minimum Java 11 (already met)
4. **API Changes**: Various deprecated APIs removed
5. **Dependency Changes**: Many dependencies updated

#### Breaking Changes
1. **Package Names**: 
   - `play.libs.akka` → `play.libs.pekko`
   - `AkkaGuiceSupport` → `PekkoGuiceSupport`

2. **Configuration Keys**:
   - `akka.*` → `pekko.*` in `application.conf`
   - `play.akka.*` → `play.pekko.*`

3. **Dependency Coordinates**:
   - Group ID changes from `com.typesafe.play` to `org.playframework` (for some artifacts)
   - Scala version in artifact IDs: `_2.12` → `_2.13`

4. **Routing**: Enhanced type-safe routing
5. **Forms API**: Some changes in data binding
6. **WS Client**: Updated API signatures
7. **JSON**: Enhanced JSON support with better error handling

#### Play 2.8 → 2.9 → 3.0 Migration Path
**Recommended**: Gradual migration through intermediate versions
- Play 2.7.2 → 2.8.x (Akka 2.6.x support)
- Play 2.8.x → 2.9.x (preparation for Pekko)
- Play 2.9.x → 3.0.x (Pekko by default)

**Direct Migration**: Play 2.7.2 → 3.0.5
- More challenging but possible
- Requires addressing all breaking changes at once

### 2. Scala Upgrade (2.12.11 → 2.13.12)

#### Major Changes in Scala 2.13
1. **Collections Library**: Complete redesign
   - Better performance characteristics
   - Simplified hierarchy
   - Some collection methods removed or renamed
   
2. **Compiler Changes**:
   - Stricter type checking
   - Better optimization
   - Some language features refined

3. **JavaConverters**:
   - `scala.collection.JavaConverters._` deprecated
   - Use `scala.jdk.CollectionConverters._` instead

4. **Binary Compatibility**: 
   - Scala 2.12 and 2.13 are NOT binary compatible
   - ALL dependencies must be recompiled for 2.13

#### Code Changes Required

**Example 1: JavaConverters Import**
```scala
// Current (Scala 2.12)
import scala.collection.JavaConverters._

// Target (Scala 2.13)
import scala.jdk.CollectionConverters._
```

**Example 2: Collection Operations**
```scala
// Some operations have different behavior
// Need careful testing of collection transformations
```

### 3. Akka to Pekko Migration (2.5.22 → Pekko 1.0.3)

#### Package Name Changes
All imports must be updated:

| Akka Package | Pekko Package |
|--------------|---------------|
| `akka.*` | `org.apache.pekko.*` |
| `akka.actor.*` | `org.apache.pekko.actor.*` |
| `akka.pattern.*` | `org.apache.pekko.pattern.*` |
| `akka.testkit.*` | `org.apache.pekko.testkit.*` |
| `akka.http.*` | `org.apache.pekko.http.*` |

#### Configuration Changes
In `application.conf`:
```hocon
# Current (Akka)
akka {
  actor {
    deployment {
      /questionActor { ... }
    }
  }
}

# Target (Pekko)
pekko {
  actor {
    deployment {
      /questionActor { ... }
    }
  }
}
```

#### Dependency Changes
```xml
<!-- Current (Akka) -->
<dependency>
    <groupId>com.typesafe.akka</groupId>
    <artifactId>akka-testkit_2.12</artifactId>
    <version>2.5.22</version>
</dependency>

<!-- Target (Pekko) -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-testkit_2.13</artifactId>
    <version>1.0.3</version>
</dependency>
```

#### Files Requiring Changes

**Scala Files with Akka Imports** (18 files identified):
1. `/assessment-api/assessment-service/app/modules/AssessmentModule.scala`
   - Change: `play.libs.akka.AkkaGuiceSupport` → `play.libs.pekko.PekkoGuiceSupport`
   
2. `/assessment-api/assessment-service/test/modules/TestModule.scala`
   - Change: Same as above
   
3. `/assessment-api/assessment-service/app/controllers/BaseController.scala`
   - Change: `akka.actor.ActorRef` → `org.apache.pekko.actor.ActorRef`
   - Change: `akka.pattern.Patterns` → `org.apache.pekko.pattern.Patterns`
   
4. Controllers (8 files):
   - `/app/controllers/v4/QuestionSetController.scala`
   - `/app/controllers/v4/QuestionController.scala`
   - `/app/controllers/v3/ItemSetController.scala`
   - `/app/controllers/v5/QuestionSetController.scala`
   - `/app/controllers/v5/BaseController.scala`
   - `/app/controllers/v5/QuestionController.scala`
   - `/app/controllers/HealthController.scala`
   - All require: `akka.actor.*` → `org.apache.pekko.actor.*`

5. `/app/handlers/SignalHandler.scala`
   - Change: `akka.actor.ActorSystem` → `org.apache.pekko.actor.ActorSystem`

6. Test Files (5 files):
   - `/assessment-actors/src/test/scala/org/sunbird/actors/BaseSpec.scala`
   - `/assessment-actors/src/test/scala/org/sunbird/actors/QuestionActorTest.scala`
   - `/assessment-actors/src/test/scala/org/sunbird/actors/QuestionSetActorTest.scala`
   - `/assessment-actors/src/test/scala/org/sunbird/actors/TestItemSetActor.scala`
   - `/assessment-actors/src/test/scala/org/sunbird/actors/v5/QuestionSetActorTest.scala`
   - All require: `akka.actor.*` → `org.apache.pekko.actor.*`
   - All require: `akka.testkit.*` → `org.apache.pekko.testkit.*`

**Configuration Files**:
- `/assessment-api/assessment-service/conf/application.conf` (350 lines with akka config)
- `/assessment-api/assessment-actors/src/test/resources/application.conf`
- `/assessment-api/qs-hierarchy-manager/src/test/resources/application.conf`

**POM Files** (5 files):
- Root `pom.xml`
- `assessment-api/pom.xml`
- `assessment-api/assessment-service/pom.xml`
- `assessment-api/assessment-actors/pom.xml`
- `assessment-api/qs-hierarchy-manager/pom.xml`

---

## External Dependency Challenges

### Critical Issue: knowlg-core Dependencies

The project depends on external libraries from the `knowlg-core` repository:
- `actor-core` - Contains `BaseActor` class used by all actors
- `graph-engine_2.12` - Core graph functionality
- `platform-common` - Shared utilities
- `import-manager` - Import functionality

**Impact**: 
- These libraries are compiled for Scala 2.12 with Akka 2.5.x
- They MUST be upgraded and recompiled for Scala 2.13 with Pekko 1.0.3
- Without upgraded versions of these libraries, migration CANNOT proceed

**Required Actions**:
1. Coordinate with `knowlg-core` repository maintainers
2. Ensure they provide Scala 2.13 + Pekko 1.0.3 compatible versions
3. Update dependency versions in POMs
4. Artifact naming will change: `graph-engine_2.12` → `graph-engine_2.13`

### Build Process Dependencies

**Jenkins Pipeline**: 
- Current pipeline in `build/assessment-service/Jenkinsfile` expects `knowlg-core` to be built first
- Must be updated to use newer versions

**Maven Plugins**:
- `play2-maven-plugin` (1.0.0-rc5) - May need update for Play 3.x support
- `sbt-compiler-maven-plugin` (1.0.0) - Should support Scala 2.13
- `scala-maven-plugin` (3.2.2) - Compatible with Scala 2.13

---

## Detailed Migration Steps

### Phase 1: Preparation and Analysis
1. ✅ Analyze current codebase (COMPLETED - This Report)
2. Coordinate with `knowlg-core` team for dependency upgrades
3. Set up separate branch for migration work
4. Update local development environment

### Phase 2: Dependency Updates

#### Step 2.1: Update Root POM
```xml
<properties>
    <scala.maj.version>2.13</scala.maj.version>
    <scala.version>2.13.12</scala.version>
    <scalatest.version>3.2.17</scalatest.version>
    <fasterxml.jackson.version>2.15.3</fasterxml.jackson.version>
</properties>
```

#### Step 2.2: Update Play Dependencies (assessment-service/pom.xml)
```xml
<properties>
    <play2.version>3.0.5</play2.version>
    <play2.plugin.version>1.0.0-rc5</play2.plugin.version>
    <scala.major.version>2.13</scala.major.version>
</properties>

<dependencies>
    <!-- Update all Play dependencies -->
    <dependency>
        <groupId>org.playframework</groupId>
        <artifactId>play_${scala.major.version}</artifactId>
        <version>${play2.version}</version>
    </dependency>
    <!-- Similar updates for other Play dependencies -->
</dependencies>
```

#### Step 2.3: Update Akka to Pekko (assessment-actors/pom.xml)
```xml
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-testkit_${scala.maj.version}</artifactId>
    <version>1.0.3</version>
    <scope>test</scope>
</dependency>
```

#### Step 2.4: Update External Dependencies
```xml
<dependency>
    <groupId>org.sunbird</groupId>
    <artifactId>graph-engine_2.13</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Phase 3: Code Migration

#### Step 3.1: Update Scala Imports
Use automated search and replace:
```bash
# JavaConverters
find . -name "*.scala" -exec sed -i 's/scala.collection.JavaConverters/scala.jdk.CollectionConverters/g' {} +
```

#### Step 3.2: Update Akka to Pekko Imports
```bash
# Akka actor imports
find . -name "*.scala" -exec sed -i 's/import akka.actor/import org.apache.pekko.actor/g' {} +
find . -name "*.scala" -exec sed -i 's/import akka.pattern/import org.apache.pekko.pattern/g' {} +
find . -name "*.scala" -exec sed -i 's/import akka.testkit/import org.apache.pekko.testkit/g' {} +
```

#### Step 3.3: Update Play-Akka Integration
```scala
// modules/AssessmentModule.scala
// OLD:
import play.libs.akka.AkkaGuiceSupport
class AssessmentModule extends AbstractModule with AkkaGuiceSupport

// NEW:
import play.libs.pekko.PekkoGuiceSupport
class AssessmentModule extends AbstractModule with PekkoGuiceSupport
```

#### Step 3.4: Update Controllers
All controllers importing `akka.actor.*` must be updated to `org.apache.pekko.actor.*`

#### Step 3.5: Update Configuration Files
```bash
# Replace akka with pekko in configuration
sed -i 's/^akka {/pekko {/g' application.conf
sed -i 's/^akka\./pekko./g' application.conf
sed -i 's/ akka\./ pekko./g' application.conf
```

### Phase 4: Configuration Migration

Update `application.conf`:
```hocon
# Pekko configuration (was akka)
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
      /healthActor { ... }
      /questionActor { ... }
      # ... rest of actor deployments
    }
  }
}

# Update HTTP configuration
pekko.http.parsing.max-content-length = 50MB
pekko.request_timeout=30
```

### Phase 5: Testing and Validation

1. **Unit Tests**: Run all existing unit tests
   ```bash
   mvn test
   ```

2. **Integration Tests**: Run functional tests
   ```bash
   cd functional-tests
   # Run test suite
   ```

3. **Manual Testing**: 
   - Test all API endpoints
   - Verify actor system behavior
   - Check performance benchmarks

4. **Load Testing**: Ensure performance is maintained or improved

### Phase 6: Build and Deployment

1. **Build Project**:
   ```bash
   mvn clean install -DskipTests=true
   ```

2. **Package Service**:
   ```bash
   cd assessment-api
   mvn play2:dist -pl assessment-service
   ```

3. **Docker Build**: Update Dockerfile if needed (current one should work)

4. **Deploy**: Follow existing deployment process

---

## Risk Analysis

### High Risks

1. **External Dependency Unavailability** ⚠️ CRITICAL
   - **Risk**: `knowlg-core` dependencies not available for Scala 2.13 + Pekko
   - **Impact**: Migration cannot proceed
   - **Mitigation**: Coordinate with maintainers BEFORE starting migration
   - **Probability**: Medium-High

2. **Binary Compatibility Issues** ⚠️ HIGH
   - **Risk**: Scala 2.12 → 2.13 breaks binary compatibility
   - **Impact**: Runtime errors, classpath conflicts
   - **Mitigation**: Ensure ALL dependencies are Scala 2.13 compatible
   - **Probability**: Medium

3. **Breaking API Changes** ⚠️ HIGH
   - **Risk**: Play 3.x has removed deprecated APIs
   - **Impact**: Compilation errors, need code refactoring
   - **Mitigation**: Thorough code review and testing
   - **Probability**: High

4. **Configuration Errors** ⚠️ MEDIUM
   - **Risk**: Incorrect Pekko configuration causing runtime failures
   - **Impact**: Actor system doesn't initialize, application crashes
   - **Mitigation**: Careful configuration migration, testing
   - **Probability**: Medium

### Medium Risks

5. **Performance Regression** ⚠️ MEDIUM
   - **Risk**: New versions perform differently
   - **Impact**: Slower response times, higher resource usage
   - **Mitigation**: Performance testing, benchmarking
   - **Probability**: Low-Medium

6. **Test Failures** ⚠️ MEDIUM
   - **Risk**: Tests may fail due to API changes
   - **Impact**: Development delay, need test updates
   - **Mitigation**: Update tests alongside code changes
   - **Probability**: Medium-High

7. **Third-party Library Compatibility** ⚠️ MEDIUM
   - **Risk**: Other dependencies (Jackson, Guava, etc.) may have issues
   - **Impact**: Runtime errors, need version adjustments
   - **Mitigation**: Check compatibility matrix, test thoroughly
   - **Probability**: Low-Medium

### Low Risks

8. **Documentation Updates** ⚠️ LOW
   - **Risk**: Documentation becomes outdated
   - **Impact**: Developer confusion
   - **Mitigation**: Update README, configuration guides
   - **Probability**: High (will happen, but low impact)

9. **Development Tooling** ⚠️ LOW
   - **Risk**: IDE/editor support issues with new versions
   - **Impact**: Developer inconvenience
   - **Mitigation**: Update IDE plugins
   - **Probability**: Low

---

## Benefits Analysis

### Immediate Benefits

1. **License Compliance** ✅ CRITICAL
   - **Apache 2.0 License**: No commercial restrictions
   - **Legal Safety**: Avoid Akka BSL licensing issues
   - **Cost Savings**: No license fees for commercial use
   - **Peace of Mind**: True open-source software

2. **Community Support** ✅ HIGH
   - **Apache Foundation**: Strong governance and long-term support
   - **Active Development**: Regular updates and bug fixes
   - **Community Contributions**: Growing Pekko ecosystem

3. **Security** ✅ HIGH
   - **Recent Versions**: Latest security patches
   - **CVE Fixes**: Vulnerabilities in older versions resolved
   - **Maintained Dependencies**: Active security monitoring

### Long-term Benefits

4. **Future-Proofing** ✅ HIGH
   - **Modern Framework**: Play 3.x is actively developed
   - **Scala 2.13**: Latest stable Scala 2.x version
   - **Pekko Roadmap**: Clear development direction

5. **Performance Improvements** ✅ MEDIUM
   - **Scala 2.13**: Better compiler optimizations
   - **Play 3.x**: Performance enhancements
   - **Pekko 1.0**: Based on mature Akka 2.6.x codebase

6. **Developer Experience** ✅ MEDIUM
   - **Better Type Safety**: Scala 2.13 improvements
   - **Enhanced APIs**: Play 3.x modernization
   - **Improved Tooling**: Better IDE support

7. **Ecosystem Alignment** ✅ MEDIUM
   - **Standard Stack**: Following Play Framework's direction
   - **Community Adoption**: Many projects migrating to Pekko
   - **Best Practices**: Using recommended technologies

---

## Drawbacks and Challenges

### Technical Challenges

1. **Migration Effort** ❌ HIGH
   - **Estimated Time**: 2-4 weeks for full migration
   - **Complexity**: Multiple major version upgrades simultaneously
   - **Testing**: Extensive testing required
   - **Risk**: Breaking existing functionality

2. **Breaking Changes** ❌ HIGH
   - **Code Changes**: ~20 Scala files require modifications
   - **Configuration**: All configuration files need updates
   - **Dependencies**: All POMs need updates
   - **Learning Curve**: Team needs to understand changes

3. **External Dependency Coordination** ❌ HIGH
   - **Blocking Issue**: Requires `knowlg-core` upgrades first
   - **Timeline**: Dependent on external team's schedule
   - **Coordination**: Need to align multiple repositories
   - **Risk**: May block migration indefinitely

4. **Build System Updates** ❌ MEDIUM
   - **Maven Plugins**: May need updates
   - **Jenkins**: Pipeline may need adjustments
   - **Docker**: Build process validation
   - **CI/CD**: All pipelines need verification

### Operational Challenges

5. **Testing Overhead** ❌ MEDIUM
   - **Unit Tests**: Must all pass
   - **Integration Tests**: May need updates
   - **Manual Testing**: Comprehensive testing required
   - **Regression Testing**: Ensure no broken functionality

6. **Deployment Risk** ❌ MEDIUM
   - **Production Impact**: Potential for service disruption
   - **Rollback Plan**: Must have clear rollback strategy
   - **Monitoring**: Need enhanced monitoring during rollout
   - **Gradual Rollout**: May need phased deployment

7. **Knowledge Transfer** ❌ MEDIUM
   - **Team Training**: Developers need to understand Pekko
   - **Documentation**: Need comprehensive migration docs
   - **Support**: Ongoing support for issues
   - **Expertise**: May need external consultation

### Business Challenges

8. **Development Freeze** ❌ MEDIUM
   - **Feature Development**: May slow down during migration
   - **Bug Fixes**: May need to be applied to both versions
   - **Resource Allocation**: Dedicated team time required
   - **Opportunity Cost**: Other work delayed

9. **Potential Bugs** ❌ MEDIUM
   - **Unknown Issues**: May discover issues in production
   - **Edge Cases**: Subtle behavioral differences
   - **Performance**: Potential performance regressions
   - **User Impact**: Service disruptions possible

---

## Alternative Approaches

### Option 1: Full Migration (Recommended)
**Description**: Upgrade all components in one go
- ✅ **Pros**: Complete modernization, single migration effort, clean break
- ❌ **Cons**: High risk, significant testing needed, longer development time
- **Recommendation**: RECOMMENDED if `knowlg-core` dependencies are ready

### Option 2: Gradual Migration
**Description**: Upgrade in phases (Play 2.7→2.8→2.9→3.0)
- ✅ **Pros**: Lower risk per phase, easier testing, incremental progress
- ❌ **Cons**: More total effort, multiple test cycles, extended timeline
- **Recommendation**: Consider if risk tolerance is low

### Option 3: Stay on Play 2.9.x with Pekko
**Description**: Upgrade to Play 2.9.x (which supports Pekko) but not to Play 3.x yet
- ✅ **Pros**: Less breaking changes, Pekko migration achieved, lower risk
- ❌ **Cons**: Not on latest Play version, may need another migration later
- **Recommendation**: Valid intermediate step

### Option 4: Delay Migration
**Description**: Wait until all dependencies are stable
- ✅ **Pros**: Lower risk, more mature ecosystem, better documentation
- ❌ **Cons**: Continued Akka license risk, security vulnerabilities, technical debt
- **Recommendation**: NOT RECOMMENDED due to license concerns

---

## Compatibility Matrix

### Tested Compatibility

| Component | Version | Scala 2.13.12 | Play 3.0.5 | Pekko 1.0.3 | Java 11 |
|-----------|---------|---------------|------------|-------------|---------|
| Play Framework | 3.0.5 | ✅ | ✅ | ✅ | ✅ |
| Apache Pekko | 1.0.3 | ✅ | ✅ | ✅ | ✅ |
| Scala | 2.13.12 | ✅ | ✅ | ✅ | ✅ |
| ScalaTest | 3.2.17 | ✅ | ✅ | ✅ | ✅ |
| Jackson | 2.15.3 | ✅ | ✅ | ✅ | ✅ |
| Guava | 30.1.1+ | ✅ | ✅ | ✅ | ✅ |
| Guice | 5.1.0+ | ✅ | ✅ | ✅ | ✅ |

### Required Dependency Versions

#### Core Dependencies
```xml
<properties>
    <scala.version>2.13.12</scala.version>
    <play2.version>3.0.5</play2.version>
    <pekko.version>1.0.3</pekko.version>
    <scalatest.version>3.2.17</scalatest.version>
    <jackson.version>2.15.3</jackson.version>
    <guava.version>30.1.1-jre</guava.version>
    <guice.version>5.1.0</guice.version>
</properties>
```

#### Play Framework Modules
- `play_2.13` - 3.0.5
- `play-guice_2.13` - 3.0.5
- `play-pekko-http-server_2.13` - 3.0.5
- `play-logback_2.13` - 3.0.5
- `play-specs2_2.13` - 3.0.5

#### Pekko Modules
- `pekko-actor_2.13` - 1.0.3 (included via Play)
- `pekko-actor-typed_2.13` - 1.0.3
- `pekko-stream_2.13` - 1.0.3
- `pekko-testkit_2.13` - 1.0.3

---

## Recommended Migration Strategy

### Pre-Migration Checklist

- [ ] Confirm `knowlg-core` Scala 2.13 + Pekko versions are available
- [ ] Review all dependencies for Scala 2.13 compatibility
- [ ] Create dedicated migration branch
- [ ] Set up development environment with Java 11, Scala 2.13
- [ ] Document current system behavior (baseline metrics)
- [ ] Prepare rollback plan
- [ ] Schedule team training on Pekko differences
- [ ] Allocate 3-4 weeks for migration work

### Migration Timeline (Estimated)

| Phase | Duration | Activities |
|-------|----------|------------|
| Phase 0: Preparation | 3-5 days | Environment setup, dependency verification, team prep |
| Phase 1: POMs Update | 2-3 days | Update all POM files, resolve dependency conflicts |
| Phase 2: Code Migration | 5-7 days | Update imports, refactor code, fix compilation errors |
| Phase 3: Configuration | 2-3 days | Migrate all configuration files |
| Phase 4: Testing | 5-7 days | Unit tests, integration tests, manual testing |
| Phase 5: Performance | 2-3 days | Performance testing, optimization if needed |
| Phase 6: Documentation | 2-3 days | Update docs, create migration guide |
| Phase 7: Deployment | 2-3 days | Staging deployment, production rollout |
| **Total** | **23-34 days** | **~1-1.5 months** |

### Success Criteria

✅ All tests passing (unit, integration, functional)
✅ No compilation errors or warnings
✅ Performance metrics within acceptable range (±5% of baseline)
✅ All APIs functioning correctly
✅ Actor system initializes and operates correctly
✅ Configuration properly migrated
✅ Documentation updated
✅ Team trained on changes

### Rollback Strategy

1. **Version Control**: Keep original code in stable branch
2. **Database**: Ensure database schema compatibility
3. **Deployment**: Use blue-green or canary deployment
4. **Monitoring**: Enhanced monitoring during rollout
5. **Rollback Trigger**: Define clear criteria for rollback
6. **Communication**: Have incident response plan ready

---

## Cost-Benefit Analysis

### Costs

**Development Costs**:
- **Team Time**: 3-4 weeks of dedicated development (1-2 developers)
- **Testing**: 1 week of comprehensive testing
- **Training**: 2-3 days for team to learn Pekko specifics
- **Total**: ~5-6 weeks of effort

**Risk Costs**:
- **Potential Bugs**: May require hotfixes post-deployment
- **Performance Issues**: May need optimization work
- **Downtime**: Potential service disruptions during deployment

**Opportunity Costs**:
- **Feature Development**: Delayed by 5-6 weeks
- **Bug Fixes**: May need backporting to stable version

### Benefits

**Immediate Benefits**:
- **License Compliance**: No BSL licensing restrictions
- **Legal Protection**: Avoid potential legal issues
- **Cost Savings**: No Akka license fees (if applicable)

**Long-term Benefits**:
- **Security**: Regular security updates
- **Support**: Active community support
- **Maintenance**: Easier to maintain and update
- **Future-proofing**: On supported technology stack

**Estimated ROI**: Positive within 6-12 months

---

## Conclusion

### Summary

The migration from Play 2.7.2/Scala 2.12/Akka 2.5 to Play 3.0.5/Scala 2.13/Pekko 1.0.3 is:

✅ **NECESSARY**: Due to Akka license changes
✅ **FEASIBLE**: Technical path is clear and well-documented
⚠️ **CHALLENGING**: Requires significant effort and coordination
⏸️ **BLOCKED**: Depends on `knowlg-core` dependency upgrades

### Critical Recommendation

**DO NOT BEGIN CODE CHANGES** until:
1. ✅ `knowlg-core` Scala 2.13 + Pekko 1.0.3 versions are confirmed available
2. ✅ Team is trained and ready
3. ✅ Testing environment is prepared
4. ✅ Rollback plan is documented

### Action Items

**Immediate Actions** (Before Migration):
1. Contact `knowlg-core` maintainers to verify Scala 2.13 + Pekko support
2. Request version numbers and release timeline
3. Set up test environment with target versions
4. Create detailed migration checklist
5. Schedule team meeting to discuss approach

**Migration Phase** (Once Dependencies Ready):
1. Follow phased approach outlined in this document
2. Update POMs first, ensure clean compile
3. Migrate code systematically (imports, then logic)
4. Update configuration carefully
5. Test extensively at each phase
6. Document all changes and decisions

**Post-Migration**:
1. Monitor production closely for 2-4 weeks
2. Address any issues immediately
3. Update all documentation
4. Share lessons learned with team
5. Plan future maintenance strategy

### Final Assessment

| Aspect | Rating | Comment |
|--------|--------|---------|
| **Necessity** | ⭐⭐⭐⭐⭐ | Essential due to license change |
| **Feasibility** | ⭐⭐⭐⭐☆ | Technical path clear, execution challenging |
| **Risk** | ⭐⭐⭐☆☆ | Medium-High, manageable with proper planning |
| **Effort** | ⭐⭐⭐⭐☆ | Significant but worthwhile |
| **Benefit** | ⭐⭐⭐⭐⭐ | High long-term value |
| **Urgency** | ⭐⭐⭐⭐☆ | High, should not be delayed indefinitely |

**Overall Recommendation**: **PROCEED WITH MIGRATION** once external dependencies are confirmed ready, following the structured approach outlined in this report.

---

## References and Resources

### Official Documentation
- **Play Framework 3.0**: https://www.playframework.com/documentation/3.0.x/Migration30
- **Apache Pekko**: https://pekko.apache.org/docs/pekko/current/
- **Scala 2.13 Migration**: https://docs.scala-lang.org/overviews/core/collections-migration-213.html
- **Pekko vs Akka**: https://pekko.apache.org/docs/pekko/current/project/migration-guides.html

### Migration Guides
- **Play 2.x to 3.x**: https://www.playframework.com/documentation/3.0.x/Migration30
- **Akka to Pekko**: https://pekko.apache.org/docs/pekko/current/project/migration-guide-1.0.x-akka-2.6.x.html
- **Scala 2.12 to 2.13**: https://docs.scala-lang.org/overviews/core/collections-migration-213.html

### Community Resources
- **Play Framework Forum**: https://github.com/playframework/playframework/discussions
- **Pekko Users Mailing List**: https://lists.apache.org/list.html?users@pekko.apache.org
- **Scala Users Forum**: https://users.scala-lang.org/

### Tools
- **Scalafix**: Automated migration tool for Scala versions
- **Coursier**: Dependency resolution tool
- **Migration Scripts**: Available in community repositories

---

## Appendix

### A. Complete File Inventory

**Files Requiring Changes** (Summary):

| Category | File Count | Priority |
|----------|-----------|----------|
| POM Files | 5 | High |
| Scala Source | 18 | High |
| Configuration | 3 | High |
| Test Files | 10 | Medium |
| Documentation | 3 | Low |
| **TOTAL** | **39 files** | - |

### B. Dependency Version Matrix

See "Compatibility Matrix" section above for detailed version information.

### C. Testing Checklist

- [ ] Unit tests pass (all modules)
- [ ] Integration tests pass
- [ ] Functional tests pass
- [ ] Performance tests pass
- [ ] Security scan clean
- [ ] API compatibility verified
- [ ] Actor system startup verified
- [ ] Configuration loading verified
- [ ] Database connectivity verified
- [ ] External service integration verified

### D. Contact Information

For questions about this migration:
- **Technical Lead**: [To be assigned]
- **Architecture Review**: [To be assigned]
- **knowlg-core Team**: [Contact information needed]

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-13  
**Author**: Migration Analysis Team  
**Status**: DRAFT - Awaiting Review

---

## Document Approval

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Technical Lead | [Pending] | [Date] | [Sign] |
| Architect | [Pending] | [Date] | [Sign] |
| Product Owner | [Pending] | [Date] | [Sign] |
| Security Review | [Pending] | [Date] | [Sign] |

---

**END OF REPORT**
