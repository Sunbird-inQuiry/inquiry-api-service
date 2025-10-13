# Migration Executive Summary

## Quick Reference: Play 3.0.5, Scala 2.13.12 & Pekko 1.0.3 Upgrade

**Status**: 🟡 READY FOR REVIEW (Awaiting Dependency Confirmation)  
**Priority**: 🔴 HIGH (License Compliance)  
**Complexity**: 🟠 MEDIUM-HIGH  
**Estimated Effort**: 4-6 weeks  

---

## Why This Migration is Essential

### The License Problem
Akka changed its license from **Apache 2.0** (open-source) to **Business Source License v1.1** (commercial restrictions) starting with version 2.7+. This means:
- ❌ Cannot use newer Akka versions without potential licensing fees
- ❌ Legal risks for commercial usage
- ❌ Not truly open-source anymore

### The Solution: Apache Pekko
Apache Pekko is a fork of Akka 2.6.x maintained by the Apache Software Foundation:
- ✅ Apache 2.0 License (truly open-source)
- ✅ API-compatible with Akka 2.6.x
- ✅ Active development and security updates
- ✅ No commercial restrictions

---

## What Needs to Change

### Version Upgrades
| Component | Current | Target | Change Type |
|-----------|---------|--------|-------------|
| **Play Framework** | 2.7.2 | 3.0.5 | Major Upgrade |
| **Scala** | 2.12.11 | 2.13.12 | Major Upgrade |
| **Akka** | 2.5.22 | **Pekko 1.0.3** | Framework Switch |
| **Java** | 11 | 11 | No Change ✅ |

### Scope of Changes
- **5 POM files**: Dependency updates
- **18 Scala files**: Import changes, API updates
- **3 Configuration files**: Akka → Pekko config migration
- **10 Test files**: Test framework updates

---

## Critical Blocker: External Dependencies

⚠️ **MIGRATION CANNOT START** until these dependencies are available:

The project depends on `knowlg-core` repository libraries:
- `actor-core` (contains BaseActor)
- `graph-engine_2.12`
- `platform-common`
- `import-manager`

**These must be upgraded to Scala 2.13 + Pekko 1.0.3 FIRST**

**Action Required**:
1. Contact `knowlg-core` maintainers
2. Confirm Scala 2.13 + Pekko versions are available
3. Get version numbers and artifact names
4. Update dependency references

---

## Key Changes Overview

### 1. Import Changes (Akka → Pekko)
```scala
// BEFORE (Akka)
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.Patterns
import play.libs.akka.AkkaGuiceSupport

// AFTER (Pekko)
import org.apache.pekko.actor.{ActorRef, ActorSystem}
import org.apache.pekko.pattern.Patterns
import play.libs.pekko.PekkoGuiceSupport
```

### 2. Configuration Changes
```hocon
# BEFORE (Akka)
akka {
  actor {
    deployment { ... }
  }
}

# AFTER (Pekko)
pekko {
  actor {
    deployment { ... }
  }
}
```

### 3. Dependency Changes
```xml
<!-- BEFORE (Akka) -->
<dependency>
    <groupId>com.typesafe.akka</groupId>
    <artifactId>akka-testkit_2.12</artifactId>
    <version>2.5.22</version>
</dependency>

<!-- AFTER (Pekko) -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-testkit_2.13</artifactId>
    <version>1.0.3</version>
</dependency>
```

### 4. Scala 2.13 Changes
```scala
// BEFORE (Scala 2.12)
import scala.collection.JavaConverters._

// AFTER (Scala 2.13)
import scala.jdk.CollectionConverters._
```

---

## Benefits

### Immediate
- ✅ **License Compliance**: No BSL restrictions
- ✅ **Legal Safety**: Apache 2.0 license
- ✅ **Cost Savings**: No license fees
- ✅ **Security**: Latest security patches

### Long-term
- ✅ **Future-proof**: On actively maintained stack
- ✅ **Community Support**: Apache Foundation backing
- ✅ **Performance**: Modern optimizations
- ✅ **Ecosystem Alignment**: Following Play Framework direction

---

## Risks & Mitigation

| Risk | Level | Mitigation |
|------|-------|------------|
| External dependencies unavailable | 🔴 HIGH | Coordinate with knowlg-core team FIRST |
| Breaking API changes | 🟠 MEDIUM | Thorough testing at each phase |
| Performance regression | 🟡 LOW | Performance testing & benchmarking |
| Configuration errors | 🟠 MEDIUM | Careful config migration, validation |
| Test failures | 🟠 MEDIUM | Update tests alongside code |

