//package controllers.v5
//
//import akka.actor.{ActorRef, ActorSystem}
//import play.api.mvc.ControllerComponents
//import utils.{ActorNames, ApiId, QuestionSetOperations}
//
//import javax.inject.{Inject, Named}
//import scala.concurrent.ExecutionContext
//
//class QuestionSetAssessController @Inject()(@Named(ActorNames.QUESTION_SET_ASSESS_ACTOR) questionSetAssessActor: ActorRef,
//                                            cc: ControllerComponents,
//                                            actorSystem: ActorSystem)(implicit exec: ExecutionContext) extends BaseController(cc) {
//  def assessment() = Action.async { implicit request =>
//    val headers = commonHeaders()
//    val body = requestBody()
//    val questionSetAssessRequest = getRequest(body, headers, QuestionSetOperations.assessQuestionSet.toString)
//    getResult(ApiId.ASSESS_QUESTION_SET, questionSetAssessActor, questionSetAssessRequest)
//  }
//}
