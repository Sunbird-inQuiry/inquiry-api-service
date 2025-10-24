package org.sunbird.actors

import org.apache.commons.lang3.StringUtils
import org.sunbird.actor.core.BaseActor
import org.sunbird.common.dto.{Request, Response, ResponseHandler}
import org.sunbird.common.exception.{ClientException, ResourceNotFoundException}
import org.sunbird.common.Platform
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node
import org.sunbird.graph.nodes.DataNode
import org.sunbird.graph.utils.NodeUtil
import org.sunbird.telemetry.logger.TelemetryManager
import org.sunbird.utils.RequestUtil
import org.sunbird.managers.AssessmentManager
import org.sunbird.validators.AssessmentItemValidator
import org.sunbird.utils.JavaJsonUtils

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
//    case "searchItem" => search(request)
    case _ => ERROR(request.getOperation)
  }

  def create(request: Request): Future[Response] = {
    val requestData = request.getRequest
    
    TelemetryManager.info(s"AssessmentItemActor.create: Received requestData with keys: ${requestData.keySet()}")
    
    val skipValidation = requestData.getOrDefault("skipValidation", false.asInstanceOf[AnyRef]).asInstanceOf[Boolean]
    
    val metadata = if (requestData.containsKey("metadata")) {
      requestData.get("metadata").asInstanceOf[util.Map[String, AnyRef]]
    } else {
      throw new ClientException("ERR_ASSESSMENT_ITEM_CREATE", "Assessment Item metadata is missing")
    }
    
    // Ensure objectType is present in metadata
    if (!metadata.containsKey("objectType")) {
      metadata.put("objectType", "AssessmentItem")
    }
    
    if (!metadata.containsKey("mimeType")) {
      metadata.put("mimeType", "application/vnd.sunbird.assessmentitem")
    }
    
    if (!metadata.containsKey("framework") || StringUtils.isBlank(metadata.get("framework").asInstanceOf[String])) {
      metadata.put("framework", getDefaultFramework())
    }

    if (!metadata.containsKey("version")) {
      metadata.put("version", java.lang.Integer.valueOf(1))
    }
    if (metadata.containsKey("level")) {
      metadata.remove("level")
    }

    replaceMediaItemsWithVariants(metadata)
    
    // Extract all metadata fields and put them directly in the request at root level
    // Remove the nested metadata structure completely and work directly with request
    request.getRequest.remove("metadata")
    request.getRequest.putAll(metadata)
    
    // Ensure objectType is at root level for knowledge platform validation
    if (!request.getRequest.containsKey("objectType")) {
      request.getRequest.put("objectType", "AssessmentItem")
    }
    
    if (!skipValidation) {
      TelemetryManager.info(s"AssessmentItemActor.create: Calling validator with request keys: ${request.getRequest.keySet()}")
      AssessmentItemValidator.validateAssessmentItemRequest(request.getRequest, "ASSESSMENT_ITEM_CREATE")
    }
    
    println("Before creating DataNode - request : " + request)
    println("Before creating DataNode - request.getObjectType : " + request.getObjectType)
    DataNode.create(request).map { node =>
      ResponseHandler.OK.put("identifier", node.getIdentifier.replace(".img", ""))
    }
  }

  def read(request: Request): Future[Response] = {
    val fieldsParam = Option(request.get("fields")).map(_.asInstanceOf[String]).getOrElse("")
    val fields: util.List[String] = fieldsParam.split(",")
      .filter(field => StringUtils.isNotBlank(field) && !StringUtils.equalsIgnoreCase(field, "null"))
      .toList.asJava
    request.getRequest.put("fields", fields)
    
    DataNode.read(request).map(node => {
      if (NodeUtil.isRetired(node)) {
        throw new ResourceNotFoundException("ERR_ASSESSMENT_ITEM_NOT_FOUND", "Assessment Item not found with identifier: " + node.getIdentifier)
      }
      
      val metadata: util.Map[String, AnyRef] = NodeUtil.serialize(node, fields, node.getObjectType.toLowerCase.replace("image", ""), request.getContext.get("version").asInstanceOf[String]) 
      metadata.put("identifier", node.getIdentifier.replace(".img", ""))
      ResponseHandler.OK.put("assessment_item", metadata)
    })
  }

  def update(request: Request): Future[Response] = {
    val requestData = request.getRequest
    request.getRequest.put("identifier", request.getContext.get("identifier"))

    val skipValidation = requestData.getOrDefault("skipValidation", false.asInstanceOf[AnyRef]).asInstanceOf[Boolean]
    
    DataNode.read(request).flatMap(existingNode => {
      if (NodeUtil.isRetired(existingNode)) {
        throw new ClientException("ERR_ASSESSMENT_ITEM_UPDATE", "Cannot update retired assessment item: " + existingNode.getIdentifier)
      }
      
      validateUpdatePermissions(request, existingNode)
      
      val metadata = if (requestData.containsKey("metadata")) {
        requestData.get("metadata").asInstanceOf[util.Map[String, AnyRef]]
      } else {
        throw new ClientException("ERR_ASSESSMENT_ITEM_UPDATE", "Assessment Item metadata is missing")
      }
      
      if (!metadata.containsKey("framework")) {
        val existingFramework = existingNode.getMetadata.get("framework")
        if (existingFramework != null) {
          metadata.put("framework", existingFramework)
        }
      }

      if (metadata.containsKey("level")) {
        metadata.remove("level")
      }

      val externalProps = handleExternalProperties(metadata)
      
      if (!skipValidation) {
        AssessmentItemValidator.validateAssessmentItemRequest(requestData, "ASSESSMENT_ITEM_UPDATE")
      }
      replaceMediaItemsWithVariants(metadata)
      DataNode.update(request).map { node =>
        ResponseHandler.OK.put("identifier", node.getIdentifier.replace(".img", ""))
      }
    })
  }

  def retire(request: Request): Future[Response] = {
    request.getRequest.put("identifier", request.getContext.get("identifier"))
    
    DataNode.read(request).flatMap(node => {
      if (NodeUtil.isRetired(node)) {
        throw new ClientException("ERR_ASSESSMENT_ITEM_RETIRE", "Assessment Item is already retired: " + node.getIdentifier)
      }
      validateRetirePermissions(request, node)
      validateAssessmentItemUsage(node)
      
      val updateRequest = new Request(request)
      updateRequest.put("status", "Retired")
      updateRequest.put("lastStatusChangedOn", System.currentTimeMillis().toString)
      updateRequest.put("lastUpdatedOn", System.currentTimeMillis().toString)
      
      DataNode.update(updateRequest).map(updatedNode => {
        ResponseHandler.OK.put("identifier", updatedNode.getIdentifier.replace(".img", ""))
      })
    })
  }

