package com.protegra_ati.agentservices.protocols

import com.biosimilarity.evaluator.distribution.ConcreteHL
import com.protegra_ati.agentservices.protocols.msgs._
import java.util.UUID

trait SenderHelper {
  def sendBeginIntroductionResponse(
    node: NodeWrapper,
    cnxn: ConcreteHL.PortableAgentCnxn,
    responseId: String,
    accepted: Boolean): Unit = {

    sendBeginIntroductionResponse(node, cnxn, responseId, accepted, None, None)
  }

  def sendBeginIntroductionResponse(
    node: NodeWrapper,
    cnxn: ConcreteHL.PortableAgentCnxn,
    responseId: String,
    accepted: Boolean,
    aRejectReason: Option[String],
    bRejectReason: Option[String]): Unit = {

    val response = new BeginIntroductionResponse(responseId, Some(accepted), aRejectReason, bRejectReason)
    node.put(cnxn)(response)
  }

  def sendGetIntroductionProfileRequest(
    node: NodeWrapper,
    cnxn: ConcreteHL.PortableAgentCnxn,
    responseCnxn: ConcreteHL.PortableAgentCnxn): String = {

    val requestId = UUID.randomUUID().toString
    val request = new GetIntroductionProfileRequest(Some(requestId), Some(responseCnxn))

    node.put(cnxn)(request)

    requestId
  }

  def sendGetIntroductionProfileResponse(
    node: NodeWrapper,
    cnxn: ConcreteHL.PortableAgentCnxn,
    responseId: String): Unit = {

    val response = new GetIntroductionProfileResponse(responseId)
    node.put(cnxn)(response)
  }

  def sendIntroductionRequest(
    node: NodeWrapper,
    cnxn: ConcreteHL.PortableAgentCnxn,
    responseCnxn: ConcreteHL.PortableAgentCnxn,
    message: Option[String]): String = {

    val requestId = UUID.randomUUID().toString
    val request = new IntroductionRequest(Some(requestId), Some(responseCnxn), message)

    node.put(cnxn)(request)

    requestId
  }

  def sendIntroductionResponse(
    node: NodeWrapper,
    cnxn: ConcreteHL.PortableAgentCnxn,
    responseId: String,
    accepted: Boolean,
    rejectReason: Option[String]): String = {

    val connectId = UUID.randomUUID().toString
    val response = new IntroductionResponse(responseId, Some(accepted), rejectReason, Some(connectId))

    node.put(cnxn)(response)

    connectId
  }

  def sendConnect(
    node: NodeWrapper,
    cnxn: ConcreteHL.PortableAgentCnxn,
    connectId: String,
    writeCnxn: ConcreteHL.PortableAgentCnxn,
    readCnxn: ConcreteHL.PortableAgentCnxn): Unit = {

    val connect = new Connect(connectId, Some(writeCnxn), Some(readCnxn))
    node.put(cnxn)(connect)
  }
}
