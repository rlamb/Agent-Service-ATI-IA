package com.protegra_ati.agentservices.protocols.msgs

trait ProtocolResponseMessage extends ProtocolMessage {
  val responseId: String
  override val innerLabel = "sessionId(" + generateLabelValue(sessionId) + "),responseId(\"" + responseId + "\")"
}