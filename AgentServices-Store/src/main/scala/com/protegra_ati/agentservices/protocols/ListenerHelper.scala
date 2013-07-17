package com.protegra_ati.agentservices.protocols

import com.biosimilarity.evaluator.distribution.ConcreteHL
import com.biosimilarity.evaluator.distribution.PortableAgentCnxn
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.protegra_ati.agentservices.protocols.msgs._
import scala.concurrent._
import scala.reflect.ClassTag
import scala.util.continuations._
import ExecutionContext.Implicits.global

trait ListenerHelper {
  def listen[T <: ProtocolMessage: ClassTag](
    node: NodeWrapper,
    cnxn: PortableAgentCnxn,
    labelMessage: T): Future[T] = {

    future {
      var result: Option[T] = None

      reset {
        for (e <- node.get(cnxn)(labelMessage)) {
          e match {
            case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, message: T))) => {
              if (!message.isValid) {
                throw new Exception("invalid protocol message")
              }

              result = Some(message)
            }
            case _ => throw new Exception("unexpected protocol message")
          }
        }
      }

      if (result == None) {
        throw new Exception("no protocol message found")
      }

      result.get
    }
  }

  def listenJoin[T <: ProtocolMessage: ClassTag, U <: ProtocolMessage: ClassTag](
    node: NodeWrapper,
    cnxnA: PortableAgentCnxn,
    cnxnB: PortableAgentCnxn,
    labelMessageA: T,
    labelMessageB: U): Future[(T, U)] = {

    future {
      var result: Option[(T, U)] = None

      reset {
        for (e <- node.get(cnxnA)(labelMessageA); f <- node.get(cnxnB)(labelMessageB)) {
          (e, f) match {
            case (
              Some(mTT.Ground(ConcreteHL.InsertContent(_, _, messageA: T))),
              Some(mTT.Ground(ConcreteHL.InsertContent(_, _, messageB: U)))) => {

              if (!messageA.isValid || !messageB.isValid) {
                throw new Exception("invalid protocol message")
              }

              result = Some((messageA, messageB))
            }
            case _ => throw new Exception("unexpected protocol message")
          }
        }
      }

      if (result == None) {
        throw new Exception("no protocol message found")
      }

      result.get
    }
  }

  def listenSubscribe[T <: ProtocolMessage: ClassTag](
    node: NodeWrapper,
    cnxn: PortableAgentCnxn,
    labelMessage: T,
    handler: Either[Throwable, T] => Unit): Unit = {

    future {
      reset {
        for (e <- node.get(cnxn)(labelMessage)) {
          e match {
            case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, message: T))) => {
              if (!message.isValid) {
                handler(Left(new Exception("invalid protocol message")))
              } else {
                handler(Right(message))
              }
            }
            case _ => {
              handler(Left(new Exception("unexpected protocol message")))
            }
          }
        }
      }
    }
  }
}
