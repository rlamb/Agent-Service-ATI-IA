package com.protegra_ati.agentservices.protocols.msgs

import com.biosimilarity.evaluator.distribution.PortableAgentCnxn

trait ProtocolRequestMessage extends ProtocolMessage {
  val requestId: Option[String]
  val responseCnxn: Option[PortableAgentCnxn]
}
