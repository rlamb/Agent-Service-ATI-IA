package com.protegra_ati.agentservices.protocols.msgs

case class BeginIntroductionResponse(
  override val responseId: String,
  accepted: Option[Boolean],
  aRejectReason: Option[String],
  bRejectReason: Option[String])

  extends ProtocolResponseMessage {

  def this(responseId: String) = this(responseId, None, None, None)
}
