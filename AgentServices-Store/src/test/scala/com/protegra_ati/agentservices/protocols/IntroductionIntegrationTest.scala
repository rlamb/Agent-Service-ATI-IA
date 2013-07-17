package com.protegra_ati.agentservices.protocols

import com.biosimilarity.evaluator.distribution.ConcreteHL
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineCtor
import com.protegra_ati.agentservices.protocols.msgs._
import java.net.URI
import java.util.UUID
import org.specs2.mutable._
import org.specs2.specification.Scope
import scala.concurrent._
import scala.concurrent.duration.Duration

class IntroductionIntegrationTest extends SpecificationWithJUnit with Tags {
  class ConcreteBaseProtocols extends BaseProtocols

  class Setup extends Scope {
    val engine = new DieselEngineCtor.DieselEngine(Some("eval.conf"))
    val node = new KVDBNodeWrapper(DieselEngineCtor.agent("/dieselProtocol"))
    val baseProtocols = new ConcreteBaseProtocols

    val zuiCnxn = new ConcreteHL.PortableAgentCnxn(new URI("z"), "zui", new URI("ui"))
    val uizCnxn = new ConcreteHL.PortableAgentCnxn(new URI("ui"), "uiz", new URI("z"))
    val zaCnxn = new ConcreteHL.PortableAgentCnxn(new URI("z"), "za", new URI("a"))
    val azCnxn = new ConcreteHL.PortableAgentCnxn(new URI("a"), "az", new URI("z"))
    val zbCnxn = new ConcreteHL.PortableAgentCnxn(new URI("z"), "zb", new URI("b"))
    val bzCnxn = new ConcreteHL.PortableAgentCnxn(new URI("b"), "bz", new URI("z"))
    val auiCnxn = new ConcreteHL.PortableAgentCnxn(new URI("a"), "aui", new URI("ui"))
    val uiaCnxn = new ConcreteHL.PortableAgentCnxn(new URI("ui"), "uia", new URI("a"))
    val buiCnxn = new ConcreteHL.PortableAgentCnxn(new URI("b"), "bui", new URI("ui"))
    val uibCnxn = new ConcreteHL.PortableAgentCnxn(new URI("ui"), "uib", new URI("b"))

    val messageToA = "Message to A"
    val messageToB = "Message to B"

    val aRejectReason = "A Reject Reason"
    val bRejectReason = "B Reject Reason"

    val beginIntroductionRequest = new BeginIntroductionRequest(
      Some(UUID.randomUUID.toString),
      Some(zuiCnxn),
      Some(zaCnxn),
      Some(azCnxn),
      Some(zbCnxn),
      Some(bzCnxn),
      Some(messageToA),
      Some(messageToB))

    baseProtocols.genericIntroducer(node, uizCnxn)
    baseProtocols.genericIntroduced(node, zaCnxn, auiCnxn, uiaCnxn)
    baseProtocols.genericIntroduced(node, zbCnxn, buiCnxn, uibCnxn)

    val fAIRq = baseProtocols.listen(node, auiCnxn, new IntroductionRequest())
    val fBIRq = baseProtocols.listen(node, buiCnxn, new IntroductionRequest())
    val fBIRsp = baseProtocols.listen(node, zuiCnxn, new BeginIntroductionResponse(beginIntroductionRequest.requestId.get))
  }

  sequential
  section("integration", "ia")

