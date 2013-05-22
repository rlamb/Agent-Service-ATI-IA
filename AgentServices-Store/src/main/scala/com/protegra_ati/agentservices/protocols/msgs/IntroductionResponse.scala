package com.protegra_ati.agentservices.protocols.msgs

case class IntroductionResponse(
  override val responseId: String,
  accepted: Option[Boolean],
  rejectReason: Option[String],
  connectId: Option[String])

  extends ProtocolResponseMessage {

  def this(responseId: String) = this(responseId, None, None, None)
}
