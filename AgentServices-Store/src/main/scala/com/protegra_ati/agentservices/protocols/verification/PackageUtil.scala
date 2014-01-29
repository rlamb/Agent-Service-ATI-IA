package com.protegra_ati.agentservices.verificationProtocol

import com.biosimilarity.evaluator.distribution.{PortableAgentCnxn, PortableAgentBiCnxn}
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.biosimilarity.lift.model.store.CnxnCtxtLabel
import com.protegra_ati.agentservices.store.extensions.StringExtensions._
import java.net.URI

object PackageUtil {


  type KVDBNode = Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse]
  type Connection = PortableAgentCnxn
  type Identifier = String
  type Label = CnxnCtxtLabel[String, String, String]

  implicit def uriOfString(str: String): URI = new URI(str)

  implicit def agentCnxnOfPortableAgentCnxn(connection: PortableAgentCnxn): acT.AgentCnxn = {
    acT.AgentCnxn(connection.src, connection.label, connection.trgt)
  }

  object SimpleFilter {
    def apply[T](cls: Class[T]): String = {
      val name = cls.getSimpleName.toCamelCase
      s"protocolMessage($name(_))"
    }
  }

  object SimpleLabel {
    def apply(obj: Message): String = {
      val name = obj.getClass.getSimpleName.toCamelCase
      val hash = Math.abs(obj.hashCode())
      s"protocolMessage($name($hash))"
    }
  }



}
