package com.protegra_ati.agentservices.protocols.verificationProtocol

import PackageUtil._
import com.protegra_ati.agentservices.store.extensions.StringExtensions._

import com.biosimilarity.evaluator.distribution.PortableAgentCnxn

sealed abstract class Message(args: Any*) {
  
  val filter =
    s"%s(%s)".format(
      this.getClass.getSimpleName.toCamelCase,
      ",_" * args.length)

  val label: Label = filter.toLabel
}


sealed abstract class Request(args: Any*) extends Message(args: _*)
case object SubmitClaim extends Request(Nil, Nil, Nil, Nil)
case object ProduceClaim extends Request(Nil, Nil, Nil, Nil)
case object TapClaimant extends Request(Nil, Nil, Nil, Nil)
case object AuthorizeVerifier extends Request(Nil, Nil, Nil, Nil)
case object ValidateClaim extends Request(Nil, Nil, Nil, Nil, Nil)
case object CompleteClaim extends Request(Nil, Nil, Nil, Nil)
case object CancelClaim extends Request(Nil, Nil, Nil, Nil)

case class SubmitClaim(token: String,
                       claim: String,
                       verifier: PortableAgentCnxn,
                       reliantParty: PortableAgentCnxn) extends Request(token, claim, verifier, reliantParty)

case class ProduceClaim(token: String,
                        claim: String,
                        claimant: PortableAgentCnxn,
                        verifier: PortableAgentCnxn) extends Request(token, claim, claimant, verifier)

case class TapClaimant(token: String,
                       claim: String,
                       claimant: PortableAgentCnxn,
                       reliantParty: PortableAgentCnxn) extends Request(token, claim, claimant, reliantParty)

case class AuthorizeVerifier(token: String,
                             claim: String,
                             verifier: PortableAgentCnxn,
                             reliantParty: PortableAgentCnxn) extends Request()

case class ValidateClaim(token: String,
                         claim: String,
                         response: Boolean,
                         claimant: PortableAgentCnxn,
                         reliantParty: PortableAgentCnxn) extends Request(token, claim, response, claimant, reliantParty)

case class CompleteClaim(token: String,
                         claim: String,
                         claimant: PortableAgentCnxn,
                         verifier: PortableAgentCnxn) extends Request(token, claim, claimant, verifier)

case class CancelClaim(token: String,
                       claim: String,
                       partyA: PortableAgentCnxn,
                       partyB: PortableAgentCnxn) extends Request(token, claim, partyA, partyB)



sealed abstract class Response(args: Any*) extends Message(args: _*)
case object ClaimSubmitted extends Response(Nil, Nil, Nil)
case object ClaimProduced extends Response(Nil, Nil, Nil)
case object AuthorizationRequested extends Response(Nil, Nil, Nil)
case object ClaimAuthorized extends Response(Nil, Nil, Nil)
case object ClaimVerified extends Response(Nil, Nil, Nil)
case object ClaimComplete extends Response(Nil, Nil, Nil)
case object ClaimCancelled extends Response(Nil, Nil)


case class ClaimSubmitted(token: String,
                          claim: String,
                          verifier: PortableAgentCnxn) extends Response(token, claim, verifier)

case class ClaimProduced(token: String,
                         claim: String,
                         claimant: PortableAgentCnxn) extends Response(token, claim, claimant)

case class AuthorizationRequested(token: String,
                                  claim: String,
                                  reliantParty: PortableAgentCnxn) extends Response(token, claim, reliantParty)

case class ClaimAuthorized(token: String,
                           claim: String,
                           reliantParty: PortableAgentCnxn) extends Response(token, claim, reliantParty)

case class ClaimVerified(token: String,
                         claim: String,
                         response: Boolean,
                         claimant: PortableAgentCnxn) extends Response(token, claim, claimant)

case class ClaimComplete(token: String,
                         claim: String,
                         verifier: PortableAgentCnxn) extends Response(token, claim, verifier)

case class ClaimCancelled(token: String,
                          claim: String) extends Response(token, claim)
