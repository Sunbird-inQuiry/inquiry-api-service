package org.sunbird.validators

import org.scalatest.{FlatSpec, Matchers}
import org.sunbird.common.exception.ClientException

import java.util
import scala.collection.JavaConverters._

class AssessmentItemValidatorSpec extends FlatSpec with Matchers {

  "AssessmentItemValidator" should "handle nested assessment_item.metadata structure" in {
    // This mimics the structure from the Actor's extractAssessmentItemData
    val assessmentItem = new util.HashMap[String, AnyRef]()
    val metadata = new util.HashMap[String, AnyRef]()
    
    metadata.put("type", "mcq")
    metadata.put("name", "Test Question")
    metadata.put("code", "TEST_Q1")
    
    assessmentItem.put("objectType", "AssessmentItem")
    assessmentItem.put("metadata", metadata)

    // Should not throw "Assessment Type is Null" error
    val errors = AssessmentItemValidator.validateAssessmentItem(assessmentItem)
    
    // Should have validation errors for missing options, but not for null type
    errors should not contain "Assessment Type is Null"
  }

  it should "handle metadata directly (when passed from Actor)" in {
    val metadata = new util.HashMap[String, AnyRef]()
    metadata.put("type", "mcq")
    metadata.put("name", "Test Question")
    metadata.put("code", "TEST_Q1")

    // Should not throw "Assessment Type is Null" error
    val errors = AssessmentItemValidator.validateAssessmentItem(metadata)
    
    // Should have validation errors for missing options, but not for null type
    errors should not contain "Assessment Type is Null"
  }

  it should "handle full request structure (backward compatibility)" in {
    val requestData = new util.HashMap[String, AnyRef]()
    val assessmentItem = new util.HashMap[String, AnyRef]()
    val metadata = new util.HashMap[String, AnyRef]()
    
    metadata.put("type", "mcq")
    metadata.put("name", "Test Question")
    metadata.put("code", "TEST_Q1")
    
    assessmentItem.put("objectType", "AssessmentItem")
    assessmentItem.put("metadata", metadata)
    requestData.put("assessment_item", assessmentItem)

    // Should not throw "Assessment Type is Null" error
    val errors = AssessmentItemValidator.validateAssessmentItem(requestData)
    
    // Should have validation errors for missing options, but not for null type
    errors should not contain "Assessment Type is Null"
  }

  it should "throw validation error when type is missing in all structures" in {
    val assessmentItem = new util.HashMap[String, AnyRef]()
    val metadata = new util.HashMap[String, AnyRef]()
    
    metadata.put("name", "Test Question")
    metadata.put("code", "TEST_Q1")
    // No type field
    
    assessmentItem.put("objectType", "AssessmentItem")
    assessmentItem.put("metadata", metadata)

    val errors = AssessmentItemValidator.validateAssessmentItem(assessmentItem)
    errors should contain("Assessment Type is Null")
  }
}
