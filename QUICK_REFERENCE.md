# Quick Reference Card - Migration Overview

## 📌 At a Glance

```
┌─────────────────────────────────────────────────────────────────┐
│           PLAY FRAMEWORK UPGRADE - QUICK REFERENCE              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Current:  Play 2.7.2 + Scala 2.12.11 + Akka 2.5.22          │
│  Target:   Play 3.0.5 + Scala 2.13.12 + Pekko 1.0.3          │
│                                                                 │
│  Status:   🟡 PLANNING PHASE - DO NOT CODE YET                 │
│  Priority: 🔴 HIGH (License Compliance)                        │
│  Effort:   ~4-6 weeks                                          │
│  Risk:     🟠 MEDIUM-HIGH                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🚨 CRITICAL BLOCKER

```
⚠️  CANNOT START MIGRATION without knowlg-core dependencies:
    
    Required: Scala 2.13 + Pekko 1.0.3 versions of:
    • actor-core
    • graph-engine_2.13
    • platform-common
    • import-manager
    
    ACTION: Contact knowlg-core maintainers FIRST!
```

---

## 📚 Document Guide

| Document | Purpose | Read If... |
|----------|---------|------------|
| **[MIGRATION_EXECUTIVE_SUMMARY.md](./MIGRATION_EXECUTIVE_SUMMARY.md)** | Quick overview | You want the 5-minute summary |
| **[UPGRADE_COMPATIBILITY_REPORT.md](./UPGRADE_COMPATIBILITY_REPORT.md)** | Full analysis | You need complete details |
| **[MIGRATION_CHECKLIST.md](./MIGRATION_CHECKLIST.md)** | Step-by-step guide | You're doing the migration |
| **[MIGRATION_CODE_EXAMPLES.md](./MIGRATION_CODE_EXAMPLES.md)** | Code patterns | You need coding examples |

---

## 🔑 Key Changes Summary

### Imports
```scala
// BEFORE                           // AFTER
import akka.actor._          →     import org.apache.pekko.actor._
import akka.pattern._        →     import org.apache.pekko.pattern._
import play.libs.akka._      →     import play.libs.pekko._
import JavaConverters._      →     import CollectionConverters._
```

### Dependencies
```xml
<!-- BEFORE -->                    <!-- AFTER -->
<scala.version>2.12.11             <scala.version>2.13.12
<play2.version>2.7.2               <play2.version>3.0.5
akka-testkit_2.12                  pekko-testkit_2.13
graph-engine_2.12                  graph-engine_2.13
```

### Configuration
```hocon
# BEFORE                           # AFTER
akka {                             pekko {
  actor { ... }                      actor { ... }
}                                  }
akka.http.parsing...               pekko.http.parsing...
akka.request_timeout               pekko.request_timeout
```

---

## ✅ Pre-Migration Checklist

**Before touching ANY code:**

- [ ] Read MIGRATION_EXECUTIVE_SUMMARY.md
- [ ] Read UPGRADE_COMPATIBILITY_REPORT.md
- [ ] Contact knowlg-core team
- [ ] Confirm dependency availability
- [ ] Get stakeholder approval
- [ ] Allocate 4-6 weeks
- [ ] Set up test environment
- [ ] Create migration branch

---

## 📊 Impact Analysis

### Files to Change
- **5** POM files
- **18** Scala source files
- **3** Configuration files
- **10** Test files
- **3** Documentation files

### Areas Affected
- ✅ Actor system initialization
- ✅ Dependency injection (Guice)
- ✅ All controllers
- ✅ All tests
- ✅ Application configuration
- ⚠️ External dependencies (BLOCKER)

---

## 🎯 Success Criteria

Migration is complete when:
- ✅ All tests pass
- ✅ No compilation errors
- ✅ Performance within ±5%
- ✅ All APIs work
- ✅ Documentation updated
- ✅ Stable for 2 weeks

---

## 🛠️ Quick Commands

### Search for Akka
```bash
grep -r "import akka\." --include="*.scala" .
grep -r "akka\." --include="*.conf" .
```

### Build & Test
```bash
mvn clean compile
mvn test
mvn play2:dist -pl assessment-service
```

### Dependency Check
```bash
mvn dependency:tree | grep -i akka
mvn dependency:tree | grep -i pekko
```

---

## ⚠️ Common Pitfalls

1. ❌ Starting code changes without dependency confirmation
2. ❌ Forgetting to update configuration files
3. ❌ Missing JavaConverters → CollectionConverters
4. ❌ Not updating test dependencies
5. ❌ Skipping thorough testing
6. ❌ Not having a rollback plan

---

## 🔗 Essential Links

- **Pekko Docs**: https://pekko.apache.org/docs/pekko/current/
- **Play 3.0 Migration**: https://www.playframework.com/documentation/3.0.x/Migration30
- **Scala 2.13 Guide**: https://docs.scala-lang.org/overviews/core/collections-migration-213.html

---

## 📞 Need Help?

1. **Technical Questions**: See MIGRATION_CODE_EXAMPLES.md
2. **Process Questions**: See MIGRATION_CHECKLIST.md
3. **Strategic Questions**: See UPGRADE_COMPATIBILITY_REPORT.md
4. **Quick Overview**: See MIGRATION_EXECUTIVE_SUMMARY.md

---

## 🏁 Migration Phases

```
Phase 0: Preparation      (3-5 days)   ← YOU ARE HERE
Phase 1: POMs             (2-3 days)
Phase 2: Code             (5-7 days)
Phase 3: Config           (2-3 days)
Phase 4: Testing          (5-7 days)
Phase 5: Deployment       (2-3 days)
────────────────────────────────────
Total:                    (19-28 days)
```

---

## 💡 Remember

**DO:**
- ✅ Read all documentation first
- ✅ Verify external dependencies
- ✅ Test thoroughly at each step
- ✅ Keep detailed notes
- ✅ Have a rollback plan

**DON'T:**
- ❌ Rush into coding
- ❌ Skip dependency verification
- ❌ Make all changes at once
- ❌ Forget to update tests
- ❌ Deploy without staging validation

---

## 📈 Risk vs Benefit

```
Risk:     ⚠️⚠️⚠️ (Medium-High)
Benefit:  ⭐⭐⭐⭐⭐ (Very High)
Urgency:  🔴🔴🔴🔴 (High)

Verdict: PROCEED (but carefully!)
```

---

**Version**: 1.0  
**Date**: 2025-10-13  
**Status**: Ready for Review

---

**Next Step**: Read [MIGRATION_EXECUTIVE_SUMMARY.md](./MIGRATION_EXECUTIVE_SUMMARY.md)
