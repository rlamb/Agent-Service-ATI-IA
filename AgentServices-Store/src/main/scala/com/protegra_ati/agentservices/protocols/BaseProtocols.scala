package com.protegra_ati.agentservices.protocols

import com.biosimilarity.evaluator.distribution.ConcreteHL
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.protegra_ati.agentservices.protocols.msgs._
import java.net.URI
import java.util.UUID
import scala.util.continuations._

trait BaseProtocols {
  def genericIntroducer(
    kvdbNode: Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse],
    cnxn: ConcreteHL.PortableAgentCnxn): Unit = {

    genericIntroducer(new KVDBNodeWrapper(kvdbNode), cnxn)
  }

  def genericIntroducer(node: NodeWrapper, cnxn: ConcreteHL.PortableAgentCnxn): Unit = {
    reset {
      // listen for BeginIntroductionRequest message
      for (e <- node.subscribe(cnxn)(new BeginIntroductionRequest())) {
        e match {
          case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, biRq: BeginIntroductionRequest))) => {
            // TODO: Validate biRq

            // create GetIntroductionProfileRequest messages
            val aGetIntroProfileRq = new GetIntroductionProfileRequest(Some(UUID.randomUUID.toString), biRq.aResponseCnxn)
            val bGetIntroProfileRq = new GetIntroductionProfileRequest(Some(UUID.randomUUID.toString), biRq.bResponseCnxn)

            // send GetIntroductionProfileRequest messages
            node.put(biRq.aRequestCnxn.get)(aGetIntroProfileRq)
            node.put(biRq.bRequestCnxn.get)(bGetIntroProfileRq)

            reset {
              // listen for GetIntroductionProfileResponse messages
              // TODO: TTL
              for (
                f <- node.get(biRq.aResponseCnxn.get)(new GetIntroductionProfileResponse(aGetIntroProfileRq.requestId.get));
                g <- node.get(biRq.bResponseCnxn.get)(new GetIntroductionProfileResponse(bGetIntroProfileRq.requestId.get))) {

                (f, g) match {
                  case (Some(mTT.Ground(ConcreteHL.InsertContent(_, _, agipRsp: GetIntroductionProfileResponse))),
                        Some(mTT.Ground(ConcreteHL.InsertContent(_, _, bgipRsp: GetIntroductionProfileResponse)))) => {

                    // TODO: Validate agipRsp
                    // TODO: Validate bgipRsp

                    // create IntroductionRequest messages
                    // TODO: Add introduction profiles to messages
                    val aIntroRq = new IntroductionRequest(Some(UUID.randomUUID.toString), biRq.aResponseCnxn, biRq.aMessage)
                    val bIntroRq = new IntroductionRequest(Some(UUID.randomUUID.toString), biRq.bResponseCnxn, biRq.bMessage)

                    // send IntroductionRequest messages
                    node.put(biRq.aRequestCnxn.get)(aIntroRq)
                    node.put(biRq.bRequestCnxn.get)(bIntroRq)

                    reset {
                      // listen for IntroductionResponse messages
                      // TODO: TTL
                      // TODO: Find a way to stop if either party rejects
                      for (
                        h <- node.get(biRq.aResponseCnxn.get)(new IntroductionResponse(aIntroRq.requestId.get));
                        i <- node.get(biRq.bResponseCnxn.get)(new IntroductionResponse(bIntroRq.requestId.get))) {

                        (h, i) match {
                          case (Some(mTT.Ground(ConcreteHL.InsertContent(_, _, aiRsp: IntroductionResponse))),
                                Some(mTT.Ground(ConcreteHL.InsertContent(_, _, biRsp: IntroductionResponse)))) => {

                            // TODO: Validate aiRsp
                            // TODO: Validate biRsp

                            // create BeginIntroductionResponse message
                            val beginIntroRsp = new BeginIntroductionResponse(
                              biRq.requestId.get,
                              Some(aiRsp.accepted.get && biRsp.accepted.get),
                              aiRsp.rejectReason,
                              biRsp.rejectReason)

                            // check whether A and B accepted
                            if (aiRsp.accepted.get && biRsp.accepted.get) {
                              // create new cnxns
                              // TODO: Create new cnxns properly
                              val abCnxn = new ConcreteHL.PortableAgentCnxn(new URI("agent://a"), "", new URI("agent://b"))
                              val baCnxn = new ConcreteHL.PortableAgentCnxn(new URI("agent://b"), "", new URI("agent://a"))

                              // create Connect messages
                              val aConnect = new Connect(aiRsp.connectId.get, Some(abCnxn), Some(baCnxn))
                              val bConnect = new Connect(biRsp.connectId.get, Some(baCnxn), Some(abCnxn))

                              // send Connect messages
                              node.put(biRq.aRequestCnxn.get)(aConnect)
                              node.put(biRq.aRequestCnxn.get)(bConnect)

                              // send BeginIntroductionResponse message
                              node.put(biRq.responseCnxn.get)(beginIntroRsp)
                            } else {
                              // send BeginIntroductionResponse message
                              node.put(biRq.responseCnxn.get)(beginIntroRsp)
                            }
                          }
                          case _ => {
                            // expected IntroductionResponse
                            throw new Exception("unexpected protocol message")
                          }
//                        }
                        }
                      }
                    }
                  }
                  case _ => {
                    // expected GetIntroductionProfileResponse (x2)
                    throw new Exception("unexpected protocol message")
                  }
                }
              }
            }
          }
          case _ => {
            // expected BeginIntroductionRequest
            throw new Exception("unexpected protocol message")
          }
        }
      }
    }
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

    reset {
      // listen for GetIntroductionProfileRequest message
      for (e <- node.subscribe(cnxn)(new GetIntroductionProfileRequest())) {

        e match {
          case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, gipRq: GetIntroductionProfileRequest))) => {
            // TODO: Validate gipRq
            // TODO: Load introduction profile

            // create GetIntroductionProfileResponse message
            // TODO: Set introduction profile on message
            val getIntroProfileRsp = new GetIntroductionProfileResponse(gipRq.requestId.get)

            // send GetIntroductionProfileResponse message
            node.put(gipRq.responseCnxn.get)(getIntroProfileRsp)
          }
          case _ => {
            // expected GetIntroductionProfileRequest
            throw new Exception("unexpected protocol message")
          }
        }
      }
    }

    reset {
      // listen for IntroductionRequest message
      for (e <- node.subscribe(cnxn)(new IntroductionRequest())) {
        e match {
          case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, iRq: IntroductionRequest))) => {
            // TODO: Validate iRq

            // create IntroductionRequest message
            // TODO: Add introduction profile to message
            val introRq = new IntroductionRequest(Some(UUID.randomUUID.toString), Some(privateRspCnxn), iRq.message)

            // send IntroductionRequest message
            node.put(privateRqCnxn)(introRq)

            reset {
              // listen for IntroductionResponse message
              // TODO: TTL
              for (f <- node.get(privateRspCnxn)(new IntroductionResponse(introRq.requestId.get))) {
                f match {
                  case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, iRsp: IntroductionResponse))) => {
                    // TODO: Validate iRsp

                    // create IntroductionResponse message
                    val introRsp = new IntroductionResponse(
                      iRq.requestId.get,
                      iRsp.accepted,
                      iRsp.rejectReason,
                      Some(UUID.randomUUID.toString))

                    // send IntroductionResponse message
                    node.put(iRq.responseCnxn.get)(introRsp)

                    if (iRsp.accepted.get) {
                      reset {
                        // listen for Connect message
                        // TODO: TTL
                        for (g <- node.get(cnxn)(new Connect(introRsp.connectId.get))) {
                          g match {
                            case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, c: Connect))) => {
                              // TODO: Validate c
                              // TODO: Store the new cnxns
                            }
                            case _ => {
                              // expected Connect
                              throw new Exception("unexpected protocol message")
                            }
                          }
                        }
                      }
                    }
                  }
                  case _ => {
                    // expected IntroductionResponse
                    throw new Exception("unexpected protocol message")
                  }
                }
              }
            }
          }
          case _ => {
            // expected IntroductionRequest
            throw new Exception("unexpected protocol message")
          }
        }
      }
    }
  }
}
