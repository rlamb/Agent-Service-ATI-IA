package com.protegra_ati.agentservices.protocols

import com.biosimilarity.evaluator.distribution.PortableAgentCnxn
import com.protegra_ati.agentservices.protocols.msgs._
import java.util.UUID
import scala.concurrent._
import ExecutionContext.Implicits.global

trait SenderHelper {
  def sendBeginIntroductionResponse(
    node: NodeWrapper,
    cnxn: PortableAgentCnxn,
    responseId: String,
    accepted: Boolean,
    aRejectReason: Option[String] = None,
    bRejectReason: Option[String] = None): Unit = {

    val response = new BeginIntroductionResponse(responseId, Some(accepted), aRejectReason, bRejectReason)

    future {
      node.put(cnxn)(response)
    }
  }

  def sendGetIntroductionProfileRequest(
    node: NodeWrapper,
    cnxn: PortableAgentCnxn,
    responseCnxn: PortableAgentCnxn): String = {

    val requestId = UUID.randomUUID().toString
    val request = new GetIntroductionProfileRequest(Some(requestId), Some(responseCnxn))

    future {
      node.publish(cnxn)(request)
    }

    requestId
  }

  def sendGetIntroductionProfileResponse(
    node: NodeWrapper,
    cnxn: PortableAgentCnxn,
    responseId: String): Unit = {

    val response = new GetIntroductionProfileResponse(responseId)

    future {
      node.put(cnxn)(response)
    }
  }

  def sendIntroductionRequest(
    node: NodeWrapper,
    cnxn: PortableAgentCnxn,
    responseCnxn: PortableAgentCnxn,
    message: Option[String]): String = {

    val requestId = UUID.randomUUID().toString
    val request = new IntroductionRequest(Some(requestId), Some(responseCnxn), message)

    future {
      node.publish(cnxn)(request)
    }

    requestId
  }

  def sendIntroductionResponse(
    node: NodeWrapper,
    cnxn: PortableAgentCnxn,
    responseId: String,
    accepted: Boolean,
    aliasName: Option[String],
    rejectReason: Option[String]): String = {

    val connectId = UUID.randomUUID().toString
    val response = new IntroductionResponse(responseId, Some(accepted), aliasName, rejectReason, Some(connectId))

    future {
      node.put(cnxn)(response)
    }

    connectId
  }

  def sendConnect(
    node: NodeWrapper,
    cnxn: PortableAgentCnxn,
    connectId: String,
    aliasName: Option[String],
    writeCnxn: PortableAgentCnxn,
    readCnxn: PortableAgentCnxn): Unit = {

    val connect = new Connect(connectId, aliasName, Some(writeCnxn), Some(readCnxn))

    future {
      node.put(cnxn)(connect)
    }
  }
}
