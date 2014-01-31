package com.protegra_ati.agentservices.protocols.verification

// Import Objects
import scala.util.continuations._
import com.protegra_ati.agentservices.store.extensions.StringExtensions._
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.protegra_ati.agentservices.store.util.Sugar
import Sugar._
import com.biosimilarity.lift.lib.BasicLogService

// Import Classes & Packages
import scala.reflect.Manifest
import com.biosimilarity.evaluator.distribution.ConcreteHL.PostedExpr
import com.biosimilarity.evaluator.distribution.PortableAgentCnxn

// import com.protegra_ati.agentservices.verificationProtocol.FilteredConnection._
object FilteredConnection {

  case class FilteredConnection(val connection: PortableAgentCnxn, val filter: Label) {

    def !(message: Message)(implicit kvdb: KVDBNode) {reset{
      kvdb.put(connection)(filter, mTT.Ground(PostedExpr(message)))
    }}

    def ?[T <: Message](continueWith: T => Unit)(implicit kvdb: KVDBNode) {reset{
      for(message <- kvdb.get(connection)(filter)) {
        message match {
          case Some(mTT.RBoundHM(Some(mTT.Ground(PostedExpr(message: T))), _)) =>
            continueWith(message)
          case Some(mTT.Ground(PostedExpr(message: T))) =>
            continueWith(message)
          case None => ;
          case somethingElse => Tweet("FilteredConnection.? got %s".format(somethingElse.toString))
        }
      }
    }}

  }

  implicit class PortableAgentConnection(cnxn: PortableAgentCnxn)
    extends PortableAgentCnxn(cnxn.src, cnxn.label, cnxn.trgt) {

    def /(filter: String): FilteredConnection = FilteredConnection(this, filter.toLabel)

    // Syntactic sugar for sending/receiving messages with their implicit filters.
    def !(msg: Message)(implicit kvdb: KVDBNode) { (this / msg.filter) ! msg }
    def ?[T <: Message](continue: T => Unit)(implicit m: Manifest[T],  kvdb: KVDBNode) {
      (this / SimpleFilter(m.runtimeClass)) ? continue }

  }

}