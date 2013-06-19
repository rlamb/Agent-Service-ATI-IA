package com.protegra_ati.agentservices.protocols.msgs

case class IntroductionResponse(
  override val responseId: String,
  accepted: Option[Boolean],
  rejectReason: Option[String],
  connectId: Option[String])

  extends ProtocolResponseMessage {

  def this(responseId: String) = this(responseId, None, None, None)
  def this(responseId: String, accepted: Option[Boolean]) = this(responseId, accepted, None, None)
  def this(responseId: String, accepted: Option[Boolean], rejectReason: Option[String]) = this(responseId, accepted, rejectReason, None)

  override def isValid: Boolean = {
    accepted != None
  }
}
