package com.protegra_ati.agentservices.protocols.msgs

import com.biosimilarity.evaluator.distribution.PortableAgentCnxn

case class BeginIntroductionRequest(
  override val requestId: Option[String],
  override val responseCnxn: Option[PortableAgentCnxn],
  aRequestCnxn: Option[PortableAgentCnxn],
  aResponseCnxn: Option[PortableAgentCnxn],
  bRequestCnxn: Option[PortableAgentCnxn],
  bResponseCnxn: Option[PortableAgentCnxn],
  aMessage: Option[String],
  bMessage: Option[String])

  extends ProtocolRequestMessage {

  def this() = this(None, None, None, None, None, None, None, None)

  override def isValid: Boolean = {
    requestId != None &&
      responseCnxn != None &&
      aRequestCnxn != None &&
      aResponseCnxn != None &&
      bRequestCnxn != None &&
      bResponseCnxn != None
  }
}
