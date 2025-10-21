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
    // Set default mimeType if not provided
    val assessmentItem = request.getRequest
    if (!assessmentItem.containsKey("mimeType")) {
      assessmentItem.put("mimeType", AssessmentConstants.ASSESSMENT_ITEM_MIME_TYPE)
    }
    
    // AssessmentManager.create will use schema-based validation via DataNode.create
    // which validates against schemas/assessmentitem/1.0/schema.json
    AssessmentManager.create(request, "ERR_ASSESSMENT_ITEM_CREATE")
  }

  def read(request: Request): Future[Response] = {
    // Use AssessmentManager.read for consistent behavior
    // This handles field filtering and serialization using schema
    AssessmentManager.read(request, "assessment_item")
  }

  def update(request: Request): Future[Response] = {
    // Set identifier from context
    request.getRequest.put("identifier", request.getContext.get("identifier"))
    
    // RequestUtil.restrictProperties will be called by AssessmentManager
    // Schema-based validation will be applied by DataNode.update
    AssessmentManager.getValidatedNodeForUpdate(request, "ERR_ASSESSMENT_ITEM_UPDATE").flatMap(_ => AssessmentManager.updateNode(request))
  }

  def retire(request: Request): Future[Response] = {
    // Set identifier from context
    request.getRequest.put("identifier", request.getContext.get("identifier"))
    
    // Use AssessmentManager for validation and update
    AssessmentManager.getValidatedNodeForRetire(request, "ERR_ASSESSMENT_ITEM_RETIRE").flatMap(node => {
      val updateRequest = new Request(request)
      updateRequest.put("status", "Retired")
      DataNode.update(updateRequest).map(_ => {
        ResponseHandler.OK.put("identifier", node.getIdentifier)
      })
    })
  }
}
