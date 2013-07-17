package com.protegra_ati.agentservices.protocols.msgs

import com.biosimilarity.evaluator.distribution.PortableAgentCnxn

case class Connect(
  connectId: String,
  aliasName: Option[String],
  writeCnxn: Option[PortableAgentCnxn],
  readCnxn: Option[PortableAgentCnxn])

  extends ProtocolMessage {

  def this(connectId: String) = this(connectId, None, None, None)
  override val innerLabel = "connectId(\"" + connectId + "\")"

  override def isValid: Boolean = {
    writeCnxn != None &&
      readCnxn != None
  }
}
