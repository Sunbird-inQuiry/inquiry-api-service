package modules

import com.google.inject.AbstractModule
import org.sunbird.actors.{HealthActor, ItemSetActor, QuestionActor, QuestionSetActor}
import play.libs.akka.AkkaGuiceSupport
import utils.ActorNames

class AssessmentModule extends AbstractModule with AkkaGuiceSupport {

    override def configure() = {
//        super.configure()
        bindActor(classOf[HealthActor], ActorNames.HEALTH_ACTOR)
        bindActor(classOf[ItemSetActor], ActorNames.ITEM_SET_ACTOR)
        bindActor(classOf[QuestionActor], ActorNames.QUESTION_ACTOR)
        bindActor(classOf[QuestionSetActor], ActorNames.QUESTION_SET_ACTOR)
        bindActor(classOf[org.sunbird.v5.actors.QuestionActor], ActorNames.QUESTION_V5_ACTOR)
        bindActor(classOf[org.sunbird.v5.actors.QuestionSetActor], ActorNames.QUESTION_SET_V5_ACTOR)
        bindActor(classOf[org.sunbird.v5.actors.QuestionSetAssessActor], ActorNames.QUESTION_SET_ASSESS_ACTOR)
        println("Initialized application actors for assessment-service")
    }
}
