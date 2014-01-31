package com.protegra_ati.agentservices.protocols.verification

import com.protegra_ati.agentservices.store.util.Sugar._
import com.biosimilarity.evaluator.distribution.bfactory.BFactoryDefaultServiceContext._
import com.protegra_ati.agentservices.protocols.verification.FilteredConnection._
import com.biosimilarity.lift.lib.BasicLogService

sealed abstract class VerificationBehavior()

case class VerifierBehavior() extends VerificationBehavior() {

  def run(kvdbNode: KVDBNode, alias: Connection, c2v: Connection) {
    implicit val kvdb = kvdbNode

    // Wait for AuthorizeVerifier
    Tweet("Verifier: Waiting for AuthorizeVerifier")
    c2v ? ({ case AuthorizeVerifier(token, claimStr, c2v, c2r) =>
      Tweet("Verifier: Got AuthorizeVerifier")

      // Build the claim
      val claim = Claim(token, claimStr, c2v, c2r)

      // Get the channels
      val v2c = claim.connection(Verifier, Claimant)
      val v2r = claim.connection(Verifier, RelParty)
      val r2v = claim.connection(RelParty, Verifier)
      val r2c = claim.connection(RelParty, Claimant)

      // Notify the alias
      Tweet("Verifier: Sending ClaimAuthorized")
      alias ! ClaimAuthorized(token, claimStr, v2r)
      Tweet("Verifier: Sent ClaimAuthorized")

      // Wait for ProduceClaim
      Tweet("Verifier: Waiting for ProduceClaim")
      r2v ? ({ case ProduceClaim(tokenPrime, claimStrPrime, r2cPrime, r2vPrime) =>
        if(Claim(tokenPrime, claimStrPrime, r2cPrime, r2vPrime).isSubClaimOf(claim)) {
          Tweet("Verifier: Got ProduceClaim")

          // Notify the alias
          Tweet("Verifier: Sending ClaimProduced")
          alias ! ClaimProduced(token, claimStr, r2c)
          Tweet("Verifier: Sent ClaimProduced")

          // Wait for ValidateClaim
          Tweet("Verifier: Waiting for ValidateClaim")
          alias ? ({ case ValidateClaim(tokenPrime, claimStrPrime, response, v2cPrime, v2rPrime) =>
            if(Claim(tokenPrime, claimStrPrime, v2rPrime, v2cPrime).isSubClaimOf(claim)) {
              Tweet("Verifier: Got ValidateClaim")

              // Send ValidateClaim to relying party
              Tweet("Verifier: Sending ValidateClaim")
              v2r ! ValidateClaim(tokenPrime, claimStrPrime, response, v2cPrime, v2rPrime)
              Tweet("Verifier: Sent ValidateClaim")
            }
          }: ValidateClaim => Unit)
        }
      }: ProduceClaim => Unit)
    }: AuthorizeVerifier => Unit)


  }

}

case class RelyingPartyBehavior() extends VerificationBehavior() {

  def run(kvdbNode: KVDBNode, alias: Connection, c2r: Connection) {
    implicit val kvdb = kvdbNode

    // Wait for SubmitClaim
    Tweet("RelyingParty: Waiting for SubmitClaim")
    c2r ? ({ case SubmitClaim(token, claimStr, c2v, c2r) =>
      Tweet("RelyingParty: Got SubmitClaim")

      // Build the claim
      val claim = Claim(token, claimStr, c2v, c2r)

      // Get the channels
      val r2c = claim.connection(RelParty, Claimant)
      var r2v = claim.connection(RelParty, Verifier)
      val v2r = claim.connection(Verifier, RelParty)
      val v2c = claim.connection(Verifier, Claimant)

      // Notify the alias.
      Tweet("RelyingParty: Sending ClaimSubmitted")
      alias ! ClaimSubmitted(token, claimStr, c2v)
      Tweet("RelyingParty: Sent ClaimSubmitted")

      // Wait for ProduceClaim
      Tweet("RelyingParty: Waiting for ProduceClaim")
      alias ? ({ case ProduceClaim(tokenPrime, claimStrPrime, r2cPrime, r2vPrime) =>
        if(Claim(tokenPrime, claimStrPrime, r2cPrime, r2vPrime).isSubClaimOf(claim)) {
          Tweet("RelyingParty: Got ProduceClaim")

          // Send ProduceClaim to verifier
          Tweet("RelyingParty: Sending ProduceClaim")
          r2v ! ProduceClaim(token, claimStr, r2c, r2v)
          Tweet("RelyingParty: Sent ProduceClaim")

          // Wait for ValidateClaim
          Tweet("RelyingParty: Waiting for ValidateClaim")
          v2r ? ({ case ValidateClaim(tokenPrime, claimStrPrime, response, v2cPrime, v2rPrime) =>
            if(Claim(tokenPrime, claimStrPrime, v2cPrime, v2rPrime).isSubClaimOf(claim)) {
              Tweet("RelyingParty: Got ValidateClaim")

              // Notify the alias
              Tweet("RelyingParty: Sending ClaimVerified")
              alias ! ClaimVerified(token, claimStr, response, v2c)
              Tweet("RelyingParty: Sent ClaimVerified")

              // Wait for CompleteClaim
              Tweet("RelyingParty: Waiting for CompleteClaim")
              alias ? ({ case CompleteClaim(tokenPrime, claimStrPrime, r2cPrime, r2vPrime) =>
                if(Claim(tokenPrime, claimStrPrime, v2cPrime, v2rPrime).isSubClaimOf(claim)) {
                  Tweet("RelyingParty: Got CompleteClaim")

                  // Send CompleteClaim to Claimant
                  Tweet("RelyingParty Sending CompleteClaim")
                  r2c ! CompleteClaim(token, claimStr, r2c, r2v)
                  Tweet("RelyingParty Sent CompleteClaim")
                }
              }: CompleteClaim => Unit)
            }
          }: ValidateClaim => Unit)
        } else Tweet("%s != %s".format(Claim(tokenPrime, claimStrPrime, r2cPrime, r2vPrime).toString, claim.toString))
      }: ProduceClaim => Unit)
    }: SubmitClaim => Unit)


  }

}

