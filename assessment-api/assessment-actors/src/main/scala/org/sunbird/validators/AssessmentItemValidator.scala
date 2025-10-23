package org.sunbird.validators

import org.apache.commons.lang3.StringUtils
import org.sunbird.common.exception.ClientException
import org.sunbird.telemetry.logger.TelemetryManager
import org.sunbird.utils.JavaJsonUtils

import java.util
import scala.collection.JavaConverters._
import scala.util.Try

object AssessmentItemValidator {

  private val validTypes = List("mcq", "mmcq", "ftb", "mtf", "ordering", "subjective")

  def validateAssessmentItemRequest(assessmentItem: util.Map[String, AnyRef], operation: String = "ASSESSMENT_ITEM"): Unit = {
    val errorMessages = validateAssessmentItem(assessmentItem)
    if (errorMessages.nonEmpty) {
      throw new ClientException(s"ERR_${operation}_VALIDATION", errorMessages.mkString("; "))
    }
  }

  def validateAssessmentItem(assessmentItem: util.Map[String, AnyRef]): List[String] = {
    val errorMessages = scala.collection.mutable.ListBuffer[String]()
    
    // Debug logging
    TelemetryManager.info(s"AssessmentItemValidator: Received request with keys: ${assessmentItem.keySet()}")
    
    // Extract metadata - the actual assessment item properties are inside metadata in Sunbird Learning Platform structure
    val metadata = if (assessmentItem.containsKey("metadata")) {
      assessmentItem.get("metadata").asInstanceOf[util.Map[String, AnyRef]]
    } else {
      assessmentItem // If no metadata wrapper, use the request directly
    }
    
    val itemType = metadata.get("type")
    TelemetryManager.info(s"AssessmentItemValidator: Extracted type from metadata: $itemType")
    
    val numAnswers = if (isNotBlank(metadata, "num_answers")) {
      Some(metadata.get("num_answers").asInstanceOf[Int])
    } else None
    
    if (isNotBlank(metadata, "hints")) {
      checkJsonList(metadata, errorMessages, "hints", 
        Array("order", "anchor", "content_type", "content", "start", "timing", "on_next"), None)
    }
    
    if (isNotBlank(metadata, "responses")) {
      validateResponses(metadata.get("responses"), errorMessages)
    }
    
    if (itemType != null && StringUtils.isNotEmpty(itemType.toString)) {
      itemType.toString.toLowerCase match {
        case "mcq" =>
          checkJsonList(metadata, errorMessages, "options", Array("value"), Some("mcq"))
        case "mmcq" =>
          checkJsonList(metadata, errorMessages, "options", Array("value"), Some("mmcq"))
        case "ftb" =>
          if (metadata.get("answer") == null) {
            errorMessages += "answer is missing."
          }
          checkAnswers(metadata, errorMessages, numAnswers)
        case "mtf" =>
          checkMtfJson(metadata, errorMessages)
        case "speech_question" | "canvas_question" =>
          if (metadata.get("answer") == null) {
            errorMessages += "answer is missing."
          }
          checkAnswers(metadata, errorMessages, numAnswers)
        case "recognition" =>
          checkJsonList(metadata, errorMessages, "options", Array("value"), Some("recognition"))
        case _ =>
      }
    } else {
      errorMessages += "Assessment Type is Null"
    }
    
    errorMessages.toList
  }

  def isValidAssessmentItemType(itemType: String): Boolean = {
    validTypes.contains(itemType.toLowerCase)
  }

  private def checkAnswers(assessmentItem: util.Map[String, AnyRef], errorMessages: scala.collection.mutable.ListBuffer[String], numAnswers: Option[Int]): Unit = {
    numAnswers.foreach { num =>
      Try {
        val answerValue = assessmentItem.get("answer")
        if (answerValue != null) {
          val answerMap = JavaJsonUtils.deserialize[java.util.Map[String, Object]](answerValue.toString)
          if (num != answerMap.size()) {
            errorMessages += "num_answers is not equals to no. of correct answer."
          }
        }
      }.recover {
        case _ => errorMessages += "answer value is invalid"
      }
    }
  }

  private def validateResponses(responses: AnyRef, errorMessages: scala.collection.mutable.ListBuffer[String]): Unit = {
    Try {
      val responsesList = JavaJsonUtils.deserialize[java.util.List[java.util.Map[String, Object]]](responses.toString)
      if (responsesList != null && !responsesList.isEmpty) {
        responsesList.asScala.foreach { response =>
          if (!isNotBlank(response, "values")) {
            errorMessages += "response must contain values"
            return
          }
          if (!isNotBlank(response, "score") && !isNotBlank(response, "mmc")) {
            errorMessages += "response must have either a score or missing micro-concepts (mmc)"
            return
          }
          val values = response.get("values").asInstanceOf[java.util.Map[String, Object]]
          if (values == null || values.isEmpty) {
            errorMessages += "response must have atleast one value"
            return
          }
        }
      }
    }.recover {
      case e: Exception =>
        TelemetryManager.error("invalid responses definition: " + e.getMessage, e)
        errorMessages += "invalid responses definition"
    }
  }

