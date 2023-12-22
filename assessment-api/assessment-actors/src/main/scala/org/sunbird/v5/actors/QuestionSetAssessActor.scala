//package org.sunbird.v5.actors
//
//import org.sunbird.actor.core.BaseActor
//import org.sunbird.common.dto.{Request, Response, ResponseHandler}
//import org.sunbird.graph.OntologyEngineContext
//import org.sunbird.managers.AssessmentManager
//import org.sunbird.utils.AssessmentConstants
//import org.sunbird.v5.managers.AssessmentV5Manager
//
//import java.util
//import javax.inject.Inject
//import scala.concurrent.{ExecutionContext, Future}
//
//class QuestionSetAssessActor @Inject()(implicit oec: OntologyEngineContext) extends BaseActor {
//
//  implicit val ec: ExecutionContext = getContext().dispatcher
//
//  @throws[Throwable]
//  override def onReceive(request: Request): Future[Response] = request.getOperation match {
//    case "assessQuestionSet" => assessment(request)
//    case _ => ERROR(request.getOperation)
//  }
//
//  private def assessment(req: Request): Future[Response] = {
//    val assessments = req.getRequest.getOrDefault(AssessmentConstants.ASSESSMENTS, new util.ArrayList[util.Map[String, AnyRef]]).asInstanceOf[util.List[util.Map[String, AnyRef]]]
//    val quesDoIds = AssessmentV5Manager.validateAssessRequest(req)
//    val list: Response = AssessmentV5Manager.questionList(quesDoIds)
//    AssessmentV5Manager.calculateScore(list, assessments)
//    Future(ResponseHandler.OK.put(AssessmentConstants.QUESTIONS, req.getRequest))
//  }
//
//}
