package com.protegra_ati.agentservices.protocols.msgs

import com.biosimilarity.evaluator.distribution.ConcreteHL

case class Connect(
  connectId: String,
  writeCnxn: Option[ConcreteHL.PortableAgentCnxn],
  readCnxn: Option[ConcreteHL.PortableAgentCnxn])

  extends ProtocolMessage {

  def this(connectId: String) = this(connectId, None, None)
  override val innerLabel = "connectId(\"" + connectId + "\")"

  override def isValid: Boolean = {
    writeCnxn != None &&
      readCnxn != None
  }
}
