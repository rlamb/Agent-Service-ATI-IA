package com.protegra_ati.agentservices.protocols.verification


import com.protegra_ati.agentservices.protocols.verification.PackageUtil._
import com.protegra_ati.agentservices.protocols.verification.FilteredConnection._
import com.biosimilarity.lift.lib.BasicLogService

object Tweet {
  def apply(msg: String) {
    BasicLogService.tweet("\n\n\n\t\t\t\t"+ msg+"\n\n\n")
  }

  def apply(msgs: Seq[String]) {
    for(msg <- msgs) {
      Tweet(msg)
    }
  }
}

case class VerifierBehavior() {

  def run(kvdbNode: KVDBNode, c2v: Connection) {
    implicit val kvdb = kvdbNode

    // Wait for authorization on a claim
    Tweet("Verifier: Waiting for AuthorizeVerifier")
    c2v ? ({ case AuthorizeVerifier(token, claimStr, c2v, c2r) =>
      Tweet("Verifier: Received AuthorizeVerifier")

      // TODO: Notify the user.
      Tweet("Claim Authorized.")

      // Build the claim.
      val claim = new Claim()
      claim.token = token
      claim.claim = claimStr
      claim.verifier = c2v.trgt.toString
      claim.relParty = c2r.trgt.toString
      claim.claimant = c2r.src.toString

      // Wait for the claim to be submitted.
      val r2c = claim.connection(RelParty, Claimant)
      val r2v = claim.connection(RelParty, Verifier)

      Tweet("Verifier: Waiting for ProduceClaim")
      r2v ? ({ case ProduceClaim(tokenPrime, claimStrPrime, r2cPrime, r2vPrime)
        if (tokenPrime == token &&
            claimStrPrime == claimStr &&
            r2cPrime == r2c &&
            r2vPrime == r2v) =>
        Tweet("Verifier: Got ProduceClaim")

        // TODO: Notify the user.
        BasicLogService.tweet("\n\nClaim Produced.\n\n")

        // Validate the claim
        val v2c = claim.connection(Verifier, Claimant)
        val v2r = claim.connection(Verifier, RelParty)

        Tweet("Verifier: Sending ValidateClaim")
        v2r ! ValidateClaim(token, claimStr, check(claim), v2c, v2r)
        Tweet("Verifier: Sent ValidateClaim")

      }: ProduceClaim => Unit)
    }: AuthorizeVerifier => Unit)

  }

  def check(claim: Claim): Boolean = { true }
}

case class RelyingPartyBehavior() {

  def run(kvdbNode: KVDBNode, c2r: Connection) {
    implicit val kvdb = kvdbNode

    // Wait for claim.
    Tweet("Relying Party: Waiting for SubmitClaim")
    c2r ? ({ case SubmitClaim(token, claimStr, c2v, c2r) =>
      BasicLogService.tweet("Relying Party: Received a SubmitClaim")

      // TODO: Notify the user.

      // Build the claim.
      val claim = new Claim()
      claim.token = token
      claim.claim = claimStr
      claim.verifier = c2v.trgt.toString
      claim.relParty = c2r.trgt.toString
      claim.claimant = c2r.src.toString

      // Produce the claim
      val r2c = claim.connection(RelParty, Claimant)
      val r2v = claim.connection(RelParty, Verifier)

      BasicLogService.tweet("Relying Party: Sending ProduceClaim")
      r2v ! ProduceClaim(token, claimStr, r2c, r2v)
      BasicLogService.tweet("Relying Party: Sent ProduceClaim")

      // Wait for the verifier
      val v2c = claim.connection(Verifier, Claimant)
      val v2r = claim.connection(Verifier, RelParty)

      BasicLogService.tweet("Relying Party: Waiting for ValidateClaim")
      v2r ? ({ case ValidateClaim(tokenPrime, claimStrPrime, response, v2cPrime, v2rPrime) =>
        BasicLogService.tweet("Relying Party: Received ValidateClaim")

        // TODO: Notify the user.
        BasicLogService.tweet("\n\nClaim Verified.\n\n")

        // Notify the claimant
        BasicLogService.tweet("Relying Party: Sending CompleteClaim")
        r2c ! CompleteClaim(token, claimStr, r2c, r2v)
        BasicLogService.tweet("Relying Party: Sent CompleteClaim")

      }: ValidateClaim => Unit)
    }: SubmitClaim => Unit)
  }
}

