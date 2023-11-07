package controllers.v5

import controllers.base.BaseSpec
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, defaultAwaitTimeout, status}

@RunWith(classOf[JUnitRunner])
class QuestionSetAssessSpec extends BaseSpec {

  val controller = app.injector.instanceOf[controllers.v5.QuestionSetAssessController]

  "create should create an assessment successfully for given valid request" in {
    val result = controller.assessment()(FakeRequest())
    isOK(result)
    status(result)(defaultAwaitTimeout) must equalTo(OK)
  }
}
