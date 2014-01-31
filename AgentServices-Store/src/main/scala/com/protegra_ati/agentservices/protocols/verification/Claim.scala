package com.protegra_ati.agentservices.protocols.verification

import com.protegra_ati.agentservices.store.util.Sugar._
import com.biosimilarity.evaluator.distribution.PortableAgentCnxn
import java.net.URI

// Types
sealed abstract trait PartyType
case object Verifier extends PartyType
case object RelParty extends PartyType
case object Claimant extends PartyType

class Claim() extends Serializable {

  var cancelled: Boolean = false
  var token: String = "N/A"
  var claim: String = "N/A"

  var verifier: String = "N/A"
  var relParty: String = "N/A"
  var claimant: String = "N/A"

  def connection(sender: PartyType, receiver: PartyType): Connection = {
    val (source, label, target) =
      (sender, receiver) match {
        case (Verifier, RelParty) => (verifier, "NODE_V2R_LABEL", relParty)
        case (Verifier, Claimant) => (verifier, "NODE_V2C_LABEL", claimant)
        case (RelParty, Verifier) => (relParty, "NODE_R2V_LABEL", verifier)
        case (RelParty, Claimant) => (relParty, "NODE_R2C_LABEL", claimant)
        case (Claimant, Verifier) => (claimant, "NODE_C2V_LABEL", verifier)
        case (Claimant, RelParty) => (claimant, "NODE_C2R_LABEL", relParty)
      }

    PortableAgentCnxn(new URI(source), label, new URI(target))
  }

  def isSubClaimOf(claim: Claim): Boolean = {
    val c1 = token == claim.token
    val c2 = this.claim == claim.claim
    val c3a = claim.verifier == "N/A" || verifier == claim.verifier
    val c3b = claim.relParty == "N/A" || relParty == claim.relParty
    val c3c = claim.claimant == "N/A" || claimant == claim.claimant
    val c3 =
      ((c3a && c3b && c3c)
        || (!c3a && c3b && c3c)
        || (c3a && !c3b && c3c)
        || (c3a && c3b && !c3c))
    if(!c1) { Tweet("c1 fails") }
    if(!c2) { Tweet("c2 fails") }
    if(!c3) { Tweet("c3 fails") }
    c1 && c2 && c3
  }

  override def toString = s"($token, $claim, $verifier, $relParty, $claimant)"

}

object Claim {

  def apply(token: String, claimStr: String, connections: Connection*): Claim = {
    val claim = new Claim()
    claim.token = token
    claim.claim = claimStr

    def addConnection(connection: Connection) {
      def catchConflict(left: String, right: String): String = {
        if(left == "N/A" || left == right) {
          right
        } else {
          throw new RuntimeException("Invalid update in Claim: %s != %s".format(left, right))
        }
      }

      connection.label match {
        case "NODE_V2R_LABEL" =>
          claim.verifier = catchConflict(claim.verifier, connection.src.toString)
          claim.relParty = catchConflict(claim.relParty, connection.trgt.toString)
        case "NODE_V2C_LABEL" =>
          claim.verifier = catchConflict(claim.verifier, connection.src.toString)
          claim.claimant = catchConflict(claim.claimant, connection.trgt.toString)
        case "NODE_R2V_LABEL" =>
          claim.relParty = catchConflict(claim.relParty, connection.src.toString)
          claim.verifier = catchConflict(claim.verifier, connection.trgt.toString)
        case "NODE_R2C_LABEL" =>
          claim.relParty = catchConflict(claim.relParty, connection.src.toString)
          claim.claimant = catchConflict(claim.claimant, connection.trgt.toString)
        case "NODE_C2V_LABEL" =>
          claim.claimant = catchConflict(claim.claimant, connection.src.toString)
          claim.verifier = catchConflict(claim.verifier, connection.trgt.toString)
        case "NODE_C2R_LABEL" =>
          claim.claimant = catchConflict(claim.claimant, connection.src.toString)
          claim.relParty = catchConflict(claim.relParty, connection.trgt.toString)
        case unexpected => throw new RuntimeException("Unexpected connection label: %s".format(unexpected))
      }
    }

    for(connection <- connections) { addConnection(connection) }
    claim
  }
}
