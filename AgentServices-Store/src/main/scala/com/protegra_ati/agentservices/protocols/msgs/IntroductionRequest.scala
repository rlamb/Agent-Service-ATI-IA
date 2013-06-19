package com.protegra_ati.agentservices.protocols.msgs

import com.biosimilarity.evaluator.distribution.ConcreteHL

case class IntroductionRequest(
  override val requestId: Option[String],
  override val responseCnxn: Option[ConcreteHL.PortableAgentCnxn],
  message: Option[String])

  extends ProtocolRequestMessage {

  def this() = this(None, None, None)

  override def isValid: Boolean = {
    requestId != None &&
      responseCnxn != None
  }
}
