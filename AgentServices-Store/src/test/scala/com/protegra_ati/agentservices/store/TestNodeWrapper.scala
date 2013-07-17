package com.protegra_ati.agentservices.store

import com.biosimilarity.evaluator.distribution.PortableAgentCnxn
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.protegra_ati.agentservices.protocols.NodeWrapper
import com.protegra_ati.agentservices.protocols.msgs.ProtocolMessage

class TestNodeWrapper(testNode: AgentTestNode) extends NodeWrapper {
  override type Generators = AgentTestNode

  override def get(cnxn: PortableAgentCnxn)(message: ProtocolMessage): Generators#Generator[Option[mTT.Resource], Unit, Unit] = {
    testNode.get(cnxn)(message.toCnxnCtxtLabel)
  }

  override def subscribe(cnxn: PortableAgentCnxn)(message: ProtocolMessage): Generators#Generator[Option[mTT.Resource], Unit, Unit] = {
    testNode.subscribe(cnxn)(message.toCnxnCtxtLabel)
  }

  override def put(cnxn: PortableAgentCnxn)(message: ProtocolMessage): Unit = {
    testNode.put(cnxn)(message.toCnxnCtxtLabel, message.toGround)
  }

  override def publish(cnxn: PortableAgentCnxn)(message: ProtocolMessage): Unit = {
    testNode.publish(cnxn)(message.toCnxnCtxtLabel, message.toGround)
  }
}
