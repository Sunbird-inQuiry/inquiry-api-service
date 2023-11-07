package org.sunbird.actors.v5

import akka.actor.Props
import org.scalamock.scalatest.MockFactory
import org.sunbird.actors.BaseSpec
import org.sunbird.common.dto.{Property, Request, Response, ResponseHandler, ResponseParams}
import org.sunbird.common.exception.ResponseCode
import org.sunbird.graph.{GraphService, OntologyEngineContext}
import org.sunbird.graph.dac.model.{Node, SearchCriteria}
import org.sunbird.v5.actors.QuestionActor

import scala.collection.JavaConverters._
import scala.concurrent.Future
import org.sunbird.graph.utils.ScalaJsonUtils
import org.sunbird.actors.CopySpec
import org.sunbird.common.HttpUtil
import org.sunbird.kafka.client.KafkaClient

import scala.concurrent.ExecutionContext.Implicits.global
import java.util

class QuestionActorTest extends BaseSpec with MockFactory {

  "questionActor" should "return failed response for 'unknown' operation" in {
    implicit val oec: OntologyEngineContext = new OntologyEngineContext
    testUnknownOperation(Props(new QuestionActor()), getQuestionRequest())
  }

  it should "send events to kafka topic" in {
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val kfClient = mock[KafkaClient]
    val hUtil = mock[HttpUtil]
    (oec.httpUtil _).expects().returns(hUtil)
    val resp: Response = ResponseHandler.OK()
    resp.put("question", new util.HashMap[String, AnyRef]() {
      {
        put("framework", "NCF")
        put("channel", "test")
        put("status", "Live")
      }
    })
    (hUtil.get(_: String, _: String, _: util.Map[String, String])).expects(*, *, *).returns(resp)
    (oec.kafkaClient _).expects().returns(kfClient)
    (kfClient.send(_: String, _: String)).expects(*, *).returns(None)
    val request = getQuestionRequest()
    request.getRequest.put("question", new util.HashMap[String, AnyRef]() {
      {
        put("source", "https://dock.sunbirded.org/api/question/v1/read/do_113486481122729984143")
        put("metadata", new util.HashMap[String, AnyRef]() {
          {
            put("name", "Test Question")
            put("description", "Test Question")
            put("mimeType", "application/vnd.sunbird.question")
            put("code", "test.ques.1")
            put("primaryCategory", "Learning Resource")
          }
        })
      }
    })
    request.setOperation("importQuestion")
    request.setObjectType("Question")
    val response = callActor(request, Props(new QuestionActor()))
    assert(response.get("processId") != null)
  }

  it should "throw exception for 'listQuestion'" in {
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val request = getQuestionRequest()
    request.put("identifier", null)
    request.put("fields", "")
    request.setOperation("listQuestions")
    val response = callActor(request, Props(new QuestionActor()))
    assert(response.getResponseCode.code == 400)
  }

  it should "throw client exception for 'listQuestion'" in {
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val request = getQuestionRequest()
    request.put("identifiers", util.Arrays.asList("test_id_1", "test_id_2", "test_id_3", "test_id_4", "test_id_5", "test_id_6", "test_id_7", "test_id_8", "test_id_9", "test_id_10", "test_id_11", "test_id_12", "test_id_13", "test_id_14", "test_id_15", "test_id_16", "test_id_17", "test_id_18", "test_id_19", "test_id_20", "test_id_21"))
    request.setOperation("listQuestions")
    request.put("fields", "")
    val response = callActor(request, Props(new QuestionActor()))
    assert(response.getResponseCode.code == 400)
  }

  it should "return error response for 'copyQuestion' when createdFor & createdBy is missing" in {
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val request = CopySpec.getInvalidQuestionSetCopyRequest()
    request.putAll(mapAsJavaMap(Map("identifier" -> "do_1234", "mode" -> "", "copyType" -> "deep")))
    request.setOperation("copyQuestion")
    val response = callActor(request, Props(new QuestionActor()))
    assert("failed".equals(response.getParams.getStatus))
  }

