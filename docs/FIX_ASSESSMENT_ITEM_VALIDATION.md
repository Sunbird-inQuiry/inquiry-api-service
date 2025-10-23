# Fix for AssessmentItem Schema Validation Error

## Problem Description

When creating an AssessmentItem through the `/assessment/v3/items/create` API endpoint, the system was throwing a validation error:

```
Exception occurred - class :org.sunbird.common.exception.ClientException with message :Validation Errors
Result: "Required Metadata objectType not set"
```

This error occurred even after ensuring both top-level and nested `objectType` fields were present.

## Root Cause Analysis

### The Real Issue: Schema Structure Inconsistency

After analyzing the knowledge-platform's DataNode code and comparing with other schemas, the root cause was that **the AssessmentItem schema had an incorrect nested structure**.

**AssessmentItem schema (WRONG - nested structure):**
```json
{
  "required": ["objectType", "metadata"],
  "properties": {
    "objectType": {...},
    "metadata": {
      "required": ["objectType"],
      "properties": {
        /* all actual fields nested here */
      }
    }
  }
}
```

**Other schemas like Question, QuestionSet, ItemSet (CORRECT - flat structure):**
```json
{
  "required": ["name", "code", "mimeType", ...],
  "properties": {
    "name": {...},
    "code": {...},
    "mimeType": {...},
    /* all fields at root level */
  }
}
```

### Why This Caused the Error

The knowledge-platform's `DataNode` expects ALL schemas to follow the **flat pattern**:
1. The controller extracts the request body (e.g., `assessment_item` wrapper)
2. The actor flattens the `metadata` properties to root level
3. DataNode receives a flat structure and validates against the schema

The nested AssessmentItem schema broke this pattern, causing the "Required Metadata objectType not set" error even though the data was present - the schema structure itself was wrong.

## Solution

The fix was to **change the schema structure** to be flat like all other object types, not to change the code.

### 1. Fixed Schema Structure

Changed `schemas/assessmentitem/1.0/schema.json` from nested to flat:

```json
// NEW SCHEMA (flat structure)
{
  "required": ["code", "type"],
  "properties": {
    "code": {"type": "string"},
    "type": {"type": "string", "enum": ["mcq", "mmcq", ...]},
    "mimeType": {"type": "string", "default": "application/vnd.sunbird.assessmentitem"},
    "framework": {"type": "string", "default": "NCF"},
    // ... all other fields at root level
  }
}
```

### 2. Reverted Code Changes

The **original code was correct** - it properly flattens the metadata:

```scala
// This flattening is CORRECT and needed:
requestData.remove("metadata")      // Remove wrapper
requestData.putAll(metadata)        // Flatten to root level
```

This matches the pattern used by Question, QuestionSet, and all other actors.

## Request Structure

### Before the Fix
```json
{
  "objectType": "AssessmentItem",
  "type": "mcq",
  "name": "Test Question",
  "options": [...],
  ...
  // metadata wrapper removed - INVALID!
}
```

### After the Fix
```json
{
  "objectType": "AssessmentItem",
  "metadata": {
    "objectType": "AssessmentItem",  // Now present
    "type": "mcq",
    "name": "Test Question",
    "options": [...],
    ...
  }
}
```

## Files Modified

- `schemas/assessmentitem/1.0/schema.json`
  - Changed from nested to flat structure
  - Removed `objectType` and `metadata` wrapper requirements
  - Made schema consistent with Question, QuestionSet, ItemSet patterns

- `assessment-api/assessment-actors/src/main/scala/org/sunbird/actors/AssessmentItemActor.scala`
  - Reverted to original implementation (the flattening code was correct)

## Testing

To test the fix:

1. Make a POST request to `/assessment/v3/items/create` with a properly structured request:

```json
{
  "request": {
    "objectType": "AssessmentItem",
    "metadata": {
      "type": "mcq",
      "name": "Test MCQ Question",
      "code": "test-mcq-001",
      "body": "{\"data\":{...}}",
      "options": [
        {"value": {"type": "text", "asset": "1"}, "answer": true}
      ],
      "mimeType": "application/vnd.sunbird.assessmentitem",
      "framework": "NCF",
      "board": "CBSE",
      "medium": "English",
      "gradeLevel": ["Class 1"],
      "subject": "English",
      "channel": "test-channel",
      "createdBy": "user-id"
    }
  }
}
```

2. The API should now return a success response with the created item identifier:

```json
{
  "id": "api.assessment.item.create",
  "ver": "3.0",
  "ts": "2025-10-23T...",
  "params": {
    "status": "successful"
  },
  "result": {
    "identifier": "do_123456789"
  }
}
```

## Related Code

The validator (`AssessmentItemValidator.scala`) was already designed to handle both structures:

```scala
val metadata = if (assessmentItem.containsKey("metadata")) {
  assessmentItem.get("metadata").asInstanceOf[util.Map[String, AnyRef]]
} else {
  assessmentItem // If no metadata wrapper, use the request directly
}
```

This flexibility was added to support both the old Sunbird Learning Platform format (flat) and the new format (with metadata wrapper). However, the schema validation in DataNode requires the structured format with the metadata wrapper.

## Compatibility

This fix maintains backward compatibility with:
- The validator's ability to handle both flat and nested structures
- Existing API contracts that expect the metadata wrapper
- The knowledge-platform's DataNode schema validation