  private def checkMtfJson(assessmentItem: util.Map[String, AnyRef], errorMessages: scala.collection.mutable.ListBuffer[String]): Unit = {
    val lhsOptions = "lhs_options"
    val lhsKeys = Array("value", "index")
    val rhsOptions = "rhs_options"
    val rhsKeys = Array("value")
    
    if (assessmentItem.get(lhsOptions) == null) {
      errorMessages += s"item $lhsOptions is missing."
    } else {
      Try {
        val values = JavaJsonUtils.deserialize[java.util.List[java.util.Map[String, Object]]](assessmentItem.get(lhsOptions).toString)
        val option1 = scala.collection.mutable.ListBuffer[Object]()
        
        values.asScala.zipWithIndex.foreach { case (value, index) =>
          lhsKeys.foreach { key =>
            if (!value.containsKey(key)) {
              errorMessages += s"invalid assessment item property: $lhsOptions. $key is missing."
              return
            }
          }
          if (value.containsKey("index")) {
            if (option1.contains(value.get("index"))) {
              errorMessages += "index should be unique."
              return
            }
            option1 += value.get("index")
          }
        }
      }.recover {
        case _ => errorMessages += s"invalid assessment item property: $lhsOptions."
      }
    }
    if (assessmentItem.get(rhsOptions) == null) {
      errorMessages += s"item $rhsOptions is missing."
    } else {
      Try {
        val values = JavaJsonUtils.deserialize[java.util.List[java.util.Map[String, Object]]](assessmentItem.get(rhsOptions).toString)
        values.asScala.foreach { value =>
          rhsKeys.foreach { key =>
            if (!value.containsKey(key)) {
              errorMessages += s"invalid assessment item property: $rhsOptions. $key is missing."
              return
            }
          }
        }
      }.recover {
        case _ => errorMessages += s"invalid assessment item property: $rhsOptions."
      }
    }
  }

  private def checkJsonList(assessmentItem: util.Map[String, AnyRef], errorMessages: scala.collection.mutable.ListBuffer[String], 
                           propertyName: String, keys: Array[String], itemType: Option[String]): Unit = {
    val numAnswers = if (assessmentItem.get("num_answers") != null) {
      assessmentItem.get("num_answers").asInstanceOf[Int]
    } else 0
    
    TelemetryManager.info(s"checkJsonList: Checking property '$propertyName' for item type: $itemType")
    
    if (assessmentItem.get(propertyName) == null) {
      errorMessages += s"item $propertyName is missing."
    } else {
      val propertyValue = assessmentItem.get(propertyName)
      TelemetryManager.info(s"checkJsonList: Property '$propertyName' value type: ${propertyValue.getClass.getSimpleName}, value: $propertyValue")
      
      Try {
        val values = if (propertyValue.isInstanceOf[util.List[_]]) {
          // Already a List, cast it directly
          TelemetryManager.info(s"checkJsonList: Property '$propertyName' is already a List")
          propertyValue.asInstanceOf[java.util.List[java.util.Map[String, Object]]]
        } else {
          // String representation, deserialize it
          TelemetryManager.info(s"checkJsonList: Property '$propertyName' is a string, deserializing")
          JavaJsonUtils.deserialize[java.util.List[java.util.Map[String, Object]]](propertyValue.toString)
        }
        
        TelemetryManager.info(s"checkJsonList: Successfully processed '$propertyName' to list with ${values.size()} items")
        
        var answerCount = 0
        
        values.asScala.zipWithIndex.foreach { case (value, index) =>
          TelemetryManager.info(s"checkJsonList: Processing $propertyName item $index with keys: ${value.keySet()}")
          keys.foreach { key =>
            if (!value.containsKey(key)) {
              errorMessages += s"invalid assessment item property: $propertyName. $key is missing."
              return
            }
          }
          
          itemType.foreach { iType =>
            if (value.get("score") != null) {
              val score = value.get("score").asInstanceOf[Int]
              if (score > 0) {
                answerCount += 1
              }
            } else if (value.get("answer") != null) {
              val answer = value.get("answer").asInstanceOf[Boolean]
              if (answer) {
                answerCount += 1
              }
            }
          }
        }
        
        itemType.foreach {
          case "mcq" =>
            if (answerCount < 1) {
              errorMessages += "no option found with answer."
            }
          case "mmcq" =>
            if (answerCount != numAnswers) {
              errorMessages += "num_answers is not equals to no. of correct options"
            }
          case _ =>
        }
      }.recover {
        case e: Exception =>
          TelemetryManager.error(s"checkJsonList: Failed to process '$propertyName': ${e.getMessage}", e)
          errorMessages += s"invalid assessment item property: $propertyName."
      }
    }
  }

  private def isNotBlank(map: java.util.Map[String, Object], key: String): Boolean = {
    if (map != null && StringUtils.isNotBlank(key) && map.containsKey(key)) {
      val value = map.get(key)
      if (value != null && StringUtils.isNotBlank(value.toString)) {
        return true
      }
    }
    false
  }

}
