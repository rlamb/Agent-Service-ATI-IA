package com.protegra_ati.agentservices.protocols

import com.biosimilarity.evaluator.distribution.ConcreteHL
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.protegra_ati.agentservices.protocols.msgs._
import java.net.URI
import scala.concurrent._
import scala.util.Failure
import scala.util.Success
import ExecutionContext.Implicits.global

trait BaseProtocols2 extends ListenerHelper with SenderHelper {

  def genericIntroducer(kvdbNode: Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse], cnxn: ConcreteHL.PortableAgentCnxn): Unit = {
    genericIntroducer(new KVDBNodeWrapper(kvdbNode), cnxn)
  }

  def genericIntroducer(node: NodeWrapper, cnxn: ConcreteHL.PortableAgentCnxn): Unit = {
    // listen for BeginIntroductionRequest message
    listenSubscribe(
      node,
      cnxn,
      new BeginIntroductionRequest(),
      handleBeginIntroductionRequest(node, _: Either[Throwable, BeginIntroductionRequest]))
  }

  def genericIntroduced(
    kvdbNode: Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse],
    cnxn: ConcreteHL.PortableAgentCnxn,
    privateRqCnxn: ConcreteHL.PortableAgentCnxn,
    privateRspCnxn: ConcreteHL.PortableAgentCnxn) {

    genericIntroduced(new KVDBNodeWrapper(kvdbNode), cnxn, privateRqCnxn, privateRspCnxn)
  }

  def genericIntroduced(
    node: NodeWrapper,
    cnxn: ConcreteHL.PortableAgentCnxn,
    privateRqCnxn: ConcreteHL.PortableAgentCnxn,
    privateRspCnxn: ConcreteHL.PortableAgentCnxn) {

    // listen for GetIntroductionProfileRequest message
    listenSubscribe(
      node,
      cnxn,
      new GetIntroductionProfileRequest(),
      handleGetIntroductionProfileRequest(node, _: Either[Throwable, GetIntroductionProfileRequest]))

    // listen for IntroductionRequest message
    listenSubscribe(
      node,
      cnxn,
      new IntroductionRequest(),
      handleIntroductionRequest(node, cnxn, privateRqCnxn, privateRspCnxn, _: Either[Throwable, IntroductionRequest]))
  }

  def handleBeginIntroductionRequest(node: NodeWrapper, result: Either[Throwable, BeginIntroductionRequest]): Unit = {
    result match {
      case Right(biRq) => {
        // send GetIntroductionProfileRequest messages
        val aGetIntroProfileRqId = sendGetIntroductionProfileRequest(node, biRq.aRequestCnxn.get, biRq.aResponseCnxn.get)
        val bGetIntroProfileRqId = sendGetIntroductionProfileRequest(node, biRq.bRequestCnxn.get, biRq.bResponseCnxn.get)

        // listen for GetIntroductionProfileResponse messages
        listenJoin(
          node,
          biRq.aResponseCnxn.get,
          biRq.bResponseCnxn.get,
          new GetIntroductionProfileResponse(aGetIntroProfileRqId),
          new GetIntroductionProfileResponse(bGetIntroProfileRqId)) onComplete {

          case Success((agipRsp, bgipRsp)) => {
            // send IntroductionRequest messages
            val aIntroRqId = sendIntroductionRequest(node, biRq.aRequestCnxn.get, biRq.aResponseCnxn.get, biRq.aMessage)
            val bIntroRqId = sendIntroductionRequest(node, biRq.bRequestCnxn.get, biRq.bResponseCnxn.get, biRq.bMessage)

            // listen for IntroductionResponse messages
            listenJoin(
              node,
              biRq.aResponseCnxn.get,
              biRq.bResponseCnxn.get,
              new IntroductionResponse(aIntroRqId),
              new IntroductionResponse(bIntroRqId)) onComplete {

              case Success((aiRsp, biRsp)) => {
                // check whether A and B accepted
                val accepted = aiRsp.accepted.get && biRsp.accepted.get

                if (accepted) {
                  // create new cnxns
                  // TODO: Create new cnxns properly
                  val abCnxn = new ConcreteHL.PortableAgentCnxn(new URI("agent://a"), "", new URI("agent://b"))
                  val baCnxn = new ConcreteHL.PortableAgentCnxn(new URI("agent://b"), "", new URI("agent://a"))

                  // send Connect messages
                  sendConnect(node, biRq.aRequestCnxn.get, aiRsp.connectId.get, abCnxn, baCnxn)
                  sendConnect(node, biRq.bRequestCnxn.get, biRsp.connectId.get, baCnxn, abCnxn)

                  // send BeginIntroductionResponse message
                  sendBeginIntroductionResponse(node, biRq.responseCnxn.get, biRq.requestId.get, accepted)
                } else {
                  // send BeginIntroductionResponse message
                  sendBeginIntroductionResponse(
                    node,
                    biRq.responseCnxn.get,
                    biRq.requestId.get,
                    accepted,
                    aiRsp.rejectReason,
                    biRsp.rejectReason)
                }
              }
              case Failure(t) => throw t
            }
          }
          case Failure(t) => throw t
        }
      }
      case Left(t) => throw t
    }
  }

  def handleGetIntroductionProfileRequest(node: NodeWrapper, result: Either[Throwable, GetIntroductionProfileRequest]): Unit = {
    result match {
      case Right(gipRq) => {
        // TODO: Load introduction profile

        // send GetIntroductionProfileResponse message
        // TODO: Send introduction profile details
        sendGetIntroductionProfileResponse(node, gipRq.responseCnxn.get, gipRq.requestId.get)
      }
      case Left(t) => throw t
    }
  }

  def handleIntroductionRequest(
    node: NodeWrapper,
    cnxn: ConcreteHL.PortableAgentCnxn,
    privateRqCnxn: ConcreteHL.PortableAgentCnxn,
    privateRspCnxn: ConcreteHL.PortableAgentCnxn,
    result: Either[Throwable, IntroductionRequest]): Unit = {

    result match {
      case Right(iRq) => {
        // send IntroductionRequest message
        // TODO: Send introduction profile to message
        val uiIntroRqId = sendIntroductionRequest(node, privateRqCnxn, privateRspCnxn, iRq.message)

        // listen for IntroductionResponse message
        listen(node, privateRspCnxn, new IntroductionResponse(uiIntroRqId)) onComplete {
          case Success(iRsp) => {
            // send IntroductionResponse message
            val connectId = sendIntroductionResponse(
              node,
              iRq.responseCnxn.get,
              iRq.requestId.get,
              iRsp.accepted.get,
              iRsp.rejectReason)

            if (iRsp.accepted.get) {
              // listen for Connect message
              listen(node, cnxn, new Connect(connectId)) onComplete {
                case Success(c) => {
                  // TODO: Store the new cnxns
                }
                case Failure(t) => throw t
              }
            }
          }
          case Failure(t) => throw t
        }
      }
      case Left(t) => throw t
    }
  }
}
