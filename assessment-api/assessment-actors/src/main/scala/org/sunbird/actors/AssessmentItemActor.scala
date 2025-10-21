package org.sunbird.actors

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.StringUtils
import org.sunbird.actor.core.BaseActor
import org.sunbird.common.dto.{Request, Response, ResponseHandler}
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node
import org.sunbird.graph.nodes.DataNode
import org.sunbird.graph.utils.NodeUtil
import org.sunbird.managers.AssessmentManager
import org.sunbird.telemetry.logger.TelemetryManager
import org.sunbird.utils.{AssessmentConstants, RequestUtil}

import java.util
import javax.inject.Inject
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class AssessmentItemActor @Inject()(implicit oec: OntologyEngineContext) extends BaseActor {

  implicit val ec: ExecutionContext = getContext().dispatcher
  private val mapper: ObjectMapper = new ObjectMapper()

  override def onReceive(request: Request): Future[Response] = request.getOperation match {
    case "createItem" => create(request)
    case "readItem" => read(request)
    case "updateItem" => update(request)
    case "retireItem" => retire(request)
    case _ => ERROR(request.getOperation)
  }

  def create(request: Request): Future[Response] = {
    val assessmentItem = request.getRequest
    if (!assessmentItem.containsKey("mimeType")) {
      assessmentItem.put("mimeType", AssessmentConstants.ASSESSMENT_ITEM_MIME_TYPE)
    }
    
    replaceMediaItemsWithVariants(request)
    AssessmentManager.create(request, "ERR_ASSESSMENT_ITEM_CREATE")
  }

  def read(request: Request): Future[Response] = {
    AssessmentManager.read(request, "assessment_item")
  }

  def update(request: Request): Future[Response] = {
    request.getRequest.put("identifier", request.getContext.get("identifier"))
    replaceMediaItemsWithVariants(request)
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

  private def replaceMediaItemsWithVariants(request: Request): Unit = {
    try {
      val assessmentItem = request.getRequest
      val media = assessmentItem.get("media")
      
      if (media != null && StringUtils.isNotBlank(media.toString)) {
        val typeRef = new TypeReference[java.util.List[java.util.Map[String, Object]]]() {}
        val mediaList = mapper.readValue(media.toString, typeRef)
        
        if (mediaList != null && !mediaList.isEmpty) {
          var replaced = false
          val resolution = "low"
          val mediaIterator = mediaList.iterator()
          while (mediaIterator.hasNext) {
            val mediaItem = mediaIterator.next()
            var assetId = mediaItem.get("asset_id")
            if (assetId == null) {
              assetId = mediaItem.get("assetId")
            }
            
            if (assetId != null && StringUtils.isNotBlank(assetId.toString)) {
              try {
                val assetRequest = new Request(request)
                assetRequest.put("identifier", assetId.toString)
                assetRequest.setOperation("getDataNode")
                
                val assetNodeFuture = DataNode.read(assetRequest)
                val assetNode = awaitResult(assetNodeFuture)
                
                if (assetNode != null) {
                  val variantsJSON = assetNode.getMetadata.get("variants")
                  
                  if (variantsJSON != null && StringUtils.isNotBlank(variantsJSON.toString)) {
                    val variantsTypeRef = new TypeReference[java.util.Map[String, String]]() {}
                    val variants = mapper.readValue(variantsJSON.toString, variantsTypeRef)
                    
                    if (variants != null && !variants.isEmpty) {
                      val lowVariantURL = variants.get(resolution)
                      if (StringUtils.isNotEmpty(lowVariantURL)) {
                        mediaItem.put("src", lowVariantURL)
                        replaced = true
                      }
                    }
                  }
                }
              } catch {
                case e: Exception =>
                  TelemetryManager.error(s"Error fetching asset node for id: $assetId", e)
              }
            }
          }
          
          if (replaced) {
            val updatedMedia = mapper.writeValueAsString(mediaList)
            assessmentItem.put("media", updatedMedia)
          }
        }
      }
    } catch {
      case e: Exception =>
        TelemetryManager.error("Error in replaceMediaItemsWithVariants while checking media for replacing with variants: " + e.getMessage, e)
    }
  }

  private def awaitResult(future: Future[Node]): Node = {
    import scala.concurrent.Await
    import scala.concurrent.duration._
    try {
      Await.result(future, 10.seconds)
    } catch {
      case e: Exception =>
        TelemetryManager.error("Error awaiting result", e)
        null
    }
  }
}
