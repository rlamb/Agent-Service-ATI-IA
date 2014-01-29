package com.protegra_ati.agentservices.verificationProtocol

/*

import com.protegra_ati.agentservices.verificationProtocol.PackageUtil._
import com.protegra_ati.agentservices.verificationProtocol.FilteredConnection._
import com.biosimilarity.lift.lib.BasicLogService

class VerifierBehavior3 {

  def run(kvdb: KVDBNode, v2g: Connection, c2v: Connection) {
    Tweet("Verifier: Waiting for AuthorizeVerifier")
    c2v ? ({ case AuthorizeVerifier(token, claimStr, c2v, c2r) =>
      Tweet("Verifier: Received AuthorizeVerifier")

      // Build the claim.
      val claim = new Claim()
      claim.token = token
      claim.claim = claimStr
      claim.verifier = c2v.trgt.toString
      claim.relParty = c2r.trgt.toString
      claim.claimant = c2r.src.toString

      // Push a notification to GLoSEval
      v2g ! ClaimAuthorized(token, claimStr, c2r)

      // Wait for the Relying Party to produce the claim.
      val r2v = claim.connection(RelParty, Verifier)

      r2v ? ({ case ProduceClaim(tokenPrime, claimStrPrime, r2c, r2v) =>

        // Push a notification to GLoSEval
        v2g ! ClaimProduced(token, claimStr, r2c)

      }: ProduceClaim => Unit)

    }: AuthorizeVerifier => Unit)

  }
}

class ReliantParty3 {

  def run(kvdb: KVDBNode, )
}
*/