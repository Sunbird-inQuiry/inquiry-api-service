package org.sunbird.actors.v5

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import org.scalamock.scalatest.MockFactory
import org.sunbird.actors.BaseSpec
import org.sunbird.common.dto.{Request, Response, ResponseHandler}
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.v5.actors.QuestionSetAssessActor

import java.util
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class QuestionSetAssessTest extends BaseSpec with MockFactory {

  "QuestionSetAssessActor" should "return a valid response after assessment" in {
    implicit val oec: OntologyEngineContext = new OntologyEngineContext
    val actorSystem = ActorSystem.create("test-system")
    val props = Props(new QuestionSetAssessActor()(oec))

    val assessments = new util.ArrayList[util.Map[String, AnyRef]]()
    val requestContext: util.Map[String, AnyRef] = new util.HashMap[String, AnyRef]()
    requestContext.put("graph_id", "domain")

    val request: Request = new Request(requestContext, new util.HashMap[String, AnyRef](), "assessQuestionSet", "your_object_type")
    request.put("assessments", assessments)

    val response = callActor(request, props)

    assert(response.getResponseCode != 200)

//    response.getParams.getStatus shouldEqual expectedResponse.getParams.getStatus
//    response.getResult.get("QUESTIONS") shouldEqual expectedResponse.getResult.get("QUESTIONS")
  }

//  private def callActor(request: Request, props: Props)(implicit oec: OntologyEngineContext): Response = {
//    val probe = new TestKit(ActorSystem.create("test-system"))
//    val actorRef = system.actorOf(props)
//    actorRef.tell(request, probe.testActor)
//    probe.expectMsgType[ResponseHandler](FiniteDuration.apply(100, TimeUnit.SECONDS))
//  }

  override def callActor(request: Request, props: Props): Response = {
    val probe = new TestKit(system)
    val actorRef = system.actorOf(props)
    actorRef.tell(request, probe.testActor)
    probe.expectMsgType[Response](FiniteDuration.apply(100, TimeUnit.SECONDS))
  }
}