  "Introduction protocol" should {
    "Return approved if A and B accept" in new Setup {
      node.publish(uizCnxn)(beginIntroductionRequest)
      val aIntroductionRequest = Await.result(fAIRq, Duration(10, "seconds"))
      val bIntroductionRequest = Await.result(fBIRq, Duration(10, "seconds"))
      val aIntroductionResponse = new IntroductionResponse(aIntroductionRequest.requestId.get, Some(true), None, None, None)
      val bIntroductionResponse = new IntroductionResponse(bIntroductionRequest.requestId.get, Some(true), None, None, None)
      node.put(aIntroductionRequest.responseCnxn.get)(aIntroductionResponse)
      node.put(bIntroductionRequest.responseCnxn.get)(bIntroductionResponse)
      val beginIntroductionResponse = Await.result(fBIRsp, Duration(10, "seconds"))

      beginIntroductionResponse.responseId must be_==(beginIntroductionRequest.requestId.get) and
        (beginIntroductionResponse.accepted must be_==(Some(true))) and
        (beginIntroductionResponse.aRejectReason must be_==(None)) and
        (beginIntroductionResponse.bRejectReason must be_==(None))
    }

    "Return not approved if A accepts and B rejects" in new Setup {
      node.publish(uizCnxn)(beginIntroductionRequest)
      val aIntroductionRequest = Await.result(fAIRq, Duration(10, "seconds"))
      val bIntroductionRequest = Await.result(fBIRq, Duration(10, "seconds"))
      val aIntroductionResponse = new IntroductionResponse(aIntroductionRequest.requestId.get, Some(true), None, None, None)
      val bIntroductionResponse = new IntroductionResponse(bIntroductionRequest.requestId.get, Some(false), None, Some(bRejectReason), None)
      node.put(aIntroductionRequest.responseCnxn.get)(aIntroductionResponse)
      node.put(bIntroductionRequest.responseCnxn.get)(bIntroductionResponse)
      val beginIntroductionResponse = Await.result(fBIRsp, Duration(10, "seconds"))

      beginIntroductionResponse.responseId must be_==(beginIntroductionRequest.requestId.get) and
        (beginIntroductionResponse.accepted must be_==(Some(false))) and
        (beginIntroductionResponse.aRejectReason must be_==(None)) and
        (beginIntroductionResponse.bRejectReason must be_==(Some(bRejectReason)))
    }

    "Return not approved if A rejects and B accepts" in new Setup {
      node.publish(uizCnxn)(beginIntroductionRequest)
      val aIntroductionRequest = Await.result(fAIRq, Duration(10, "seconds"))
      val bIntroductionRequest = Await.result(fBIRq, Duration(10, "seconds"))
      val aIntroductionResponse = new IntroductionResponse(aIntroductionRequest.requestId.get, Some(false), None, Some(aRejectReason), None)
      val bIntroductionResponse = new IntroductionResponse(bIntroductionRequest.requestId.get, Some(true), None, None, None)
      node.put(aIntroductionRequest.responseCnxn.get)(aIntroductionResponse)
      node.put(bIntroductionRequest.responseCnxn.get)(bIntroductionResponse)
      val beginIntroductionResponse = Await.result(fBIRsp, Duration(10, "seconds"))

      beginIntroductionResponse.responseId must be_==(beginIntroductionRequest.requestId.get) and
        (beginIntroductionResponse.accepted must be_==(Some(false))) and
        (beginIntroductionResponse.aRejectReason must be_==(Some(aRejectReason))) and
        (beginIntroductionResponse.bRejectReason must be_==(None))
    }

    "Return not approved if A and B reject" in new Setup {
      node.publish(uizCnxn)(beginIntroductionRequest)
      val aIntroductionRequest = Await.result(fAIRq, Duration(10, "seconds"))
      val bIntroductionRequest = Await.result(fBIRq, Duration(10, "seconds"))
      val aIntroductionResponse = new IntroductionResponse(aIntroductionRequest.requestId.get, Some(false), None, Some(aRejectReason), None)
      val bIntroductionResponse = new IntroductionResponse(bIntroductionRequest.requestId.get, Some(false), None, Some(bRejectReason), None)
      node.put(aIntroductionRequest.responseCnxn.get)(aIntroductionResponse)
      node.put(bIntroductionRequest.responseCnxn.get)(bIntroductionResponse)
      val beginIntroductionResponse = Await.result(fBIRsp, Duration(10, "seconds"))

      beginIntroductionResponse.responseId must be_==(beginIntroductionRequest.requestId.get) and
        (beginIntroductionResponse.accepted must be_==(Some(false))) and
        (beginIntroductionResponse.aRejectReason must be_==(Some(aRejectReason))) and
        (beginIntroductionResponse.bRejectReason must be_==(Some(bRejectReason)))
    }
  }
}
