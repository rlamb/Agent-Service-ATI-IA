package com.protegra_ati.agentservices.protocols.msgs

import com.biosimilarity.evaluator.distribution.ConcreteHL

trait ProtocolRequestMessage extends ProtocolMessage {
  val requestId: Option[String]
  val responseCnxn: Option[ConcreteHL.PortableAgentCnxn]
}
