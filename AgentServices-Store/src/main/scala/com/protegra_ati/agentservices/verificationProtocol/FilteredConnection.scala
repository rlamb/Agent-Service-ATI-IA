package com.protegra_ati.agentservices.verificationProtocol

// Import Objects
import scala.util.continuations._
import com.protegra_ati.agentservices.store.extensions.StringExtensions._
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.protegra_ati.agentservices.protocols.verificationProtocol.PackageUtil._

// Import Classes & Packages
import scala.reflect.Manifest
import com.protegra_ati.agentservices.protocols.verificationProtocol.Message
import com.biosimilarity.evaluator.distribution.ConcreteHL.PostedExpr
import com.biosimilarity.evaluator.distribution.PortableAgentCnxn

// import com.protegra_ati.agentservices.verificationProtocol.FilteredConnection._
object FilteredConnection {

  implicit class PortableAgentConnection(cnxn: PortableAgentCnxn)(implicit val kvdb: KVDBNode)
    extends PortableAgentCnxn(cnxn.src, cnxn.label, cnxn.trgt) {

    /**
     * The FilteredConnection captures (as a thunk) the relationship between
     * PortableAgentConnection object, KVDBNode object, Pattern object, and
     * Message object.
     *
     * This, by clever use of implicits, contributes to a display of the Pi-
     * Calculus transformation in Behaviors.scala, such that the type system
     * of the display matches that of our dictated protocol (see Behaviors:15)
     * @param pattern: datalog - shape functor, filters Connection
     */
      case class FilteredConnection(val pattern: Label) {

        // Note that (kvdb: KVDBNode) is implicit in this context.
        //   asset: kvdb: KVDBNode - virtual KVDB Node.

      /**
       *  The display language in Behaviors.scala calls for infix operators
       *  (!) and (?).  This results in a mongodb query.
       * @param message
       */
        def !(message: Message) {reset{
          kvdb.put(pattern, mTT.Ground(PostedExpr(message)))
        }}

        def ?[T <: Message](continueWith: T => Unit) {reset{
          for(message <- kvdb.get(acT.AgentCnxn(src, label, trgt))(pattern)) {
            message match {
              case Some(mTT.RBoundHM(Some(mTT.Ground(PostedExpr(message: T))), _)) =>
                continueWith(message)
              case _ => ;
            }
          }
        }}

      }

    // Created a filtered connection by slicing with a pattern.
    // For most literate use, declare current node to be implicit kvdb.
    def /(pattern: String): FilteredConnection = FilteredConnection(pattern.toLabel)

    // Syntactic sugar for sending/receiving messages with their implicit filters.
    def !(msg: Message) { (this / msg.filter) ! msg }
    def ?[T <: Message](continue: T => Unit)(implicit m : Manifest[T]) {
      (this / m.runtimeClass.newInstance.asInstanceOf[T].filter) ? continue }

  }

}