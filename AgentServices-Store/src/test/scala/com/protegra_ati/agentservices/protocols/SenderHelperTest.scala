package com.protegra_ati.agentservices.protocols

import com.biosimilarity.evaluator.distribution.PortableAgentCnxn
import com.protegra_ati.agentservices.protocols.msgs._
import com.protegra_ati.agentservices.store.{AgentTestNode, TestNodeWrapper}
import java.net.URI
import org.specs2.mutable._
import org.specs2.specification.Scope
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SenderHelperTest extends SpecificationWithJUnit with Tags {
  class ConcreteBaseProtocols extends BaseProtocols

  class Setup extends Scope {
    val node = new TestNodeWrapper(new AgentTestNode)
    val baseProtocols = new ConcreteBaseProtocols
    val cnxn = new PortableAgentCnxn(new URI("a"), "ab", new URI("b"))
  }

  section("unit", "ia")

  "SenderHelper.sendBeginIntroductionResponse" should {
    "send a message" in new Setup() {
      val responseId = "responseId"
      val accepted = false
      val aRejectReason = "aRejectReason"
      val bRejectReason = "bRejectReason"

      baseProtocols.sendBeginIntroductionResponse(node, cnxn, responseId, accepted, Some(aRejectReason), Some(bRejectReason))
      val f = baseProtocols.listen[BeginIntroductionResponse](node, cnxn, new BeginIntroductionResponse(responseId))
      val result = Await.result(f, Duration(10, "seconds"))

      result.responseId must be_==(responseId) and
        (result.accepted must be_==(Some(false))) and
        (result.aRejectReason must be_==(Some(aRejectReason))) and
        (result.bRejectReason must be_==(Some(bRejectReason)))
    }
  }

  "SenderHelper.sendGetIntroductionProfileRequest" should {
    "send a message" in new Setup() {
      val responseCnxn = new PortableAgentCnxn(new URI("b"), "ba", new URI("a"))

      val requestId = baseProtocols.sendGetIntroductionProfileRequest(node, cnxn, responseCnxn)
      val f = baseProtocols.listen[GetIntroductionProfileRequest](node, cnxn, new GetIntroductionProfileRequest())
      val result = Await.result(f, Duration(10, "seconds"))

      result.requestId must be_==(Some(requestId)) and
        (result.responseCnxn must be_==(Some(responseCnxn)))
    }
  }

  "SenderHelper.sendGetIntroductionProfileResponse" should {
    "send a message" in new Setup() {
      val responseId = "responseId"

      baseProtocols.sendGetIntroductionProfileResponse(node, cnxn, responseId)
      val f = baseProtocols.listen[GetIntroductionProfileResponse](node, cnxn, new GetIntroductionProfileResponse(responseId))
      val result = Await.result(f, Duration(10, "seconds"))

      result.responseId must be_==(responseId)
    }
  }

  "SenderHelper.sendIntroductionRequest" should {
    "send a message" in new Setup() {
      val responseCnxn = new PortableAgentCnxn(new URI("b"), "ba", new URI("a"))
      val message = "message"

      val requestId = baseProtocols.sendIntroductionRequest(node, cnxn, responseCnxn, Some(message))
      val f = baseProtocols.listen[IntroductionRequest](node, cnxn, new IntroductionRequest())
      val result = Await.result(f, Duration(10, "seconds"))

      result.requestId must be_==(Some(requestId)) and
        (result.responseCnxn must be_==(Some(responseCnxn))) and
        (result.message must be_==(Some(message)))
    }
  }

  "SenderHelper.sendIntroductionResponse" should {
    "send a message" in new Setup() {
      val responseId = "responseId"
      val accepted = false
      val aliasName = "aliasName"
      val rejectReason = "rejectReason"

      val connectId = baseProtocols.sendIntroductionResponse(node, cnxn, responseId, accepted, Some(aliasName), Some(rejectReason))
      val f = baseProtocols.listen[IntroductionResponse](node, cnxn, new IntroductionResponse(responseId))
      val result = Await.result(f, Duration(10, "seconds"))

      result.responseId must be_==(responseId) and
        (result.accepted must be_==(Some(false))) and
        (result.aliasName must be_==(Some(aliasName))) and
        (result.rejectReason must be_==(Some(rejectReason))) and
        (result.connectId must be_==(Some(connectId)))
    }
  }

  "SenderHelper.sendConnect" should {
    "send a message" in new Setup() {
      val connectId = "connectId"
      val aliasName = "aliasName"
      val writeCnxn = new PortableAgentCnxn(new URI("b"), "bc", new URI("c"))
      val readCnxn = new PortableAgentCnxn(new URI("c"), "cb", new URI("b"))

      baseProtocols.sendConnect(node, cnxn, connectId, Some(aliasName), writeCnxn, readCnxn)
      val f = baseProtocols.listen[Connect](node, cnxn, new Connect(connectId))
      val result = Await.result(f, Duration(10, "seconds"))

      result.connectId must be_==(connectId) and
        (result.aliasName must be_==(Some(aliasName))) and
        (result.writeCnxn must be_==(Some(writeCnxn))) and
        (result.readCnxn must be_==(Some(readCnxn)))
    }
  }
}
