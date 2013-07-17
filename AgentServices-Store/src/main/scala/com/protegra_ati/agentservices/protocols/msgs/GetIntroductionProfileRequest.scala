package com.protegra_ati.agentservices.protocols.msgs

import com.biosimilarity.evaluator.distribution.PortableAgentCnxn

case class GetIntroductionProfileRequest(
  override val requestId: Option[String],
  override val responseCnxn: Option[PortableAgentCnxn])

  extends ProtocolRequestMessage {

  def this() = this(None, None)

  override def isValid: Boolean = {
    requestId != None &&
      responseCnxn != None
  }
}