//  def search(request: Request): Future[Response] = {
//    DataNode.search(request).map(nodes => {
//      if (nodes != null && !nodes.isEmpty) {
//        val assessmentItems = nodes.asScala.toList.map { node =>
//          val metadata = NodeUtil.serialize(node, null,
//            node.getObjectType.toLowerCase.replace("image", ""),
//            request.getContext.get("version").asInstanceOf[String])
//          metadata.put("identifier", node.getIdentifier.replace(".img", ""))
//          metadata
//        }.asJava
//
//        ResponseHandler.OK.put("assessment_items", assessmentItems)
//      } else {
//        ResponseHandler.OK.put("assessment_items", new util.ArrayList[util.Map[String, AnyRef]]())
//      }
//    })
//  }

  private def validateUpdatePermissions(request: Request, existingNode: Node): Unit = {
    val currentStatus = existingNode.getMetadata.getOrDefault("status", "Draft").asInstanceOf[String]
    if (StringUtils.equalsIgnoreCase(currentStatus, "Live")) {
      throw new ClientException("ERR_ASSESSMENT_ITEM_UPDATE", "Cannot update live assessment item. Please create a new version.")
    }
  }

  private def validateRetirePermissions(request: Request, node: Node): Unit = {
    val currentStatus = node.getMetadata.getOrDefault("status", "Draft").asInstanceOf[String]
    if (StringUtils.equalsIgnoreCase(currentStatus, "Processing")) {
      throw new ClientException("ERR_ASSESSMENT_ITEM_RETIRE", "Cannot retire assessment item in processing state")
    }
  }

  private def validateAssessmentItemUsage(node: Node): Unit = {
    val inRelations = node.getInRelations
    if (inRelations != null && !inRelations.isEmpty) {
      val activeRelations = inRelations.asScala.filter(rel => 
        rel.getStartNodeMetadata != null && 
        !StringUtils.equalsIgnoreCase(rel.getStartNodeMetadata.getOrDefault("status", "").asInstanceOf[String], "Retired")
      )
      if (activeRelations.nonEmpty) {
        TelemetryManager.warn("Assessment item has active relations but proceeding with retirement: " + node.getIdentifier)
      }
    }
  }

  private def handleExternalProperties(metadata: util.Map[String, AnyRef]): util.Map[String, AnyRef] = {
    // Define external properties based on config (same as Cassandra table columns)
    val externalPropsList = List("body", "editorstate", "question", "solutions")
    
    val externalProps = new util.HashMap[String, AnyRef]()
    
    externalPropsList.foreach { prop =>
      if (metadata.containsKey(prop) && metadata.get(prop) != null) {
        externalProps.put(prop, metadata.get(prop))
        metadata.remove(prop)
      }
    }
    
    externalProps
  }
  
  private def replaceMediaItemsWithVariants(assessmentItem: util.Map[String, AnyRef]): Unit = {
    
    val media = assessmentItem.get("media")
    
    if (media != null && StringUtils.isNotBlank(media.toString)) {
      val mediaList = if (media.isInstanceOf[String]) {
        JavaJsonUtils.deserialize[java.util.List[java.util.Map[String, Object]]](media.toString)
      } else if (media.isInstanceOf[util.List[_]]) {
        media.asInstanceOf[java.util.List[java.util.Map[String, Object]]]
      } else {
        null
      }
      
      if (mediaList != null && !mediaList.isEmpty) {
        var replaced = false
        val resolution = Platform.getString("assessment.media.resolution", "low")
        
        val processedMediaList = mediaList.asScala.map { mediaItem =>
          processMediaItem(mediaItem, resolution) match {
            case Some(updatedItem) =>
              replaced = true
              updatedItem
            case None => mediaItem
          }
        }.asJava
        
        if (replaced) {
          val updatedMedia = JavaJsonUtils.serialize(processedMediaList)
          assessmentItem.put("media", updatedMedia)
        }
      }
    }
  }
  
  private def processExternalMediaInContent(content: String): String = {
    var processedContent = content
    
    val assetPattern = """asset_id['":\s]*([^'",\s}]+)""".r
    assetPattern.findAllMatchIn(content).foreach { matchResult =>
      val assetId = matchResult.group(1)
      if (StringUtils.isNotBlank(assetId)) {
        try {
          val assetRequest = new Request()
          assetRequest.getContext.put("identifier", assetId)
          assetRequest.setOperation("getDataNode")

          DataNode.read(assetRequest).map { assetNode =>
            if (assetNode != null) {
              val variantsJSON = assetNode.getMetadata.get("variants")
              if (variantsJSON != null && StringUtils.isNotBlank(variantsJSON.toString)) {
                val variants = JavaJsonUtils.deserialize[java.util.Map[String, String]](variantsJSON.toString)
                if (variants != null && !variants.isEmpty) {
                  val resolution = Platform.getString("assessment.media.resolution", "low")
                  val variantURL = variants.get(resolution)
                  if (StringUtils.isNotEmpty(variantURL)) {
                    processedContent = processedContent.replace(assetId, variantURL)
                  }
                }
              }
            }
          }
        } catch {
          case e: Exception =>
            TelemetryManager.warn(s"Failed to process external media for asset: $assetId. Error: ${e.getMessage}")
        }
      }
    }
    
    processedContent
  }

  private def processMediaItem(mediaItem: java.util.Map[String, Object], resolution: String): Option[java.util.Map[String, Object]] = {
    var assetId = mediaItem.get("asset_id")
    if (assetId == null) {
      assetId = mediaItem.get("assetId")
    }
    
    if (assetId != null && StringUtils.isNotBlank(assetId.toString)) {
      val assetRequest = new Request()
      assetRequest.getContext.put("identifier", assetId.toString)
      assetRequest.setOperation("getDataNode")

      DataNode.read(assetRequest).map { assetNode =>
        if (assetNode != null) {
        val variantsJSON = assetNode.getMetadata.get("variants")

        if (variantsJSON != null && StringUtils.isNotBlank(variantsJSON.toString)) {
          val variants = JavaJsonUtils.deserialize[java.util.Map[String, String]](variantsJSON.toString)

          if (variants != null && !variants.isEmpty) {
            val variantURL = variants.get(resolution)
            if (StringUtils.isNotEmpty(variantURL)) {
              val updatedMediaItem = new java.util.HashMap[String, Object](mediaItem)
              updatedMediaItem.put("src", variantURL)

              if (variants.containsKey("high")) {
                updatedMediaItem.put("highResolutionSrc", variants.get("high"))
              }
              if (variants.containsKey("medium")) {
                updatedMediaItem.put("mediumResolutionSrc", variants.get("medium"))
              }

              return Some(updatedMediaItem)
            }
          }
        }
      }
      }
    }
    None
  }

  private def getDefaultFramework(): String = {
    Platform.getString("assessment.default.framework", "NCF")
  }
}
