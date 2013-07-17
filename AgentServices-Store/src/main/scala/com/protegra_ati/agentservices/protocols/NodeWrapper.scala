package com.protegra_ati.agentservices.protocols

import com.biosimilarity.evaluator.distribution.PortableAgentCnxn
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.biosimilarity.lift.lib.MonadicGenerators
import com.protegra_ati.agentservices.protocols.msgs.ProtocolMessage
import scala.util.continuations._

trait NodeWrapper {
  type Generators <: MonadicGenerators

  def get(cnxn : PortableAgentCnxn)(message: ProtocolMessage): Generators#Generator[Option[mTT.Resource], Unit, Unit]
  def subscribe(cnxn : PortableAgentCnxn)(message: ProtocolMessage): Generators#Generator[Option[mTT.Resource], Unit, Unit]
  def put(cnxn : PortableAgentCnxn)(message: ProtocolMessage): Unit
  def publish(cnxn : PortableAgentCnxn)(message: ProtocolMessage): Unit
}

class KVDBNodeWrapper(kvdbNode: Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse])
  extends NodeWrapper {

  override type Generators = Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse]

  override def get(cnxn: PortableAgentCnxn)(message: ProtocolMessage): Generators#Generator[Option[mTT.Resource], Unit, Unit] = {
    kvdbNode.get(toAgentCnxn(cnxn))(message.toCnxnCtxtLabel)
  }

  override def subscribe(cnxn: PortableAgentCnxn)(message: ProtocolMessage): Generators#Generator[Option[mTT.Resource], Unit, Unit] = {
    kvdbNode.subscribe(toAgentCnxn(cnxn))(message.toCnxnCtxtLabel)
  }

  override def put(cnxn: PortableAgentCnxn)(message: ProtocolMessage): Unit = {
    reset { kvdbNode.put(toAgentCnxn(cnxn))(message.toCnxnCtxtLabel, message.toGround) }
  }

  override def publish(cnxn: PortableAgentCnxn)(message: ProtocolMessage): Unit = {
    reset { kvdbNode.publish(toAgentCnxn(cnxn))(message.toCnxnCtxtLabel, message.toGround) }
  }

  private def toAgentCnxn(cnxn: PortableAgentCnxn): acT.AgentCnxn = {
    new acT.AgentCnxn(cnxn.src, cnxn.label, cnxn.trgt)
  }
}
