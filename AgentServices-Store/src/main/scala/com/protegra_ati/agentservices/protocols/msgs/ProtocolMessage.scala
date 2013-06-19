package com.protegra_ati.agentservices.protocols.msgs

import com.biosimilarity.evaluator.distribution.ConcreteHL
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.biosimilarity.lift.model.store.CnxnCtxtLabel
import com.protegra_ati.agentservices.store.extensions.StringExtensions._

trait ProtocolMessage {
  val innerLabel = "_"
  private lazy val label = ("protocolMessage(" + getClass.getName.trimPackage.toCamelCase + "(" + innerLabel + "))").toLabel

  def toCnxnCtxtLabel: CnxnCtxtLabel[String, String, String] = {
    label
  }

  def toGround: mTT.Ground = {
    mTT.Ground(ConcreteHL.InsertContent(label, Nil, this))
  }

  def isValid: Boolean
}
