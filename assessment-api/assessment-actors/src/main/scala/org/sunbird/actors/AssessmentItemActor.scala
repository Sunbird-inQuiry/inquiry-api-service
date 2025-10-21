package org.sunbird.actors

import org.apache.commons.lang3.StringUtils
import org.sunbird.actor.core.BaseActor
import org.sunbird.common.dto.{Request, Response, ResponseHandler}
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.nodes.DataNode
import org.sunbird.graph.utils.NodeUtil
import org.sunbird.managers.AssessmentManager
import org.sunbird.utils.{AssessmentConstants, RequestUtil}

import java.util
import javax.inject.Inject
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class AssessmentItemActor @Inject()(implicit oec: OntologyEngineContext) extends BaseActor {

  implicit val ec: ExecutionContext = getContext().dispatcher

  override def onReceive(request: Request): Future[Response] = request.getOperation match {
    case "createItem" => create(request)
    case "readItem" => read(request)
    case "updateItem" => update(request)
    case "retireItem" => retire(request)
    case _ => ERROR(request.getOperation)
  }

  def create(request: Request): Future[Response] = {
    // Validate that assessment item is provided
    val assessmentItem = request.getRequest
    if (assessmentItem == null || assessmentItem.isEmpty) {
      throw new ClientException("ERR_ASSESSMENT_ITEM_CREATE", "Assessment item data is required")
    }
    
    // Set default values if not provided
    if (!assessmentItem.containsKey("mimeType")) {
      assessmentItem.put("mimeType", AssessmentConstants.ASSESSMENT_ITEM_MIME_TYPE)
    }
    
    if (!assessmentItem.containsKey("status")) {
      assessmentItem.put("status", "Draft")
    }
    
    // Validate that required fields are present
    validateRequiredFields(assessmentItem, "create")
    
    // Use AssessmentManager to create
    AssessmentManager.create(request, "ERR_ASSESSMENT_ITEM_CREATE")
  }

  def read(request: Request): Future[Response] = {
    // Validate identifier is provided
    val identifier = request.getRequest.getOrDefault("identifier", "").asInstanceOf[String]
    if (StringUtils.isBlank(identifier)) {
      throw new ClientException("ERR_ASSESSMENT_ITEM_READ", "Assessment item identifier is required")
    }
    
    val fields = request.getRequest.getOrDefault("fields", "").asInstanceOf[String]
      .split(",").filter((field: String) => StringUtils.isNotBlank(field) && !StringUtils.equalsIgnoreCase(field, "null")).toList.asJava
    request.getRequest.put("fields", fields)
    
    DataNode.read(request).map(node => {
      // Check if node is an AssessmentItem
      if (!StringUtils.equalsIgnoreCase(node.getObjectType, "AssessmentItem")) {
        throw new ClientException("ERR_ASSESSMENT_ITEM_READ", s"Node with identifier ${identifier} is not an Assessment Item")
      }
      
      val metadata = NodeUtil.serialize(node, fields, request.getContext.get("schemaName").asInstanceOf[String], request.getContext.get("version").asInstanceOf[String])
      metadata.remove("versionKey")
      ResponseHandler.OK.put("assessment_item", metadata)
    })
  }

  def update(request: Request): Future[Response] = {
    // Set identifier in request
    request.getRequest.put("identifier", request.getContext.get("identifier"))
    
    // Validate that we have data to update
    val updateData = request.getRequest
    if (updateData == null || updateData.isEmpty || updateData.size() <= 1) { // Only identifier
      throw new ClientException("ERR_ASSESSMENT_ITEM_UPDATE", "No data provided for update")
    }
    
    // Restrict properties that shouldn't be updated
    RequestUtil.restrictProperties(request)
    
    // Don't allow changing objectType
    if (updateData.containsKey("objectType")) {
      updateData.remove("objectType")
    }
    
    // Validate required fields if status is being changed to Review or Live
    val status = updateData.getOrDefault("status", "").asInstanceOf[String]
    if (StringUtils.equalsAnyIgnoreCase(status, "Review", "Live")) {
      validateRequiredFields(updateData, "update")
    }
    
    AssessmentManager.getValidatedNodeForUpdate(request, "ERR_ASSESSMENT_ITEM_UPDATE").flatMap(_ => AssessmentManager.updateNode(request))
  }

  def retire(request: Request): Future[Response] = {
    request.getRequest.put("identifier", request.getContext.get("identifier"))
    
    AssessmentManager.getValidatedNodeForRetire(request, "ERR_ASSESSMENT_ITEM_RETIRE").flatMap(node => {
      val updateRequest = new Request(request)
      updateRequest.put("status", "Retired")
      DataNode.update(updateRequest).map(_ => {
        ResponseHandler.OK.put("identifier", node.getIdentifier)
      })
    })
  }
  
  /**
   * Validate required fields for assessment item
   */
  private def validateRequiredFields(data: util.Map[String, AnyRef], operation: String): Unit = {
    val missingFields = new util.ArrayList[String]()
    
    // Check required fields for create
    if (operation == "create") {
      if (!data.containsKey("name") || StringUtils.isBlank(data.get("name").asInstanceOf[String])) {
        missingFields.add("name")
      }
      if (!data.containsKey("code") || StringUtils.isBlank(data.get("code").asInstanceOf[String])) {
        missingFields.add("code")
      }
      if (!data.containsKey("mimeType")) {
        missingFields.add("mimeType")
      }
    }
    
    // For update with status Review/Live, check additional fields
    if (operation == "update") {
      val status = data.getOrDefault("status", "").asInstanceOf[String]
      if (StringUtils.equalsAnyIgnoreCase(status, "Review", "Live")) {
        // Add validation for review/publish - can be enhanced based on item type
        if (!data.containsKey("body") || StringUtils.isBlank(data.get("body").asInstanceOf[String])) {
          missingFields.add("body")
        }
      }
    }
    
    if (!missingFields.isEmpty) {
      throw new ClientException("ERR_ASSESSMENT_ITEM_VALIDATION", 
        s"Missing required fields: ${missingFields.asScala.mkString(", ")}")
    }
  }
}
