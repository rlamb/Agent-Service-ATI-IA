package com.protegra_ati.agentservices.protocols.verificationProtocol

import com.biosimilarity.evaluator.distribution.{PortableAgentCnxn, PortableAgentBiCnxn}
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.biosimilarity.lift.model.store.CnxnCtxtLabel
import com.protegra_ati.agentservices.store.extensions.StringExtensions._

object PackageUtil {


  type KVDBNode = Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse]
  type Connection = PortableAgentCnxn
  type Identifier = String
  type Label = CnxnCtxtLabel[String, String, String]

  implicit def agentCnxnOfPortableAgentCnxn(connection: PortableAgentCnxn): acT.AgentCnxn = {
    acT.AgentCnxn(connection.src, connection.label, connection.trgt)
  }



}