case class ClaimantBehavior() {

  def run(kvdbNode: KVDBNode, aliasCnxn: Connection) {
    implicit val kvdb = kvdbNode

    Tweet("Claimant: Waiting for AuthorizeVerifier (non-compliant start message)")
    aliasCnxn ? ({ case AuthorizeVerifier(token, claimStr, c2v, c2r) =>
    Tweet("Claimant: Received AuthorizeVerifier (non-compliant start message)")

      // Build the claim.
      val claim = new Claim()
      claim.token = token
      claim.claim = claimStr
      claim.verifier = c2v.trgt.toString
      claim.relParty = c2r.trgt.toString
      claim.claimant = c2r.src.toString

      // Authorize the verifier.
      Tweet("Claimant: Sending AuthorizeVerifier")
      c2v ! AuthorizeVerifier(token, claimStr, c2v, c2r)
      Tweet("Claimant: Sent AuthorizeVerifier")

      // Submit the claim.
      Tweet("Claimant: Sending SubmitClaim")
      c2r ! SubmitClaim(token, claimStr, c2v, c2r)
      Tweet("Claimant: Sent SubmitClaim")

      // Wait for the relying party
      val r2c = claim.connection(RelParty, Claimant)
      val r2v = claim.connection(RelParty, Verifier)

      Tweet("Claimant: Waiting for CompleteClaim")
      r2c ? ({ case CompleteClaim(tokenPrime, claimStrPrime, r2cPrime, r2vPrime) =>
        Tweet("Claimant: Received CompleteClaim")

        // TODO: Notify the user.
        Tweet("\n\nClaim Complete.\n\n")

      }: CompleteClaim => Unit)
    }: AuthorizeVerifier => Unit)
  }
}

package test {

import FilteredConnection._
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.biosimilarity.evaluator.distribution.PortableAgentCnxn
import java.net.URI

object Behaviors2 {


  val dslNodeKey = "foobarbazqux"
  implicit val kvdb: Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse] = {
    val res: Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse] =
      com.biosimilarity.evaluator.distribution.diesel.DieselEngineCtor.dslEvaluatorAgent()
    com.biosimilarity.evaluator.distribution.diesel.EvalNodeMapper += (dslNodeKey -> res)
    res
  }

  def agent(str: String) = s"agent://%s".format(str)

  val mockClaim = new Claim()
  mockClaim.token = "kjhaslkdjhflakjshflkajshdfasdflakjhasdf"
  mockClaim.claim = "claimy claim claim"
  mockClaim.verifier = agent("verifier")
  mockClaim.relParty = agent("relParty")
  mockClaim.claimant = agent("claimant")

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

  val clmtAlias = PortableAgentCnxn(new URI(agent("gloseval")), "MyLabel", new URI(agent("claimant")))

  def exercise() {
    verifier.run(kvdb, c2v)
    relyingParty.run(kvdb, c2r)
    claimant.run(kvdb, clmtAlias)
    readLine("Press enter key to submit claim")
    Tweet("Sending AuthorizeVerifier to Verifier via Claimant")
    val msg = AuthorizeVerifier(mockClaim.token, mockClaim.claim, c2v, c2r)
    clmtAlias ! AuthorizeVerifier(mockClaim.token, mockClaim.claim, c2v, c2r)
    Tweet("Sent AuthorizeVerifier to Verifier via Claimant")
  }
  }
}