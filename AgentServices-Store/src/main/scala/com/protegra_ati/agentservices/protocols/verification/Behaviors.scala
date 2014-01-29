package com.protegra_ati.agentservices.verificationProtocol

import PackageUtil._
import com.protegra_ati.agentservices.verificationProtocol.FilteredConnection._

case class VerifierBehavior() {

  def run(kvdbNode: KVDBNode, r2v: Connection) {
    implicit val kvdb = kvdbNode

    // Listen for a claim.
    r2v ? ({ case ProduceClaim(token, claimStr, r2c, r2vPrime) if r2vPrime == r2v =>

      // Build the claim.
      val claim = new Claim()
      claim.token = token
      claim.claim = claimStr
      claim.verifier = r2v.trgt.toString
      claim.relParty = r2v.src.toString
      claim.claimant = r2c.trgt.toString

      // Push a notification to GLoSEval.
      r2v ! ClaimProduced(token, claimStr, r2c)

      // Request the claimant's permission to verify the claim.
      val v2c = claim.connection(Verifier, Claimant)
      val v2r = claim.connection(Verifier, RelParty)

      v2c ! TapClaimant(token, claimStr, v2c, v2r)

      // Wait for permission to verify the claim.
      val c2v = claim.connection(Claimant, Verifier)
      val c2r = claim.connection(Claimant, RelParty)

      c2v ? ({ case AuthorizeVerifier(tokenPrime, claimStrPrime, c2vPrime, c2rPrime)
        if (c2vPrime == c2v &&
            c2rPrime == c2r &&
            tokenPrime == token &&
            claimStrPrime == claimStr) =>

        // Push a notification to GLoSEval
        (c2v / ClaimAuthorized.filter) ! ClaimAuthorized(token, claimStr, c2r)

      }: AuthorizeVerifier => Unit)
    }: ProduceClaim => Unit)
  }
}

case class ReliantPartyBehavior() {

  def run(kvdbNode: KVDBNode, c2r: Connection) {
    implicit val kvdb = kvdbNode

    // Listen for a claim.
    c2r ? ({ case SubmitClaim(token, claimStr, c2v, c2rPrime) if c2rPrime == c2r =>

      // Build the claim.
      val claim = new Claim()
      claim.token = token
      claim.claim = claimStr
      claim.verifier = c2v.trgt.toString
      claim.relParty = c2r.trgt.toString
      claim.claimant = c2r.src.toString

      // Push a notification to GLoSEval.
      c2r ! ClaimSubmitted(token, claimStr, c2v)

      // Produce the claim for the verifier.
      val r2c = claim.connection(RelParty, Claimant)
      val r2v = claim.connection(RelParty, Verifier)

      r2v ! ProduceClaim(token, claimStr, r2c, r2v)

      // Wait for the verifier to validate the claim.
      val v2c = claim.connection(Verifier, Claimant)
      val v2r = claim.connection(Verifier, RelParty)

      claim.connection(Verifier, RelParty) ? ({ case ValidateClaim(tokenPrime, claimStrPrime, ans, v2cPrime, v2rPrime)
        if (v2cPrime == v2c &&
            v2rPrime == v2r &&
            tokenPrime == token &&
            claimStrPrime == claimStr) =>

        // Push a notification to GLoSEval
        v2r ! ClaimVerified(token, claimStr, ans, v2c)

        // Notify the claimant
        r2c ! CompleteClaim(token, claimStr, r2c, r2v)

      }: ValidateClaim => Unit)
    }: SubmitClaim => Unit)
  }
}

case class ClaimantBehavior() {

  def run(kvdbNode: KVDBNode, v2c: Connection) {
    implicit val kvdb = kvdbNode

    // Wait for verifier.
    v2c ? ({ case TapClaimant(token, claimStr, v2cPrime, v2r) if v2cPrime == v2c =>

      // Build the claim.
      val claim = new Claim()
      claim.token = token
      claim.claim = claimStr
      claim.verifier = v2c.src.toString
      claim.relParty = v2r.trgt.toString
      claim.claimant = v2c.trgt.toString

      // Push notification to GLoSEval
      v2c ! AuthorizationRequested(token, claimStr, v2r)

      // Wait for reliant party.
      val r2c = claim.connection(RelParty, Claimant)
      val r2v = claim.connection(RelParty, Verifier)

      r2c ? ({ case CompleteClaim(tokenPrime, claimStrPrime, r2cPrime, r2vPrime)
        if (r2cPrime == r2c &&
            r2vPrime == r2v &&
            tokenPrime == token &&
            claimStrPrime == claimStr) =>

        // Push notification to GLoSEval
        r2c ! ClaimComplete(token, claimStr, r2v)

      }: CompleteClaim => Unit)
    }: TapClaimant => Unit)
  }
}

package usage {

import java.net.URI
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._

object VPTest {

  val dslNodeKey = "foobarbazqux"
  implicit val kvdb: Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse] = {
    val res: Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse] =
      com.biosimilarity.evaluator.distribution.diesel.DieselEngineCtor.dslEvaluatorAgent()
    com.biosimilarity.evaluator.distribution.diesel.EvalNodeMapper += (dslNodeKey -> res)
    res
  }

  val addr = "agent://e6eca9b84dd49d91e45c9e23cd74d2d98bb1"

  val mockClaim = new Claim()
  mockClaim.token = "kjhaslkdjhflakjshflkajshdfasdflakjhasdf"
  mockClaim.claim = "claimy claim claim"
  mockClaim.verifier = addr
  mockClaim.relParty = addr
  mockClaim.claimant = addr

  val verifier = VerifierBehavior()
  val reliantParty = ReliantPartyBehavior()
  val claimant = ClaimantBehavior()

  val (v2r, v2c, r2v, r2c, c2v, c2r) =
    (mockClaim.connection(Verifier, RelParty),
      mockClaim.connection(Verifier, Claimant),
      mockClaim.connection(RelParty, Verifier),
      mockClaim.connection(RelParty, Claimant),
      mockClaim.connection(Claimant, Verifier),
      mockClaim.connection(Claimant, RelParty))

  def exercise() {
    verifier.run(kvdb, r2v)
    reliantParty.run(kvdb, c2r)
    claimant.run(kvdb, v2c)
    readLine("Press enter key to submit claim")
    r2v ! SubmitClaim(mockClaim.token, mockClaim.claim, c2v, c2r)
  }

}


}