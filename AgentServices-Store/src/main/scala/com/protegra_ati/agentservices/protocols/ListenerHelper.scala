package com.protegra_ati.agentservices.protocols

import com.biosimilarity.evaluator.distribution.ConcreteHL
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.protegra_ati.agentservices.protocols.msgs._
import scala.concurrent._
import scala.util.continuations._

trait ListenerHelper {
  def listenBeginIntroductionRequest(
    node: NodeWrapper,
    cnxn: ConcreteHL.PortableAgentCnxn): Future[BeginIntroductionRequest] = {

    val p = promise[BeginIntroductionRequest]()

    reset {
      for (e <- node.get(cnxn)(new BeginIntroductionRequest())) {
        e match {
          case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, message: BeginIntroductionRequest))) => {
            // TODO: Validate message
            p.success(message)
          }
          case _ => p.failure(new Exception("unexpected protocol message"))
        }
        () // Used to return unit in continuation
      }
    }

    p.future
  }

  def listenGetIntroductionProfileRequest(
    node: NodeWrapper,
    cnxn: ConcreteHL.PortableAgentCnxn): Future[GetIntroductionProfileRequest] = {

    val p = promise[GetIntroductionProfileRequest]()

    reset {
      for (e <- node.get(cnxn)(new GetIntroductionProfileRequest())) {
        e match {
          case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, message: GetIntroductionProfileRequest))) => {
            // TODO: Validate message
            p.success(message)
          }
          case _ => p.failure(new Exception("unexpected protocol message"))
        }
        () // Used to return unit in continuation
      }
    }

    p.future
  }

  def listenGetIntroductionProfileResponses(
    node: NodeWrapper,
    aCnxn: ConcreteHL.PortableAgentCnxn,
    bCnxn: ConcreteHL.PortableAgentCnxn,
    aRequestId: String,
    bRequestId: String): Future[(GetIntroductionProfileResponse, GetIntroductionProfileResponse)] = {

    val p = promise[(GetIntroductionProfileResponse, GetIntroductionProfileResponse)]()

    reset {
      for (
        e <- node.get(aCnxn)(new GetIntroductionProfileResponse(aRequestId));
        f <- node.get(bCnxn)(new GetIntroductionProfileResponse(bRequestId))) {

        (e, f) match {
          case (
            Some(mTT.Ground(ConcreteHL.InsertContent(_, _, aMessage: GetIntroductionProfileResponse))),
            Some(mTT.Ground(ConcreteHL.InsertContent(_, _, bMessage: GetIntroductionProfileResponse)))) => {

            // TODO: Validate message
            p.success((aMessage, bMessage))
          }
          case _ => p.failure(new Exception("unexpected protocol message"))
        }
        () // Used to return unit in continuation
      }
    }

    p.future
  }

  def listenIntroductionRequest(
    node: NodeWrapper,
    cnxn: ConcreteHL.PortableAgentCnxn): Future[IntroductionRequest] = {

    val p = promise[IntroductionRequest]()

    reset {
      for (e <- node.get(cnxn)(new IntroductionRequest())) {
        e match {
          case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, message: IntroductionRequest))) => {
            // TODO: Validate message
            p.success(message)
          }
          case _ => p.failure(new Exception("unexpected protocol message"))
        }
        () // Used to return unit in continuation
      }
    }

    p.future
  }

  def listenIntroductionResponses(
    node: NodeWrapper,
    aCnxn: ConcreteHL.PortableAgentCnxn,
    bCnxn: ConcreteHL.PortableAgentCnxn,
    aRequestId: String,
    bRequestId: String): Future[(IntroductionResponse, IntroductionResponse)] = {

    val p = promise[(IntroductionResponse, IntroductionResponse)]()

    reset {
      for (
        e <- node.get(aCnxn)(new IntroductionResponse(aRequestId));
        f <- node.get(bCnxn)(new IntroductionResponse(bRequestId))) {

        (e, f) match {
          case (
            Some(mTT.Ground(ConcreteHL.InsertContent(_, _, aMessage: IntroductionResponse))),
            Some(mTT.Ground(ConcreteHL.InsertContent(_, _, bMessage: IntroductionResponse)))) => {

            // TODO: Validate message
            p.success((aMessage, bMessage))
          }
          case _ => p.failure(new Exception("unexpected protocol message"))
        }
        () // Used to return unit in continuation
      }
    }

    p.future
  }

  def listenIntroductionResponse(
    node: NodeWrapper,
    cnxn: ConcreteHL.PortableAgentCnxn,
    requestId: String): Future[IntroductionResponse] = {

    val p = promise[IntroductionResponse]()

    reset {
      for (e <- node.get(cnxn)(new IntroductionResponse(requestId))) {
        e match {
          case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, message: IntroductionResponse))) => {
            // TODO: Validate message
            p.success(message)
          }
          case _ => p.failure(new Exception("unexpected protocol message"))
        }
        () // Used to return unit in continuation
      }
    }

    p.future
  }

  def listenConnect(node: NodeWrapper, cnxn: ConcreteHL.PortableAgentCnxn, connectId: String): Future[Connect] = {
    val p = promise[Connect]()

    reset {
      for (e <- node.get(cnxn)(new Connect(connectId))) {
        e match {
          case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, message: Connect))) => {
            // TODO: Validate message
            p.success(message)
          }
          case _ => p.failure(new Exception("unexpected protocol message"))
        }
        () // Used to return unit in continuation
      }
    }

    p.future
  }
}