  it should "return error response for 'copyQuestion' when visibility is Parent" in {
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val graphDB = mock[GraphService]
    (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
    val request = CopySpec.getQuestionCopyRequest()
    (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects("domain", "do_1234", false, *).returns(Future(CopySpec.getQuestionNode())).anyNumberOfTimes()
    request.putAll(mapAsJavaMap(Map("identifier" -> "do_1234", "mode" -> "", "copyType" -> "deep")))
    request.setOperation("copyQuestion")
    val response = callActor(request, Props(new QuestionActor()))
    assert("failed".equals(response.getParams.getStatus))
  }

  private def getQuestionRequest(): Request = {
    val request = new Request()
    request.setContext(new java.util.HashMap[String, AnyRef]() {
      {
        put("graph_id", "domain")
        put("version", "1.0")
        put("objectType", "Question")
        put("schemaName", "question")
      }
    })
    request.setObjectType("Question")
    request
  }

  def getDefinitionNode(): Node = {
    val node = new Node()
    node.setIdentifier("obj-cat:practice-question-set_question_all")
    node.setNodeType("DATA_NODE")
    node.setObjectType("ObjectCategoryDefinition")
    node.setGraphId("domain")
    node.setMetadata(mapAsJavaMap(
      ScalaJsonUtils.deserialize[Map[String, AnyRef]]("{\n    \"objectCategoryDefinition\": {\n      \"name\": \"Learning Resource\",\n      \"description\": \"Content Playlist\",\n      \"categoryId\": \"obj-cat:practice_question_set\",\n      \"targetObjectType\": \"Content\",\n      \"objectMetadata\": {\n        \"config\": {},\n        \"schema\": {\n          \"required\": [\n            \"author\",\n            \"copyright\",\n            \"license\",\n            \"audience\"\n          ],\n          \"properties\": {\n            \"audience\": {\n              \"type\": \"array\",\n              \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                  \"Student\",\n                  \"Teacher\"\n                ]\n              },\n              \"default\": [\n                \"Student\"\n              ]\n            },\n            \"mimeType\": {\n              \"type\": \"string\",\n              \"enum\": [\n                \"application/pdf\"\n              ]\n            }\n          }\n        }\n      }\n    }\n  }")))
    node
  }

  def getReadPropsResponseForQuestion(): Response = {
    val response = getSuccessfulResponse()
    response.put("body", "<div class='question-body' tabindex='-1'><div class='mcq-title' tabindex='0'><p><span style=\"background-color:#ffffff;color:#202124;\">Which of the following crops is a commercial crop?</span></p></div><div data-choice-interaction='response1' class='mcq-vertical'></div></div>")
    response.put("editorState", "{\n                \"options\": [\n                    {\n                        \"answer\": false,\n                        \"value\": {\n                            \"body\": \"<p>Wheat</p>\",\n                            \"value\": 0\n                        }\n                    },\n                    {\n                        \"answer\": false,\n                        \"value\": {\n                            \"body\": \"<p>Barley</p>\",\n                            \"value\": 1\n                        }\n                    },\n                    {\n                        \"answer\": false,\n                        \"value\": {\n                            \"body\": \"<p>Maize</p>\",\n                            \"value\": 2\n                        }\n                    },\n                    {\n                        \"answer\": true,\n                        \"value\": {\n                            \"body\": \"<p>Tea</p>\",\n                            \"value\": 3\n                        }\n                    }\n                ],\n                \"question\": \"<p><span style=\\\"background-color:#ffffff;color:#202124;\\\">Which of the following crops is a commercial crop?</span></p>\",\n                \"solutions\": [\n                    {\n                        \"id\": \"f8e65cff-1451-4353-b281-3ceaf874b5b8\",\n                        \"type\": \"html\",\n                        \"value\": \"<p>Tea is the <span style=\\\"background-color:#ffffff;color:#202124;\\\">commercial crop</span></p><figure class=\\\"image image-style-align-left\\\"><img src=\\\"/assets/public/content/assets/do_2137498365362995201237/tea.jpeg\\\" alt=\\\"tea\\\" data-asset-variable=\\\"do_2137498365362995201237\\\"></figure>\"\n                    }\n                ]\n            }")
    response.put("responseDeclaration", "{\n                \"response1\": {\n                    \"maxScore\": 1,\n                    \"cardinality\": \"single\",\n                    \"type\": \"integer\",\n                    \"correctResponse\": {\n                        \"value\": \"3\",\n                        \"outcomes\": {\n                            \"SCORE\": 1\n                        }\n                    },\n                    \"mapping\": [\n                        {\n                            \"response\": 3,\n                            \"outcomes\": {\n                                \"score\": 1\n                            }\n                        }\n                    ]\n                }\n            }")
    response.put("interactions", "{\n                \"response1\": {\n                    \"type\": \"choice\",\n                    \"options\": [\n                        {\n                            \"label\": \"<p>Wheat</p>\",\n                            \"value\": 0\n                        },\n                        {\n                            \"label\": \"<p>Barley</p>\",\n                            \"value\": 1\n                        },\n                        {\n                            \"label\": \"<p>Maize</p>\",\n                            \"value\": 2\n                        },\n                        {\n                            \"label\": \"<p>Tea</p>\",\n                            \"value\": 3\n                        }\n                    ]\n                },\n                \"validation\": {\n                    \"required\": \"Yes\"\n                }\n            }")
    response.put("answer", "")
    //response.put("solutions", "[\n                    {\n                        \"id\": \"f8e65cff-1451-4353-b281-3ceaf874b5b8\",\n                        \"type\": \"html\",\n                        \"value\": \"<p>Tea is the <span style=\\\"background-color:#ffffff;color:#202124;\\\">commercial crop</span></p><figure class=\\\"image image-style-align-left\\\"><img src=\\\"/assets/public/content/assets/do_2137498365362995201237/tea.jpeg\\\" alt=\\\"tea\\\" data-asset-variable=\\\"do_2137498365362995201237\\\"></figure>\"\n                    }\n                ]")
    response.put("instructions", null)
    response.put("media", "[\n                {\n                    \"id\": \"do_2137498365362995201237\",\n                    \"type\": \"image\",\n                    \"src\": \"/assets/public/content/assets/do_2137498365362995201237/tea.jpeg\",\n                    \"baseUrl\": \"https://dev.inquiry.sunbird.org\"\n                }\n            ]")
    response
  }

  def getSuccessfulResponse(): Response = {
    val response = new Response
    val responseParams = new ResponseParams
    responseParams.setStatus("successful")
    response.setParams(responseParams)
    response.setResponseCode(ResponseCode.OK)
    response
  }
}