---

## Migration Timeline

```
┌─────────────────────────────────────────────────────────────┐
│ PHASE 0: Preparation (3-5 days)                             │
│ • Verify knowlg-core dependencies                           │
│ • Set up environment                                         │
│ • Create migration branch                                    │
├─────────────────────────────────────────────────────────────┤
│ PHASE 1: POM Updates (2-3 days)                            │
│ • Update all dependency versions                             │
│ • Resolve conflicts                                          │
├─────────────────────────────────────────────────────────────┤
│ PHASE 2: Code Migration (5-7 days)                         │
│ • Update Akka → Pekko imports                               │
│ • Update Scala 2.12 → 2.13 syntax                          │
│ • Fix compilation errors                                     │
├─────────────────────────────────────────────────────────────┤
│ PHASE 3: Configuration (2-3 days)                          │
│ • Migrate application.conf                                   │
│ • Update test configurations                                 │
├─────────────────────────────────────────────────────────────┤
│ PHASE 4: Testing (5-7 days)                                │
│ • Unit tests                                                 │
│ • Integration tests                                          │
│ • Performance tests                                          │
├─────────────────────────────────────────────────────────────┤
│ PHASE 5: Deployment (2-3 days)                             │
│ • Staging deployment                                         │
│ • Production rollout                                         │
└─────────────────────────────────────────────────────────────┘

TOTAL: 19-28 days (~1 month)
```

---

## Success Criteria

Migration is successful when:

- ✅ All tests passing (unit, integration, functional)
- ✅ No compilation errors or warnings
- ✅ Performance within ±5% of baseline
- ✅ All APIs functioning correctly
- ✅ Actor system working properly
- ✅ Configuration correctly migrated
- ✅ Documentation updated
- ✅ Team trained on changes

---

## Next Steps

### Before You Start
1. **DO NOT** make code changes yet
2. **DO** read the full compatibility report: `UPGRADE_COMPATIBILITY_REPORT.md`
3. **DO** contact `knowlg-core` maintainers
4. **DO** confirm dependency availability

### When Ready to Start
1. Create migration branch: `git checkout -b migration/play3-pekko`
2. Set up Scala 2.13 development environment
3. Follow the phased approach in detailed report
4. Test extensively at each phase
5. Document all changes and issues

### Need Help?
- 📖 **Full Report**: See `UPGRADE_COMPATIBILITY_REPORT.md`
- 🔗 **Play Migration**: https://www.playframework.com/documentation/3.0.x/Migration30
- 🔗 **Pekko Docs**: https://pekko.apache.org/docs/pekko/current/
- 🔗 **Scala 2.13**: https://docs.scala-lang.org/overviews/core/collections-migration-213.html

---

## Decision Point

### ✅ Proceed with Migration When:
- knowlg-core dependencies confirmed available for Scala 2.13 + Pekko
- Team has allocated 4-6 weeks for migration work
- Testing environment is ready
- Rollback plan is documented

### 🛑 Do Not Proceed If:
- knowlg-core dependencies not available
- Cannot allocate sufficient time for testing
- Critical production issues need attention
- Team lacks Scala/Play expertise

---

## Key Recommendations

1. **🔴 CRITICAL**: Verify `knowlg-core` dependency availability BEFORE starting
2. **🟠 IMPORTANT**: Follow phased approach - don't try to do everything at once
3. **🟡 RECOMMENDED**: Consider Play 2.9.x as intermediate step if needed
4. **✅ BEST PRACTICE**: Test extensively at each phase
5. **✅ BEST PRACTICE**: Keep detailed migration log

---

## Quick Links

- 📄 **Full Compatibility Report**: [`UPGRADE_COMPATIBILITY_REPORT.md`](./UPGRADE_COMPATIBILITY_REPORT.md)
- 📋 **Pre-Migration Checklist**: See Section "Pre-Migration Checklist" in full report
- 🔧 **Technical Details**: See Section "Detailed Migration Steps" in full report
- ⚠️ **Risk Analysis**: See Section "Risk Analysis" in full report
- 💰 **Cost-Benefit**: See Section "Cost-Benefit Analysis" in full report

---

## Document Info

- **Version**: 1.0
- **Created**: 2025-10-13
- **Status**: Draft - Awaiting Review
- **Full Report**: UPGRADE_COMPATIBILITY_REPORT.md

---

**Remember**: This is a significant migration. Take time to plan properly, test thoroughly, and coordinate with all stakeholders before beginning code changes.
