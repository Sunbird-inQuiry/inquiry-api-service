//package org.sunbird.actors.v5.managers
//
//import org.scalamock.matchers.Matchers
//import org.scalamock.scalatest.MockFactory
//
//import java.util
//import org.scalatest.PrivateMethodTester
//import org.sunbird.actors.BaseSpec
//import org.sunbird.common.dto.{Request, Response}
//import org.sunbird.common.exception.{ClientException, ErrorCodes}
//import org.sunbird.graph.{GraphService, OntologyEngineContext}
//import org.sunbird.utils.AssessmentConstants
//import org.sunbird.v5.managers.AssessmentV5Manager
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.fasterxml.jackson.databind.node.{ArrayNode, ObjectNode}
//import org.sunbird.graph.dac.model.Node
//import org.sunbird.v5.managers.AssessmentV5Manager.calculateScore
//
//import scala.collection.JavaConverters._
//
//class AssessmentV5ManagerTest extends BaseSpec with PrivateMethodTester with MockFactory with Matchers {
//
//  val validateAssessRequestMethod = PrivateMethod[Array[String]]('validateAssessRequest)
//  val hideEditorStateAnsMethod = PrivateMethod[String]('hideEditorStateAns)
//  val mapper = new ObjectMapper()
//
//  "validateAssessRequest" should "throw ClientException for invalid token" in {
//    implicit val oec: OntologyEngineContext = new OntologyEngineContext
//
//
//    val requestContext: util.Map[String, AnyRef] = new util.HashMap[String, AnyRef]()
//    requestContext.put("graph_id", "domain")
//
//    val request: Request = new Request(requestContext, new util.HashMap[String, AnyRef](), "assessQuestionSet", "your_object_type")
//    request.put(AssessmentConstants.QUESTION_SET_TOKEN, "invalid_token")
//
//    val thrown = intercept[ClientException] {
//      AssessmentV5Manager invokePrivate validateAssessRequestMethod(request)
//    }
//
//    assert(thrown.getMessage == "Token Authentication Failed")
//  }
//
//  "hideEditorStateAns" should "remove 'answer' from 'options' in JSON with 'answer'" in {
//
//    val mockNode = mock[Node]
//    val editorStateWithAnswer = generateEditorState(optionsWithAnswer = true)
//
//
//    (mockNode.getMetadata _).expects().returning(Map("editorState" -> editorStateWithAnswer).asInstanceOf[Map[String, AnyRef]].asJava)
//
//    // Call the function
//    val result = AssessmentV5Manager invokePrivate hideEditorStateAnsMethod(mockNode)
//
//
//    val resultJson = AssessmentV5Manager.mapper.readTree(result)
//
//
//    resultJson.has("options") shouldBe true
//    val optionsNode = resultJson.get("options").asInstanceOf[ArrayNode]
//    optionsNode.size() shouldBe 2
//
//
//    for (i <- 0 until optionsNode.size()) {
//      val optionNode = optionsNode.get(i).asInstanceOf[ObjectNode]
//      optionNode.has("answer") shouldBe false
//    }
//  }
//
//  "calculateScore" should "work as expected" in {
//    implicit val oec: OntologyEngineContext = new OntologyEngineContext
//
//    // Use ResponseTestHelper to create a Response object
//    val privateList = generateMockPrivateList()
//
//    // Create assessments data
//    val assessmentsData: List[(Long, String, String, String, String, String)] = List(
//      (1681284869464L, "0132677340746629120", "do_213267731619962880127", "843a9940-720f-43ed-a415-26bbfd3da9ef", "5486724f41afb4997118e6d97695684f", "do_2129959063404544001107"),
//      // Add more assessments data as needed
//    )
//
//    // Convert assessments data to util.List[util.Map[String, AnyRef]]
//    val assessments: util.List[util.Map[String, AnyRef]] = createAssessments(assessmentsData)
//
//    calculateScore(privateList, assessments)
//
//    ()
//  }
//
//
//  private def generateEditorState(optionsWithAnswer: Boolean): String = {
//    val mapper = new ObjectMapper()
//    val editorStateNode = mapper.createObjectNode()
//    val optionsNode = mapper.createArrayNode()
//
//    val optionNode1 = mapper.createObjectNode()
//    optionNode1.put("answer", true)
//    optionNode1.set("value", createValueNode("even", 0))
//    if (optionsWithAnswer) optionNode1.put("answer", true)
//
//    val optionNode2 = mapper.createObjectNode()
//    optionNode2.put("answer", false)
//    optionNode2.set("value", createValueNode("odd", 1))
//    if (optionsWithAnswer) optionNode2.put("answer", false)
//
//    optionsNode.add(optionNode1)
//    optionsNode.add(optionNode2)
//
//    editorStateNode.set("options", optionsNode)
//    editorStateNode.put("question", "two is")
//
//    mapper.writeValueAsString(editorStateNode)
//  }
//
//  private def createValueNode(body: String, value: Int): ObjectNode = {
//    val mapper = new ObjectMapper()
//    val valueNode = mapper.createObjectNode()
//    valueNode.put("body", body)
//    valueNode.put("value", value)
//    valueNode
//  }
//
//  def generateMockPrivateList(): Response = {
//    val response = new Response()
//
//    // Adding the 'result' data
//    val resultData = new java.util.HashMap[String, AnyRef]()
//    val questions = new java.util.ArrayList[java.util.Map[String, AnyRef]]()
//
//    // Adding the question data
//    val questionData = new java.util.HashMap[String, AnyRef]()
//    questionData.put("copyright", "compass")
//    questionData.put("channel", "0138325860604395527")
//    questionData.put("maxScore","2")
//    questionData.put("downloadUrl", "https://storageco.blob.core.windows.net/content-storage/question/do_1139217006770667521206/question-3_1699429270605_do_1139217006770667521206_1.ecar")
//
//    // Adding the responseDeclaration data
//    val responseDeclarationData = new java.util.HashMap[String, AnyRef]()
//    val response1Data = new java.util.HashMap[String, AnyRef]()
//    response1Data.put("cardinality", "single")
//    response1Data.put("type", "integer")
//
//    // Adding the mapping data
//    val mappingData = new java.util.ArrayList[java.util.Map[String, AnyRef]]()
//    val mappingItem = new java.util.HashMap[String, AnyRef]()
//    mappingItem.put("value", java.lang.Integer.valueOf(0))
//    mappingItem.put("score", java.lang.Integer.valueOf(10))
//    mappingData.add(mappingItem)
//
//    response1Data.put("mapping", mappingData)
//    responseDeclarationData.put("response1", response1Data)
//    questionData.put("responseDeclaration", responseDeclarationData)
//
//    val editorStateData = new java.util.HashMap[String, AnyRef]()
//    val optionsData = new java.util.ArrayList[java.util.Map[String, AnyRef]]()
//
//    val option1 = new java.util.HashMap[String, AnyRef]()
//    option1.put("value", Map("body" -> "<p>java</p>", "value" -> 0).asJava)
//
//    val option2 = new java.util.HashMap[String, AnyRef]()
//    option2.put("value", Map("body" -> "<p>Python</p>", "value" -> 1).asJava)
//
//    val option3 = new java.util.HashMap[String, AnyRef]()
//    option3.put("value", Map("body" -> "<p>Scala</p>", "value" -> 2).asJava)
//
//    val option4 = new java.util.HashMap[String, AnyRef]()
//    option4.put("value", Map("body" -> "<p>Ruby</p>", "value" -> 3).asJava)
//
//    optionsData.add(option1)
//    optionsData.add(option2)
//    optionsData.add(option3)
//    optionsData.add(option4)
//
//    editorStateData.put("options", optionsData)
//    editorStateData.put("question", "<p>Which programming language is commonly used for developing Apache Flink applications?</p>")
//    questionData.put("editorState", editorStateData)
//
//    questions.add(questionData)
//
//    resultData.put("questions", questions)
//    resultData.put("count", java.lang.Integer.valueOf(1))
//
//    response.put("result", resultData)
//  }
//
//
//  def assessmentData(assessmentTs: Long, batchId: String, collectionId: String, userId: String, attemptId: String, contentId: String): util.Map[String, AnyRef] = {
//    val assessmentMap: util.Map[String, AnyRef] = new util.HashMap[String, AnyRef]()
//    assessmentMap.put("assessmentTs", assessmentTs.asInstanceOf[AnyRef])
//    assessmentMap.put("batchId", batchId)
//    assessmentMap.put("collectionId", collectionId)
//    assessmentMap.put("userId", userId)
//    assessmentMap.put("attemptId", attemptId)
//    assessmentMap.put("contentId", contentId)
//
//    val eventsList: util.List[AnyRef] = new util.ArrayList[AnyRef]()
//    assessmentMap.put("events", eventsList)
//
//    assessmentMap
//  }
//
//  def createAssessment(data: util.Map[String, AnyRef]): util.Map[String, AnyRef] = {
//    val assessmentMap: util.Map[String, AnyRef] = new util.HashMap[String, AnyRef]()
//    assessmentMap.putAll(data)
//    assessmentMap
//  }
//
//  def createAssessments(assessmentsData: List[(Long, String, String, String, String, String)]): util.List[util.Map[String, AnyRef]] = {
//    val assessmentsList: util.List[util.Map[String, AnyRef]] = new util.ArrayList[util.Map[String, AnyRef]]()
//
//    assessmentsData.foreach { assessment =>
//      val assessmentMap: util.Map[String, AnyRef] = new util.HashMap[String, AnyRef]()
//      assessmentMap.put("assessmentTs", assessment._1.asInstanceOf[AnyRef])
//      assessmentMap.put("batchId", assessment._2)
//      assessmentMap.put("collectionId", assessment._3)
//      assessmentMap.put("userId", assessment._4)
//      assessmentMap.put("attemptId", assessment._5)
//      assessmentMap.put("contentId", assessment._6)
//
//      val eventsList: util.List[AnyRef] = new util.ArrayList[AnyRef]()
//      assessmentMap.put("events", eventsList)
//
//      assessmentsList.add(assessmentMap)
//    }
//
//    assessmentsList
//  }
//
//
//  "hideCorrectResponse" should "remove correctResponse from response1 if it exists" in {
//    // Create a Node with the provided responseDeclaration JSON
//    val nodeWithCorrectResponse = createNodeWithResponseDeclaration()
//
//    // Call hideCorrectResponse function
//    val updatedJson = AssessmentV5Manager hideCorrectResponse(nodeWithCorrectResponse)
//
//    // Verify that correctResponse is removed
//    val updatedNode = mapper.readTree(updatedJson)
//    updatedNode.has("response1") shouldBe true
//    val responseNode = updatedNode.get("response1").asInstanceOf[ObjectNode]
//    responseNode.has("correctResponse") shouldBe false
//  }
//
//  it should "return an empty string if response1 or correctResponse is not present" in {
//
//    val nodeWithoutResponseDeclaration = createNodeWithoutResponseDeclaration()
//
//    val result = AssessmentV5Manager hideCorrectResponse(nodeWithoutResponseDeclaration)
//
//    result shouldBe ""
//  }
//
//  "populateSingleCardinality" should "set score and pass in edata based on user response" in {
//    val responseMap = new util.HashMap[String, AnyRef]()
//    responseMap.put(AssessmentConstants.VALUE, Integer.valueOf(1))
//
//    val edataMap = new util.HashMap[String, AnyRef]()
//    edataMap.put(AssessmentConstants.RESVALUES, new util.ArrayList[util.Map[String, AnyRef]]())
//    val resValuesList = edataMap.get(AssessmentConstants.RESVALUES).asInstanceOf[util.ArrayList[util.Map[String, AnyRef]]]
//    resValuesList.add(responseMap)
//
//    val maxScore = 10
//
//    AssessmentV5Manager populateSingleCardinality(responseMap,edataMap,maxScore)
//
//    edataMap.get(AssessmentConstants.SCORE) shouldEqual Integer.valueOf(0)
//    edataMap.get(AssessmentConstants.PASS) shouldEqual AssessmentConstants.NO
//  }
//
//  "populateMultiCardinality" should "work as expected" in {
//    // Create a mock response map
//    val responseMap = new util.HashMap[String, AnyRef]()
//    responseMap.put(AssessmentConstants.CORRECT_RESPONSE, createCorrectResponse())
//
//    // Create a mock edata map
//    val edataMap = createEdataMap()
//    val maxScore = 10
//
//    // Call the populateMultiCardinality method
//    AssessmentV5Manager.populateMultiCardinality(responseMap, edataMap, maxScore)
//
//    // Print some debugging information
//    println(s"Resulting edataMap: $edataMap")
//
//    // Add your assertions here based on the expected behavior of the populateMultiCardinality function
//    // For example, you can check if the edataMap has been updated correctly
//    edataMap.get(AssessmentConstants.SCORE) shouldBe 0.0
//    edataMap.get(AssessmentConstants.PASS) shouldBe AssessmentConstants.NO
//  }
//
//
//  // Helper function to create a correct response map
//  private def createCorrectResponse(): util.Map[String, AnyRef] = {
//    val correctResponse = new util.HashMap[String, AnyRef]()
//    correctResponse.put(AssessmentConstants.VALUE, createCorrectValues())
//    correctResponse
//  }
//
//  // Helper function to create correct values for the correct response
//  private def createCorrectValues(): util.ArrayList[Integer] = {
//    val correctValues = new util.ArrayList[Integer]()
//    // Add some correct values to the list
//    correctValues.add(1)
//    correctValues.add(2)
//    correctValues
//  }
//
//  // Helper function to create an edata map
//  private def createEdataMap(): util.Map[String, AnyRef] = {
//    // Create your edata map here based on the expected structure in your application
//    // ...
//
//    // Return the created map
//    new util.HashMap[String, AnyRef]()
//  }
//
//
//
//
//
//
//  private def createNodeWithoutResponseDeclaration(): Node = {
//    val node = new Node()
//    val metadata = new util.HashMap[String, AnyRef]()
//    metadata.put("responseDeclaration", "")
//    node.setMetadata(metadata)
//    node
//  }
//
//
//  private def createNodeWithResponseDeclaration(): Node = {
//    val node = new Node()
//    if (node.getMetadata == null) {
//      node.setMetadata(new util.HashMap[String, AnyRef]())
//    }
//    val responseDeclaration =
//      """
//        |{
//        |  "response1": {
//        |    "cardinality": "single",
//        |    "type": "integer",
//        |    "mapping": [
//        |      {
//        |        "value": 0,
//        |        "score": 10
//        |      }
//        |    ]
//        |  }
//        |}""".stripMargin
//    node.getMetadata.put("responseDeclaration", responseDeclaration)
//    node
//  }
//
//
//}
