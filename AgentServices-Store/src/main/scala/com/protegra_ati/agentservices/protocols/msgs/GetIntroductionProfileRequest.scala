package com.protegra_ati.agentservices.protocols.msgs

import com.biosimilarity.evaluator.distribution.ConcreteHL

case class GetIntroductionProfileRequest(
  override val requestId: Option[String],
  override val responseCnxn: Option[ConcreteHL.PortableAgentCnxn])

  extends ProtocolRequestMessage {

  def this() = this(None, None)

  override def isValid: Boolean = {
    requestId != None &&
      responseCnxn != None
  }
}