case class ClaimantBehavior() extends VerificationBehavior() {

  def run(kvdbNode: KVDBNode, alias: Connection) {
    implicit val kvdb = kvdbNode

    // Wait for SubmitClaim
    Tweet("Claimant: Waiting for SubmitClaim")
    alias ? ({ case SubmitClaim(token, claimStr, c2v, c2r) =>
      Tweet("Claimant: Got SubmitClaim")

      // Build the claim
      val claim = Claim(token, claimStr, c2v, c2r)

      // Get the channels
      val v2c = claim.connection(Verifier, Claimant)
      val r2c = claim.connection(RelParty, Claimant)
      val r2v = claim.connection(RelParty, Verifier)

      // Send AuthorizeVerifier to the verifier
      Tweet("Claimant: Sending AuthorizeVerifier")
      c2v ! AuthorizeVerifier(token, claimStr, c2v, c2r)
      Tweet("Claimant: Sent AuthorizeVerifier")

      // Send SubmitClaim to the reliant party
      Tweet("Claimant: Sending SubmitClaim")
      c2r ! SubmitClaim(token, claimStr, c2v, c2r)
      Tweet("Claimant: Sent SubmitClaim")

      // Wait for CompleteClaim
      Tweet("Claimant: Waiting for CompleteClaim")
      r2c ? ({ case CompleteClaim(tokenPrime, claimStrPrime, r2cPrime, r2vPrime) =>
        if(Claim(tokenPrime, claimStrPrime, r2cPrime, r2vPrime).isSubClaimOf(claim)){
          Tweet("Claimant: Got CompleteClaim")

          // Notify the alias
          Tweet("Claimant: Sending ClaimComplete")
          alias ! ClaimComplete(token, claimStr, r2v)
          Tweet("Claimant: Sent ClaimComplete")
        }
      }: CompleteClaim => Unit)
    }: SubmitClaim => Unit)
  }

}

object Behaviors {
  import FilteredConnection._
  import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
  import com.biosimilarity.evaluator.distribution.PortableAgentCnxn

  val dslNodeKey = "foobarbazqux"
  implicit val kvdb: Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse] = {
    val res: Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse] =
      com.biosimilarity.evaluator.distribution.diesel.DieselEngineCtor.dslEvaluatorAgent()
    com.biosimilarity.evaluator.distribution.diesel.EvalNodeMapper += (dslNodeKey -> res)
    res
  }

  val mockClaim = new Claim()
  mockClaim.token = "kjhaslkdjhflakjshflkajshdfasdflakjhasdf"
  mockClaim.claim = "claimy claim claim"
  mockClaim.verifier = "agent://verifier"
  mockClaim.relParty = "agent://relParty"
  mockClaim.claimant = "agent://claimant"

  val verifier = VerifierBehavior()
  val relyingParty = RelyingPartyBehavior()
  val claimant = ClaimantBehavior()

  val (v2r, v2c, r2v, r2c, c2v, c2r) =
    (mockClaim.connection(Verifier, RelParty),
      mockClaim.connection(Verifier, Claimant),
      mockClaim.connection(RelParty, Verifier),
      mockClaim.connection(RelParty, Claimant),
      mockClaim.connection(Claimant, Verifier),
      mockClaim.connection(Claimant, RelParty))

  val alias = PortableAgentCnxn("alias", "id", "alias")

  def exercise() {
    verifier.run(kvdb, alias, c2v)
    relyingParty.run(kvdb, alias, c2r)
    claimant.run(kvdb, alias)

    readLine("Press enter key to submit claim (as claimant)")
    Tweet("Sending SubmitClaim to Claimant via alias")
    alias ! SubmitClaim(mockClaim.token, mockClaim.claim, c2v, c2r)
    Tweet("Sent SubmitClaim to Claimant via alias")
    Thread.sleep(2500)

    readLine("Press enter key to produce the claim (as relying party)")
    Tweet("Sending ProduceClaim to Verifier via alias")
    alias ! ProduceClaim(mockClaim.token, mockClaim.claim, r2c, r2v)
    Tweet("Sent ProduceClaim to Verifier via alias")
    Thread.sleep(2500)

    readLine("Press enter key to validate the claim (as verifier)")
    Tweet("Sending ValidateClaim to RelyingParty via alias")
    alias ! ValidateClaim(mockClaim.token, mockClaim.claim, true, v2c, v2r)
    Tweet("Sent ValidateClaim to RelyingParty via alias")

    readLine("Press enter key to complete the claim (as relying party)")
    Tweet("Sending CompleteClaim to Claimant via alias")
    alias ! CompleteClaim(mockClaim.token, mockClaim.claim, r2c, r2v)
    Tweet("Sent CompleteClaim to Claimant via alias")
  }
}
