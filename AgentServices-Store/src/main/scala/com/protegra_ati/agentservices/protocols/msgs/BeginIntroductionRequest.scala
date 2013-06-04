package com.protegra_ati.agentservices.protocols.msgs

import com.biosimilarity.evaluator.distribution.ConcreteHL

case class BeginIntroductionRequest(
  override val requestId: Option[String],
  override val responseCnxn: Option[ConcreteHL.PortableAgentCnxn],
  aRequestCnxn: Option[ConcreteHL.PortableAgentCnxn],
  aResponseCnxn: Option[ConcreteHL.PortableAgentCnxn],
  bRequestCnxn: Option[ConcreteHL.PortableAgentCnxn],
  bResponseCnxn: Option[ConcreteHL.PortableAgentCnxn],
  aMessage: Option[String],
  bMessage: Option[String])

  extends ProtocolRequestMessage {

  def this() = this(None, None, None, None, None, None, None, None)
}
