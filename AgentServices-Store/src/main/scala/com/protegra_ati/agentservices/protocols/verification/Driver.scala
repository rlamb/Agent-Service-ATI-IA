// -*- mode: Scala;-*- 
// Filename:    Driver.scala 
// Authors:     lgm                                                    
// Creation:    Thu Feb 13 16:40:06 2014 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.protegra_ati.agentservices.protocols

import com.biosimilarity.evaluator.distribution.{PortableAgentCnxn, PortableAgentBiCnxn}
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.biosimilarity.evaluator.distribution.ConcreteHL.PostedExpr
import com.protegra_ati.agentservices.protocols.msgs._
import com.biosimilarity.lift.model.store.CnxnCtxtLabel
import com.biosimilarity.lift.model.store.CnxnCtxtBranch
import com.biosimilarity.lift.model.store.CnxnCtxtLeaf
import com.biosimilarity.lift.model.store.Factual
import com.biosimilarity.lift.lib._
import scala.util.continuations._
import scala.collection.mutable.HashMap
import java.util.UUID

package usage {  
  import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
  import com.biosimilarity.evaluator.distribution.diesel.EvalNodeMapper
  import com.biosimilarity.evaluator.distribution.diesel.DieselEngineCtor
  import com.biosimilarity.evaluator.distribution.diesel.DieselEngineCtor.StdEvalChannel

  import com.biosimilarity.evaluator.distribution.utilities.DieselValueTrampoline._

  import com.biosimilarity.evaluator.distribution.NodeStreams
  import com.biosimilarity.evaluator.distribution.FuzzyStreams
  import com.biosimilarity.evaluator.distribution.FuzzyTerms
  import com.biosimilarity.evaluator.distribution.FuzzyTermStreams

  import com.protegra_ati.agentservices.store.extensions.StringExtensions._

  import com.biosimilarity.lift.model.store.CnxnString
  //import scala.concurrent.FJTaskRunnersX  
  import com.biosimilarity.lift.lib.concurrent._
  import com.biosimilarity.lift.lib.concurrent.cpsops._

  trait GLoSStubT
    extends NodeStreams
     with FuzzyStreams
     with FuzzyTerms
     with FuzzyTermStreams
     with CnxnString[String,String,String] 
     with Serializable
  {
    def claimantToGLoS : PortableAgentCnxn
    def verifierToGLoS : PortableAgentCnxn
    def relyingPartyToGLoS : PortableAgentCnxn

    def waitForSignalToInitiateClaim(
      continuation : CnxnCtxtLabel[String,String,String] => SimulationContext
    ) : SimulationContext = {     
      println( "Please state your claim: " )
      val ln = readLine() // Note: this is blocking.
      val claim = ln.toLabel
      println( "your claim, " + claim + ", has been submitted." )
      continuation( claim )      
     }    

    def waitForVerifierVerificationNotification(
      node : StdEvalChannel,      
      continuation : VerificationMessage => Unit
    ) : Unit = {
      val vrfr2GLoSRd =
        acT.AgentCnxn( verifierToGLoS.src, verifierToGLoS.label, verifierToGLoS.trgt )

      BasicLogService.tweet(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\nwaiting for verification notification from verifier on: " 
          + "cnxn: " + vrfr2GLoSRd
          + "label: " + VerificationNotification.toLabel
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )
      println(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\nwaiting for verification notification from verifier on: " 
          + "cnxn: " + vrfr2GLoSRd
          + "label: " + VerificationNotification.toLabel
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )

      reset {
        for(
          eVNote <- node.subscribe(
            vrfr2GLoSRd
          )( VerificationNotification.toLabel() )
        ) {
          rsrc2V[VerificationMessage]( eVNote ) match {
            case Left( vmsg@VerificationNotification( sidVN, cidVN, clmntVN, clmVN, witVN ) ) => { 
              BasicLogService.tweet(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nreceived verification notification " + eVNote
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              println(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nreceived verification notification " + eVNote
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              continuation( vmsg )
            }
            case Right( true ) => {
              BasicLogService.tweet(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nstill waiting for verifier verification notification"
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              println(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nstill waiting for verifier verification notification"
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
            }
            case _ => {
              BasicLogService.tweet(
                "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                + "\nunexpected protocol message : " + eVNote
                + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
              )
              println(
                "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                + "\nunexpected protocol message : " + eVNote
                + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
              )
            }
          }
        }
      }
    }

    def waitForRelyingPartyVerificationNotification(
      node : StdEvalChannel,
      continuation : VerificationMessage => Unit
    ) : Unit = {
      val rp2GLoSRd =
        acT.AgentCnxn( relyingPartyToGLoS.src, relyingPartyToGLoS.label, relyingPartyToGLoS.trgt )

      BasicLogService.tweet(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\nwaiting for verification notification from relying party on: " 
          + "cnxn: " + rp2GLoSRd
          + "label: " + VerificationNotification.toLabel
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )
      println(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\nwaiting for verification notification from relying party on: " 
          + "cnxn: " + rp2GLoSRd
          + "label: " + VerificationNotification.toLabel
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )

      reset {
        for(
          eVNote <- node.subscribe(
            rp2GLoSRd
          )( VerificationNotification.toLabel() )
        ) {
          rsrc2V[VerificationMessage]( eVNote ) match {
            case Left( vmsg@VerificationNotification( sidVN, cidVN, clmntVN, clmVN, witVN ) ) => { 
              BasicLogService.tweet(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nreceived relying party verification notification " + eVNote
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              println(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nreceived relying party verification notification " + eVNote
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              continuation( vmsg )
            }
            case Right( true ) => {
              BasicLogService.tweet(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nstill waiting for relying party verification notification"
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              println(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nstill waiting for relying party verification notification"
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
            }
            case _ => {
              BasicLogService.tweet(
                "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                + "\nunexpected protocol message : " + eVNote
                + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
              )
              println(
                "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                + "\nunexpected protocol message : " + eVNote
                + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
              )
            }
          }
        }
      }
    }

    def waitForCompleteClaim(
      node : StdEvalChannel,
      continuation : VerificationMessage => Unit
    ) : Unit = {
      val clmnt2GLoSRd =
        acT.AgentCnxn( claimantToGLoS.trgt, claimantToGLoS.label, claimantToGLoS.src )

      BasicLogService.tweet(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\nwaiting for complete claim on: " 
          + "cnxn: " + clmnt2GLoSRd
          + "label: " + CompleteClaim.toLabel
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )
      println(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\nwaiting for complete claim on: " 
          + "cnxn: " + clmnt2GLoSRd
          + "label: " + CompleteClaim.toLabel
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )

      reset {
        for(
          eCompleteClaim <- node.subscribe(
            clmnt2GLoSRd
          )( CompleteClaim.toLabel() )
        ) {
          rsrc2V[VerificationMessage]( eCompleteClaim ) match {
            case Left( vmsg@CompleteClaim( sidCC, cidCC, vrfrCC, clmCC, witCC ) ) => { 
              BasicLogService.tweet(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nreceived complete claim " + eCompleteClaim
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              println(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nreceived complete claim " + eCompleteClaim
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              continuation( vmsg )
            }
            case Right( true ) => {
              BasicLogService.tweet(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nstill waiting for complete claim"
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              println(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nstill waiting for complete claim"
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
            }
            case _ => {
              BasicLogService.tweet(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nunexpected protocol message : " + eCompleteClaim
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              println(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nunexpected protocol message : " + eCompleteClaim
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
            }
          }
        }
      }
    }
  }    

  object RunParameters extends Serializable {
    def claimantRunArgs(
      simCtxt : SimulationContext
    ) : ( List[PortableAgentCnxn], List[CnxnCtxtLabel[String,String,String]] ) = {
      ( List[PortableAgentCnxn]( simCtxt.glosStub.claimantToGLoS ), Nil )
    }
    def verifierRunArgs(
      simCtxt : SimulationContext
    ) : ( List[PortableAgentCnxn], List[CnxnCtxtLabel[String,String,String]] ) = {
      ( List[PortableAgentCnxn]( simCtxt.glosStub.verifierToGLoS, simCtxt.c2v ), Nil )
    }
    def relyingPartyRunArgs(
      simCtxt : SimulationContext
    ) : ( List[PortableAgentCnxn], List[CnxnCtxtLabel[String,String,String]] ) = {
      ( List[PortableAgentCnxn]( simCtxt.glosStub.relyingPartyToGLoS, simCtxt.c2r ), Nil )
    }
  }

  case class GLoSStub(
    override val claimantToGLoS : PortableAgentCnxn,
    override val verifierToGLoS : PortableAgentCnxn,
    override val relyingPartyToGLoS : PortableAgentCnxn
  ) extends GLoSStubT  

  case class SimulationContext(
    node : StdEvalChannel,
    glosStub : GLoSStub,
    sid : String,
    cid : String,
    c : PortableAgentCnxn,
    v : PortableAgentCnxn,
    r : PortableAgentCnxn,
    c2v : PortableAgentCnxn,
    c2r : PortableAgentCnxn,
    v2r : PortableAgentCnxn,
    claim : CnxnCtxtLabel[String,String,String]
  )

  trait VerificationDriverT {
    def simulateInitiateClaimStep(
      simCtxt : SimulationContext
    ) : Unit =
      {
        val SimulationContext( node, glosStub, sid, cid, c, v, r, c2v, c2r, v2r, clm ) = simCtxt
        val agntCRd = 
          acT.AgentCnxn(
            glosStub.claimantToGLoS.src,
            glosStub.claimantToGLoS.label,
            glosStub.claimantToGLoS.trgt
          )
        
        BasicLogService.tweet(
          (
            "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
            + "\npublishing initiate claim on: " 
            + "\ncnxn: " + agntCRd
            + "\nlabel: " + InitiateClaim.toLabel( sid )
            + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          )
        )
        println(
          (
            "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
            + "\npublishing initiate claim on: " 
            + "\ncnxn: " + agntCRd
            + "\nlabel: " + InitiateClaim.toLabel( sid )
            + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          )
        )
          
        reset {
          node.publish( agntCRd )(
            InitiateClaim.toLabel( sid ),
            InitiateClaim( sid, cid, c2v, c2r, clm )
          )
        }
      }

    def simulateClaimantOpenClaimStep(
      simCtxt : SimulationContext
    ) : SimulationContext =
      {
        val SimulationContext( node, glosStub, sid, cid, c, v, r, c2v, c2r, v2r, clm ) = simCtxt
        val agntCRd = 
          acT.AgentCnxn(
            c2r.src,
            c2r.label,
            c2r.trgt
          )
        
        BasicLogService.tweet(
          (
            "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
            + "\npublishing open claim on: " 
            + "\ncnxn: " + agntCRd
            + "\nlabel: " + OpenClaim.toLabel( sid )
            + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          )
        )
        println(
          (
            "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
            + "\npublishing open claim on: " 
            + "\ncnxn: " + agntCRd
            + "\nlabel: " + OpenClaim.toLabel( sid )
            + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          )
        )
          
        reset {
          node.publish( agntCRd )(
            OpenClaim.toLabel( sid ),
            OpenClaim( sid, cid, v2r, clm )
          )
        }

        simCtxt
      }

    def simulateWaitForVerifierAllowVerificationAcknowledgment(
      simCtxt : SimulationContext
    ) : Unit = {
      val agntClmntRd =
        acT.AgentCnxn(
          simCtxt.c2v.src,
          simCtxt.c2v.label,
          simCtxt.c2v.trgt
        )

      BasicLogService.tweet(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\nwaiting for allow verification acknowledgment on: " 
          + "cnxn: " + agntClmntRd
          + "label: " + AckAllowVerification.toLabel
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )
      println(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\nwaiting for allow verification acknowledgment on: " 
          + "cnxn: " + agntClmntRd
          + "label: " + AckAllowVerification.toLabel
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )

      reset {
        for(
          eAckAllowV <- simCtxt.node.subscribe(
            agntClmntRd
          )( AckAllowVerification.toLabel() )
        ) {
          rsrc2V[VerificationMessage]( eAckAllowV ) match {
            case Left( vmsg@AckAllowVerification( sidAAV, cidAAV, rpAAV, clmAAV ) ) => { 
              BasicLogService.tweet(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nreceived allow verification acknowledgment " + eAckAllowV
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              println(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nreceived allow verification acknowledgment " + eAckAllowV
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              //continuation( vmsg )
            }
            case Right( true ) => {
              BasicLogService.tweet(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nstill waiting for verifier allow verification acknowledgment"
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              println(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nstill waiting for verifier allow verification acknowledgment"
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
            }
            case _ => {
              BasicLogService.tweet(
                "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                + "\nunexpected protocol message : " + eAckAllowV
                + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
              )
              println(
                "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                + "\nunexpected protocol message : " + eAckAllowV
                + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
              )
            }
          }
        }
      }
    }

    def simulateClaimantAllowVerificationStep(
      simCtxt : SimulationContext
    ) : SimulationContext = {
      val SimulationContext( node, glosStub, sid, cid, c, v, r, c2v, c2r, v2r, clm ) = simCtxt
      val agntVrfrRd = 
        acT.AgentCnxn( c2v.src, c2v.label, c2v.trgt )
      BasicLogService.tweet(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\npublishing allow verification on: " 
          + "\ncnxn: " + agntVrfrRd
          + "\nlabel: " + AllowVerification.toLabel( sid )
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )
      println(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\npublishing allow verification on: " 
          + "\ncnxn: " + agntVrfrRd
          + "\nlabel: " + AllowVerification.toLabel( sid )
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )
      reset {
        node.publish( agntVrfrRd )(
          AllowVerification.toLabel( sid ),
          AllowVerification( sid, cid, v2r, clm )
        )
      }
      simCtxt
    }

    def simulateRelyingPartyVerifyStep(
      simCtxt : SimulationContext
    ) : SimulationContext = {
      val SimulationContext( node, glosStub, sid, cid, c, v, r, c2v, c2r, v2r, clm ) = simCtxt
      val agntRPRd = 
        acT.AgentCnxn( v2r.src, v2r.label, v2r.trgt )
      BasicLogService.tweet(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\npublishing verify request on: " 
          + "\ncnxn: " + agntRPRd
          + "\nlabel: " + Verify.toLabel( sid )
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )
      println(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\npublishing verify request on: " 
          + "\ncnxn: " + agntRPRd
          + "\nlabel: " + Verify.toLabel( sid )
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )
      reset {
        node.publish( agntRPRd )(
          Verify.toLabel( sid ),
          Verify( sid, cid, c2r, clm )
        )
      }
      simCtxt
    }

    def simulateVerifierVerificationStep(
      simCtxt : SimulationContext
    ) : SimulationContext = {
      val SimulationContext( node, glosStub, sid, cid, c, v, r, c2v, c2r, v2r, clm ) = simCtxt
      val agntRPRd = 
        acT.AgentCnxn( v2r.src, v2r.label, v2r.trgt )

      val witness = "claimVerified( true )".toLabel

      BasicLogService.tweet(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\npublishing verification testimony on: " 
          + "\ncnxn: " + agntRPRd
          + "\nlabel: " + Verification.toLabel( sid )
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )
      println(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\npublishing verification testimony on: " 
          + "\ncnxn: " + agntRPRd
          + "\nlabel: " + Verification.toLabel( sid )
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )

      reset {
        node.publish( agntRPRd )(
          Verification.toLabel( sid ),
          Verification( sid, cid, c2r, clm, witness )
        )
      }
      simCtxt
    }

    def simulateVerifierAckAllowVerificationStep(
      simCtxt : SimulationContext
    ) : SimulationContext = {
      val SimulationContext( node, glosStub, sid, cid, c, v, r, c2v, c2r, v2r, clm ) = simCtxt
      val agntVrfrRd = 
        acT.AgentCnxn( c2v.src, c2v.label, c2v.trgt )

      BasicLogService.tweet(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\npublishing allow verification acknowledgment on: " 
          + "\ncnxn: " + agntVrfrRd
          + "\nlabel: " + AckAllowVerification.toLabel( sid )
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )
      println(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\npublishing allow verification acknowledgment on: " 
          + "\ncnxn: " + agntVrfrRd
          + "\nlabel: " + AckAllowVerification.toLabel( sid )
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )

      reset {
        node.publish( agntVrfrRd )(
          AckAllowVerification.toLabel( sid ),
          AckAllowVerification( sid, cid, c2r, clm )
        )
      }
      simCtxt
    }

    def simulateRelyingPartyCloseClaimStep(
      simCtxt : SimulationContext
    ) : SimulationContext = {
      val SimulationContext( node, glosStub, sid, cid, c, v, r, c2v, c2r, v2r, clm ) = simCtxt
      val agntRPRd = 
        acT.AgentCnxn( c2r.trgt, c2r.label, c2r.src )
      
      val witness =
        new CnxnCtxtBranch[String,String,String](
          "verified",
          clm.asInstanceOf[CnxnCtxtLabel[String,String,String] with Factual] :: Nil
        )

      BasicLogService.tweet(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\npublishing allow verification acknowledgment on: " 
          + "\ncnxn: " + agntRPRd
          + "\nlabel: " + CloseClaim.toLabel( sid )
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )
      println(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\npublishing allow verification acknowledgment on: " 
          + "\ncnxn: " + agntRPRd
          + "\nlabel: " + CloseClaim.toLabel( sid )
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )

      reset {
        node.publish( agntRPRd )(
          CloseClaim.toLabel( sid ),
          CloseClaim( sid, cid, c2v, clm, witness )
        )
      }
      simCtxt
    }

    def simulateWaitForCloseClaim(
      simCtxt : SimulationContext
    ) : SimulationContext = {
      val SimulationContext( node, glosStub, sid, cid, c, v, r, c2v, c2r, v2r, clm ) = simCtxt
      val agntClmntRd =
        acT.AgentCnxn( c2r.src, c2r.label, c2r.trgt )

      BasicLogService.tweet(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\nwaiting for close claim on: " 
          + "cnxn: " + agntClmntRd
          + "label: " + CloseClaim.toLabel( sid )
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )
      println(
        (
          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          + "\nwaiting for close claim on: " 
          + "cnxn: " + agntClmntRd
          + "label: " + CloseClaim.toLabel( sid )
          + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
        )
      )

      reset {
        for(
          eCloseClaim <- simCtxt.node.subscribe(
            agntClmntRd
          )( CloseClaim.toLabel( sid ) )
        ) {
          rsrc2V[VerificationMessage]( eCloseClaim ) match {
            case Left( vmsg@CloseClaim( sidCC, cidCC, vrfrCC, clmCC, witCC ) ) => { 
              BasicLogService.tweet(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nreceived close claim " + eCloseClaim
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              println(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nreceived close claim " + eCloseClaim
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              //continuation( vmsg )
            }
            case Right( true ) => {
              BasicLogService.tweet(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nstill waiting for close claim"
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
              println(
                (
                  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  + "\nstill waiting for close claim"
                  + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                )
              )
            }
            case _ => {
              BasicLogService.tweet(
                "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                + "\nunexpected protocol message : " + eCloseClaim
                + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
              )
              println(
                "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                + "\nunexpected protocol message : " + eCloseClaim
                + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
              )
            }
          }
        }
      }

      simCtxt
    }
  }

  object VerificationDriver
   extends VerificationDriverT
     with NodeStreams
     with FuzzyStreams
     with FuzzyTerms
     with FuzzyTermStreams
     with CnxnString[String,String,String] 
     with FJTaskRunnersX
     with Serializable
  {    
    def nextClaimant() : ClaimantBehavior = ClaimantBehavior()
    def nextVerifier() : VerifierBehavior = VerifierBehavior()
    def nextRelyingParty() : RelyingPartyBehavior = RelyingPartyBehavior()
    def claimantStrm() : Stream[ClaimantBehavior] = {
      tStream( nextClaimant() )( { clmnt => nextClaimant() } )
    }
    def verifierStrm() : Stream[VerifierBehavior] = {
      tStream( nextVerifier() )( { vrfr => nextVerifier() } )
    }
    def relyingPartyStrm() : Stream[RelyingPartyBehavior] = {
      tStream( nextRelyingParty() )( { rp => nextRelyingParty() } )
    }    
    
    def connect(
      selfCnxn1 : PortableAgentCnxn,
      selfCnxn2 : PortableAgentCnxn
    )(
      useSrc : Boolean = true
    ): PortableAgentCnxn = {
      val cnxnLabel = UUID.randomUUID().toString.split( "-" )( 0 )
      if ( useSrc ) {
        PortableAgentCnxn( selfCnxn1.src, cnxnLabel, selfCnxn2.src )
      } 
      else {
        PortableAgentCnxn( selfCnxn1.trgt, cnxnLabel, selfCnxn2.trgt )
      }
    }
    
    def getSelfCnxnStream(
    ) : Stream[PortableAgentCnxn] = {
      val lblStrm =
        for(
          s <- mkRandomLabelStringStream( "l", 0 )
          if !(
            s.contains( "\"" ) 
            || s.contains( "true" )
            || s.contains( "false" )
          )
        ) yield {
          s.split( "-" )( 0 )
        }
      mkSelfCnxnStream(
        new scala.util.Random(),
        lblStrm
      )
    }

    def getSelfCnxnStreamStream(
    ) : Stream[Stream[PortableAgentCnxn]] = {
      tStream[Stream[PortableAgentCnxn]](
        getSelfCnxnStream()
      )(
        ( seed : Stream[PortableAgentCnxn] ) => {
          getSelfCnxnStream()
        }
      )
    }

    def verificationEnsembleCnxnStrm(
    ) : Stream[(PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn)] = {
      val ( cCnxnStrm, vCnxnStrm, rCnxnStrm ) =
        ( getSelfCnxnStream(), getSelfCnxnStream(), getSelfCnxnStream() );
      for(
        ( c, ( v, r ) ) <- cCnxnStrm.zip( vCnxnStrm.zip( rCnxnStrm ) )
      ) yield {
        ( c, v, r, connect( c, v )( true ), connect( c, r )( true ), connect( v, r )( true ) )
      }        
    }
    def verificationEnsembleStrm(
    ) : Stream[(ClaimantBehavior,VerifierBehavior,RelyingPartyBehavior)] = {
      for(
        ( c, ( v, r ) ) <- claimantStrm().zip( verifierStrm().zip( relyingPartyStrm() ) )
      ) yield {
        ( c, v, r )
      }
    }

    trait BehaviorTestStreamT[Behavior <: ProtocolBehaviorT] {
      def behaviorStream : Stream[Behavior]
      def behaviorToGLoSIndex : Int
      def behaviorRunArgsFn : SimulationContext => ( List[PortableAgentCnxn], List[CnxnCtxtLabel[String,String,String]] )

      val _behaviorToGLoS : HashMap[Int,Stream[List[PortableAgentCnxn]]] = 
        new HashMap[Int,Stream[List[PortableAgentCnxn]]]()

      def behaviorToGLoS(
        numberOfCnxns : Int = 3
      ) : Stream[List[PortableAgentCnxn]] = {
        _behaviorToGLoS.get( numberOfCnxns ) match {
          case None => {
            val strm = 
              for(
                strm <- getSelfCnxnStreamStream;
                cstrm = strm.take( numberOfCnxns )
              ) yield {
                cstrm.toList
              }
            _behaviorToGLoS += ( numberOfCnxns -> strm )
            strm 
          }
          case Some( strm ) => strm 
        }
      }

      def behaviorTestStrm(
        node : StdEvalChannel,
        glosStub : GLoSStub,
        bhvr : Behavior
      )(
        cnxnStrm : Stream[(PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn)]
      ): Stream[() => SimulationContext] = {              
        for(
          ( c, v, r, c2v, c2r, v2r ) <- cnxnStrm
        ) yield {
          val agntCRd = 
            acT.AgentCnxn(
              glosStub.claimantToGLoS.src,
              glosStub.claimantToGLoS.label,
              glosStub.claimantToGLoS.trgt
            )
          
          val sid = UUID.randomUUID.toString
          val cid = UUID.randomUUID.toString
          
          () => {          
            glosStub.waitForSignalToInitiateClaim(
              ( claim : CnxnCtxtLabel[String,String,String] ) => {
                val simCtxt =
                  SimulationContext( node, glosStub, sid, cid, c, v, r, c2v, c2r, v2r, claim )
                val ( cnxns, filters ) = behaviorRunArgsFn( simCtxt )

                BasicLogService.tweet(
                  (
                    "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                    + "\ninvoking run on " + bhvr
                    + "\nnode: " + node
                    + "\ncnxns: " + cnxns
                    + "\nfilters: " + filters
                    + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  )
                )
                println(
                  (
                    "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                    + "\ninvoking run on " + bhvr
                    + "\nnode: " + node
                    + "\ncnxns: " + cnxns
                    + "\nfilters: " + filters
                    + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                  )
                )
                
                spawn { bhvr.run( node, cnxns, filters ) }

                simCtxt
              }
            )          
          }
        }
      }
      
      def behaviorTestStrm(
        node : StdEvalChannel,
        bhvr : Behavior
      )(
        cnxnStrm : Stream[(PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn)]
      ): Stream[() => SimulationContext] = {                     
        behaviorToGLoS().flatMap(
          {
            trpl => {
              trpl match {
                case c :: v :: r :: Nil => {
                  behaviorTestStrm(
                    node,
                    GLoSStub( c, v, r ),
                    bhvr
                  )(
                    cnxnStrm
                  )
                }
                case _ => {
                  throw new Exception( "unexpected cnxn stream structure: " + trpl )
                }
              }              
            }
          }
        )
      }
      
      def behaviorTestStrm(
        bhvr : Behavior
      )(
        cnxnStrm : Stream[(PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn)]
      ): Stream[() => SimulationContext] = {      
        val ndStrm = dslNodeStream
        ndStrm.flatMap(
          {
            node => {
              behaviorTestStrm(
                node, bhvr
              )( cnxnStrm )
            }
          }
        )
      }
      
      def behaviorTestStrm(
        cnxnStrm : Stream[(PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn,PortableAgentCnxn)]
      ): Stream[() => SimulationContext] = {      
        val cStrm = behaviorStream
        cStrm.flatMap(
          {
            clmnt => {
              behaviorTestStrm(
                clmnt
              )(
                cnxnStrm
              )
            }
          }
        )
      }
      
      def behaviorTestStrm(
      ) : Stream[() => SimulationContext] = {      
        behaviorTestStrm(
          verificationEnsembleCnxnStrm()
        )
      }
    }

    case class ClaimantTestStream( 
      override val behaviorToGLoSIndex : Int = 1,
      override val behaviorStream : Stream[ClaimantBehavior] = claimantStrm,
      override val behaviorRunArgsFn : SimulationContext => ( List[PortableAgentCnxn], List[CnxnCtxtLabel[String,String,String]] ) =
        RunParameters.claimantRunArgs
    ) extends BehaviorTestStreamT[ClaimantBehavior]

    case class VerifierTestStream( 
      override val behaviorToGLoSIndex : Int = 2,
      override val behaviorStream : Stream[VerifierBehavior] = verifierStrm,
      override val behaviorRunArgsFn : SimulationContext => ( List[PortableAgentCnxn], List[CnxnCtxtLabel[String,String,String]] ) =
        RunParameters.verifierRunArgs
    ) extends BehaviorTestStreamT[VerifierBehavior]

    case class RelyingPartyTestStream( 
      override val behaviorToGLoSIndex : Int = 3,
      override val behaviorStream : Stream[RelyingPartyBehavior] = relyingPartyStrm,
      override val behaviorRunArgsFn : SimulationContext => ( List[PortableAgentCnxn], List[CnxnCtxtLabel[String,String,String]] ) =
        RunParameters.relyingPartyRunArgs
    ) extends BehaviorTestStreamT[RelyingPartyBehavior]

    def verificationProtocolTestStream(
    ) : Stream[() => SimulationContext] = {
      val veCnxnStrm =
        verificationEnsembleCnxnStrm()

      val ndStrm = dslNodeStream

      val cts = ClaimantTestStream()
      val vts = VerifierTestStream()
      val rts = RelyingPartyTestStream()

      val ctsBhvrStrm = cts.behaviorStream
      val vtsBhvrStrm = vts.behaviorStream
      val rtsBhvrStrm = rts.behaviorStream

      val bhvrTplStrm = 
        for(
          ( clmntBhvr, ( vrfrBhvr, rpBhvr ) ) <- ctsBhvrStrm.zip( vtsBhvrStrm.zip( rtsBhvrStrm ) )
        ) yield {
          ( clmntBhvr, vrfrBhvr, rpBhvr )
        }

      for(
        ( cnxns, ( node, bhvrs ) ) <- veCnxnStrm.zip( ndStrm.zip( bhvrTplStrm ) )
      ) yield {
        () => {
          val ( c, v, r, c2v, c2r, v2r ) = cnxns
          val ( clmntBhvr, vrfrBhvr, rpBhvr ) = bhvrs
          val ( sid, cid ) = ( UUID.randomUUID.toString, UUID.randomUUID.toString );
          val gs = GLoSStub( c, v, r )
          gs.waitForSignalToInitiateClaim(
            {
              claim => {
                val simCtxt =
                  SimulationContext(
                    node, 
                    gs,
                    sid,
                    cid,
                    c, v, r, c2v, c2r, v2r,
                    claim
                  )

                val ctrlBhvrs : List[( BehaviorTestStreamT[_], ProtocolBehaviorT )] =
                  List[( BehaviorTestStreamT[_], ProtocolBehaviorT )](
                    ( cts, clmntBhvr ),
                    ( vts, vrfrBhvr ),
                    ( rts, rpBhvr )
                  )

                for( ( ctrl, bhvr ) <- ctrlBhvrs ) {
                  val ( cnxns, filters ) = ctrl.behaviorRunArgsFn( simCtxt )

                  BasicLogService.tweet(
                    (
                      "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                      + "\ntest protocol -- invoking run on " + bhvr
                      + "\nnode: " + node
                      + "\ncnxns: " + cnxns
                      + "\nfilters: " + filters
                      + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                    )
                  )
                  println(
                    (
                      "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                      + "\ntest protocol -- invoking run on " + bhvr
                      + "\nnode: " + node
                      + "\ncnxns: " + cnxns
                      + "\nfilters: " + filters
                      + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
                    )
                  )

                  spawn { bhvr.run( node, cnxns, filters ) }
                }

                simCtxt
              }
            }
          )
        }
      }
    }
    
    def testProtocol( 
      numOfTests : Int = 1
    ) : Unit = {
      val testStrm = verificationProtocolTestStream.take( numOfTests )
      for( i <- 1 to numOfTests ) {
        BasicLogService.tweet(
          (
            "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
            + "\ntest protocol -- running test number " + i + " out of " + numOfTests + " tests"
            + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          )
        )
        println(
          (
            "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
            + "\ntest protocol -- running test number " + i + " out of " + numOfTests + " tests"
            + "\n||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||"
          )
        )

        val test = testStrm( i - 1 )
        val simCtxt1 = test()
        val gs = simCtxt1.glosStub

        VerificationDriver.simulateInitiateClaimStep( simCtxt1 )
      }
    }
  }  
  
  // trait SinglePartyDriverT {
//     def drive(
//       numOfTests : Int = 1,
//       promptBeforeTakingNextStep : Boolean = false,
//       sleepBeforePrompt : Boolean = true
//     ) : Unit
//   }

//   object ClaimantDriver extends SinglePartyDriverT with Serializable {
//     def drive(
//       numOfTests : Int = 1,
//       promptBeforeTakingNextStep : Boolean = false,
//       sleepBeforePrompt : Boolean = true
//     ) = {
//       val cts = VerificationDriver.ClaimantTestStream()
//       val tStrm = cts.behaviorTestStrm
//       val ctStrm = tStrm.take( numOfTests )
//       for( i <- ( 1 to numOfTests ) ) {
//         val t = ctStrm( i - 1 )
//         val simCtxt1 = t()
//         val gs = simCtxt1.glosStub
        
//         VerificationDriver.simulateInitiateClaimStep( simCtxt1 )

//         if ( promptBeforeTakingNextStep ) {
//           if ( sleepBeforePrompt ) { Thread.sleep( 2500 ) }
//           println( "Proceed to next step? " )
//           val ln = readLine() // Note: this is blocking.
//           if ( ln.contains( "y" ) ) {
//             println( "simulating acknowledgment of allow verification request" )

//             val simCtxt2 =
//               VerificationDriver.simulateVerifierAckAllowVerificationStep( simCtxt1 )

//             if ( sleepBeforePrompt ) { Thread.sleep( 2500 ) }
//             println( "Proceed to next step? " )
//             val ln = readLine() // Note: this is blocking.
//             if ( ln.contains( "y" ) ) {
//               println( "simulating close claim" )
//               VerificationDriver.simulateRelyingPartyCloseClaimStep( simCtxt2 )
              
//               if ( sleepBeforePrompt ) { Thread.sleep( 2500 ) }
//               println( "Proceed to next step? " )
//               val ln = readLine() // Note: this is blocking.
//               if ( ln.contains( "y" ) ) {
//                 println( "simulating complete claim" )
//                 gs.waitForCompleteClaim( simCtxt2.node, { vmsg => println( "Done." ) } )
//               }
//             }
//           }
//         }
//         else {
//           val simCtxt2 =
//             VerificationDriver.simulateVerifierAckAllowVerificationStep( simCtxt1 )
//           VerificationDriver.simulateRelyingPartyCloseClaimStep( simCtxt2 )
//           gs.waitForCompleteClaim( simCtxt2.node, { vmsg => println( "Done." ) } )
//         }
//       }
//     }
//   }

//   object VerifierDriver extends SinglePartyDriverT with Serializable {
//     def drive(
//       numOfTests : Int = 1,
//       promptBeforeTakingNextStep : Boolean = false,
//       sleepBeforePrompt : Boolean = true
//     ) = {
//       val cts = VerificationDriver.VerifierTestStream()
//       val tStrm = cts.behaviorTestStrm
//       val ctStrm = tStrm.take( numOfTests )
//       for( i <- ( 1 to numOfTests ) ) {
//         val t = ctStrm( i - 1 )
//         val simCtxt1 = t()
//         val gs = simCtxt1.glosStub
        
//         if ( promptBeforeTakingNextStep ) {
//           if ( sleepBeforePrompt ) { Thread.sleep( 2500 ) }
//           println( "Proceed to next step? " )
//           val ln = readLine() // Note: this is blocking.
//           if ( ln.contains( "y" ) ) {            
//             // Now run the test
//             val simCtxt2 =
//               VerificationDriver.simulateClaimantAllowVerificationStep(
//                 simCtxt1
//               )
//             VerificationDriver.simulateWaitForVerifierAllowVerificationAcknowledgment(
//               simCtxt2
//             )
//             if ( sleepBeforePrompt ) { Thread.sleep( 2500 ) }
//             println( "Proceed to next step? " )
//             val ln = readLine() // Note: this is blocking.
//             if ( ln.contains( "y" ) ) {            
//               // Now run the test
//               val simCtxt3 =
//                 VerificationDriver.simulateRelyingPartyVerifyStep(
//                   simCtxt2
//                 )
//               if ( sleepBeforePrompt ) { Thread.sleep( 2500 ) }
//               println( "Proceed to next step? " )
//               val ln = readLine() // Note: this is blocking.
//               if ( ln.contains( "y" ) ) {            
//                 gs.waitForVerifierVerificationNotification(
//                   simCtxt3.node,
//                   { vmsg => println( "witnessed claim: " + vmsg ) }
//                 )
//                 // This is not part of the immediate environment of
//                 // the verifier.
//                 // gs.waitForRelyingPartyVerificationNotification(
//                 //    simCtxt3.node,
//                 //    { vmsg => println( "witnessed verification of claim: " + vmsg ) }
//                 // )
//               }
//             }
//           }
//         }
//         else {
//           // Just run the test
//           val simCtxt2 =
//             VerificationDriver.simulateClaimantAllowVerificationStep(
//               simCtxt1
//             )
//           VerificationDriver.simulateWaitForVerifierAllowVerificationAcknowledgment(
//             simCtxt2
//           )            
//           val simCtxt3 =
//             VerificationDriver.simulateRelyingPartyVerifyStep(
//               simCtxt2
//             )          
//           gs.waitForVerifierVerificationNotification(
//             simCtxt3.node,
//             { vmsg => println( "witnessed claim: " + vmsg ) }
//           )
//           // This is not part of the immediate environment of
//           // the verifier.
//           // gs.waitForRelyingPartyVerificationNotification(
//           //    simCtxt3.node,
//           //    { vmsg => println( "witnessed verification of claim: " + vmsg ) }
//           // )
//         }
//       }
//     }
//   }
  
//   object RelyingPartyDriver extends SinglePartyDriverT with Serializable {
//     def drive(
//       numOfTests : Int = 1,
//       promptBeforeTakingNextStep : Boolean = false,
//       sleepBeforePrompt : Boolean = true
//     ) = {
//       val cts = VerificationDriver.RelyingPartyTestStream()
//       val tStrm = cts.behaviorTestStrm
//       val ctStrm = tStrm.take( numOfTests )
//       for( i <- ( 1 to numOfTests ) ) {
//         val t = ctStrm( i - 1 )
//         val simCtxt1 = t()
//         val gs = simCtxt1.glosStub
        
//         if ( promptBeforeTakingNextStep ) {
//           if ( sleepBeforePrompt ) { Thread.sleep( 2500 ) }
//           println( "Proceed to next step? " )
//           val ln = readLine() // Note: this is blocking.
//           if ( ln.contains( "y" ) ) {            
//             // Now run the test
//             val simCtxt2 =
//               VerificationDriver.simulateClaimantOpenClaimStep(
//                 simCtxt1
//               )
//             if ( sleepBeforePrompt ) { Thread.sleep( 2500 ) }
//             println( "Proceed to next step? " )
//             val ln = readLine() // Note: this is blocking.
//             if ( ln.contains( "y" ) ) {            
//               // Now run the test
//               val simCtxt3 =
//                 VerificationDriver.simulateVerifierVerificationStep(
//                   simCtxt2
//                 )              
//               if ( sleepBeforePrompt ) { Thread.sleep( 2500 ) }
//               println( "Proceed to next step? " )
//               val ln = readLine() // Note: this is blocking.
//               if ( ln.contains( "y" ) ) {            
//                 val simCtxt4 =
//                   VerificationDriver.simulateWaitForCloseClaim(
//                     simCtxt3
//                   )
//                 simCtxt4.glosStub.waitForRelyingPartyVerificationNotification(
//                   simCtxt4.node,
//                   { vmsg => println( "witnessed verification of claim: " + vmsg ) }
//                 )
//                 simCtxt4
//               }
//             }
//           }
//         }
//         else {
//           // Just run the test
//           val simCtxt2 =
//             VerificationDriver.simulateClaimantOpenClaimStep(
//               simCtxt1
//             )
//           val simCtxt3 =
//             VerificationDriver.simulateVerifierVerificationStep(
//               simCtxt2
//             )          
//           val simCtxt4 =
//             VerificationDriver.simulateWaitForCloseClaim(
//               simCtxt3
//             )
//           simCtxt4.glosStub.waitForRelyingPartyVerificationNotification(
//             simCtxt4.node,
//             { vmsg => println( "witnessed verification of claim: " + vmsg ) }
//           )
//           simCtxt4
//         }
//       }
//     }
//   }
}
