package org.sunbird.actors

import org.apache.commons.lang3.StringUtils
import org.sunbird.actor.core.BaseActor
import org.sunbird.common.dto.{Request, Response, ResponseHandler}
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.nodes.DataNode
import org.sunbird.graph.utils.NodeUtil
import org.sunbird.managers.AssessmentManager

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
    AssessmentManager.create(request, "ERR_ASSESSMENT_ITEM_CREATE")
  }

  def read(request: Request): Future[Response] = {
    val fields = request.getRequest.getOrDefault("fields", "").asInstanceOf[String]
      .split(",").filter((field: String) => StringUtils.isNotBlank(field) && !StringUtils.equalsIgnoreCase(field, "null")).toList.asJava
    request.getRequest.put("fields", fields)
    DataNode.read(request).map(node => {
      val metadata = NodeUtil.serialize(node, fields, request.getContext.get("schemaName").asInstanceOf[String], request.getContext.get("version").asInstanceOf[String])
      metadata.remove("versionKey")
      ResponseHandler.OK.put("assessment_item", metadata)
    })
  }

  def update(request: Request): Future[Response] = {
    request.getRequest.put("identifier", request.getContext.get("identifier"))
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
}
