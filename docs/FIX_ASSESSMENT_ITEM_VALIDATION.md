# Fix for AssessmentItem Schema Validation Error

## Problem Description

When creating an AssessmentItem through the `/assessment/v3/items/create` API endpoint, the system was throwing a validation error:

```
Exception occurred - class :org.sunbird.common.exception.ClientException with message :Validation Errors
```

This error occurred even though the request contained the required `objectType` field.

## Root Cause Analysis

### Schema Requirements

The AssessmentItem schema (`schemas/assessmentitem/1.0/schema.json`) requires:

```json
{
  "required": [
    "objectType",     // Required at top level
    "metadata"        // Required at top level
  ],
  "properties": {
    "objectType": {
      "type": "string",
      "enum": ["AssessmentItem"]
    },
    "metadata": {
      "type": "object",
      "required": ["objectType"],  // Also required inside metadata
      "properties": {
        "objectType": {
          "type": "string",
          "enum": ["AssessmentItem"]
        },
        ...
      }
    }
  }
}
```

### The Bug

In `AssessmentItemActor.scala`, the `create()` method was:

1. Extracting the `metadata` object from the request
2. Adding/modifying properties within metadata
3. **Removing the metadata wrapper** and **flattening all properties to the top level**:

```scala
// BEFORE (buggy code):
requestData.remove("metadata")      // Line 70 - removes metadata wrapper
requestData.putAll(metadata)        // Line 71 - flattens all to top level
```

After this flattening:
- ✅ Top-level `objectType` existed
- ❌ Top-level `metadata` object was missing
- ❌ `metadata.objectType` didn't exist (because metadata object was removed)

This violated the schema requirements, causing validation to fail in `DataNode.create()`.

## Solution

The fix involves two changes to `AssessmentItemActor.scala`:

### 1. Preserve the Metadata Structure

Remove the code that flattens the metadata structure:

```scala
// AFTER (fixed code):
// Lines 70-71 removed - no longer flattening metadata
```

This ensures the request maintains the proper structure with the `metadata` object intact.

### 2. Ensure metadata.objectType is Set

Add a check to ensure `metadata.objectType` is present:

```scala
// Ensure metadata.objectType is set for schema validation
if (!metadata.containsKey("objectType")) {
  metadata.put("objectType", "AssessmentItem")
}
```

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

- `assessment-api/assessment-actors/src/main/scala/org/sunbird/actors/AssessmentItemActor.scala`
  - `create()` method: Added metadata.objectType check and removed flattening logic
  - `update()` method: Added metadata.objectType check for consistency

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
