# Before and After Comparison

## The Problem

### Root Cause: Schema Structure Mismatch

The AssessmentItem schema had a **nested structure** while all other schemas (Question, QuestionSet, ItemSet) use a **flat structure**. This inconsistency caused validation failures in the knowledge-platform's DataNode.

### Schema Comparison

**AssessmentItem (WRONG - nested):**
```json
{
  "required": ["objectType", "metadata"],
  "properties": {
    "objectType": {"enum": ["AssessmentItem"]},
    "metadata": {
      "required": ["objectType"],
      "properties": {
        "objectType": {"enum": ["AssessmentItem"]},
        "type": {...},
        "name": {...},
        // ... all fields nested inside metadata
      }
    }
  }
}
```

**Question/QuestionSet (CORRECT - flat):**
```json
{
  "required": ["name", "code", "mimeType", "primaryCategory"],
  "properties": {
    "name": {...},
    "code": {...},
    "mimeType": {...},
    "type": {...},
    // ... all fields at root level
  }
}
```

### Why This Failed

The knowledge-platform DataNode expects:
1. Controller extracts request body wrapper
2. Actor flattens `metadata` to root level
3. DataNode validates against **flat schema**

The nested AssessmentItem schema broke step 3, causing "Required Metadata objectType not set" error.

---

## The Solution

### Fixed Schema Structure (AFTER)

```json
{
  "required": ["code", "type"],
  "properties": {
    "code": {"type": "string"},
    "type": {"type": "string", "enum": ["mcq", "mmcq", ...]},
    "mimeType": {"type": "string", "default": "application/vnd.sunbird.assessmentitem"},
    "framework": {"type": "string", "default": "NCF"},
    "name": {"type": "string"},
    "options": {"type": "array"},
    // ... all other fields at root level
  }
}
```

### Code Was Already Correct!

The original flattening code was RIGHT - it matches the pattern used by all other actors:

```scala
// This is CORRECT and necessary:
replaceMediaItemsWithVariants(metadata)
requestData.remove("metadata")      // Remove wrapper
requestData.putAll(metadata)        // Flatten to root
```

---

## Changes Summary

### File: `schemas/assessmentitem/1.0/schema.json`

```diff
{
-  "required": ["objectType", "metadata"],
+  "required": ["code", "type"],
   "properties": {
-    "objectType": {"enum": ["AssessmentItem"]},
-    "metadata": {
-      "required": ["objectType"],
-      "properties": {
-        "objectType": {"enum": ["AssessmentItem"]},
         "code": {"type": "string"},
         "type": {"type": "string", "enum": [...]},
         "mimeType": {"type": "string", "default": "..."},
         // ... all other fields
-      }
-    }
   }
}
```

### File: `assessment-api/assessment-actors/src/main/scala/org/sunbird/actors/AssessmentItemActor.scala`

**Reverted to original** - the flattening code was correct all along!

```scala
// This code is CORRECT:
replaceMediaItemsWithVariants(metadata)
requestData.remove("metadata")
requestData.putAll(metadata)
```

---

## Impact

### Lines Changed: ~110 lines
- **Schema file**: Complete restructuring from nested to flat
- **Code file**: Reverted to original (no net change)

### Files Changed: 1 (schema only)
- `schemas/assessmentitem/1.0/schema.json` - restructured to flat

### Scope: Schema fix only
✅ Fixed schema structure to match other objects
✅ Original code was already correct
✅ No behavior changes needed in the actor
✅ Maintains consistency with knowledge-platform patterns
