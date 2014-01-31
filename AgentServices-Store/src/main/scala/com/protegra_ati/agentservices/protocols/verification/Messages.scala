package com.protegra_ati.agentservices.protocols.verification

import com.protegra_ati.agentservices.store.util.Sugar
import Sugar._
import com.protegra_ati.agentservices.store.extensions.StringExtensions._
import com.biosimilarity.lift.lib.BasicLogService

sealed class Message {
 val filter = SimpleFilter(this.getClass)
 val label = SimpleLabel(this)
}

sealed class Request extends Message
case object SubmitClaim extends Request
case object ProduceClaim extends Request
case object TapClaimant extends Request
case object AuthorizeVerifier extends Request
case object ValidateClaim extends Request
case object CompleteClaim extends Request
case object CancelClaim extends Request

sealed class Response extends Message
case object ClaimSubmitted extends Response
case object ClaimProduced extends Response
case object AuthorizationRequested extends Response
case object ClaimAuthorized extends Response
case object ClaimVerified extends Response
case object ClaimComplete extends Response
case object ClaimCancelled extends Response


// Request/Response Pairs

// Claimant submits claim to RelyingParty,
case class SubmitClaim(token: String, claim: String, c2v: Connection, c2r: Connection) extends Request
case class ClaimSubmitted(token: String, claim: String, c2v: Connection) extends Response

// RelyingParty produces claim to Verifier,
case class ProduceClaim(token: String, claim: String, r2c: Connection, r2v: Connection) extends Request
case class ClaimProduced(token: String, claim: String, r2c: Connection) extends Response

// Verifier taps Claimant,
case class TapClaimant(token: String, claim: String, v2c: Connection, v2r: Connection) extends Request
case class AuthorizationRequested(token: String, claim: String, v2r: Connection) extends Response

// Client authorizes Verifier,
case class AuthorizeVerifier(token: String, claim: String, c2v: Connection, c2r: Connection) extends Request
case class ClaimAuthorized(token: String, claim: String, c2r: Connection) extends Response

// Verifier validates claim to RelyingParty,
case class ValidateClaim(token: String, claim: String, response: Boolean, v2c: Connection, v2r: Connection) extends Request
case class ClaimVerified(token: String, claim: String, response: Boolean, v2c: Connection) extends Response

// finally, RelyingParty completes claim with the claimant.
case class CompleteClaim(token: String, claim: String, r2c: Connection, r2v: Connection) extends Request
case class ClaimComplete(token: String, claim: String, r2v: Connection) extends Response

// Also, anybody can cancel a claim.
case class CancelClaim(token: String, claim: String, a2b: Connection, a2c: Connection) extends Request
case class ClaimCancelled(token: String, claim: String) extends Response









