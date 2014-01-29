package com.protegra_ati.agentservices.verificationProtocol

import org.json4s.{ JValue => JSON, _}
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._
import com.protegra_ati.agentservices.verificationProtocol.FilteredConnection.PortableAgentConnection
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

  def connection(sender: PartyType, receiver: PartyType): PortableAgentCnxn = {
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
    val c1 = token.equals(claim.token)
    val c2 = claim.equals(claim.claim)
    val c3a = claim.verifier.equals("N/A") || verifier.equals(claim.verifier)
    val c3b = claim.relParty.equals("N/A") || relParty.equals(claim.relParty)
    val c3c = claim.claimant.equals("N/A") || claimant.equals(claim.claimant)
    val c3 =
      ((c3a && c3b && c3c)
        || (!c3a && c3b && c3c)
        || (c3a && !c3b && c3c)
        || (c3a && c3b && !c3c))
    c1 && c2 && c3
  }

}

