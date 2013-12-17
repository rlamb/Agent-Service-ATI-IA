package com.protegra_ati.agentservices.protocols

import com.biosimilarity.evaluator.distribution.FuzzyStreams

object NodeSupply extends FuzzyStreams with Serializable {
  import com.biosimilarity.evaluator.distribution.diesel._
  import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
  import java.util.UUID

  def configFileStream() : Stream[Option[String]] = {
    tStream[Int]( 1 )( _ + 1 ).map( { i => Some( "eval" + i + ".conf" ) } )
  }

  def dataLocationStream() : Stream[String] = {
    tStream[Int]( 1 )( _ + 1 ).map( { i => "/dieselProtocol" + i } )
  }

  def configuredNodeStream(
  ) : Stream[Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse]] = {
    val configFn : ( String, Option[String] ) => Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse] = 
      { ( dataLoc, cnfgOpt ) => DieselEngineCtor.dslEvaluatorAgent( dataLoc, cnfgOpt ) }
    dataLocationStream.zip( configFileStream ).map( { tpl => configFn( tpl._1, tpl._2 ) } )
  }

  def nodeStream(
  ) : Stream[Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse]] = {
    tStream[Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse]](
      DieselEngineCtor.dslEvaluatorAgent( )
    )(
      {
        seed => DieselEngineCtor.dslEvaluatorAgent( )
      }
    )
  }

  def keyNodeStream(
  ) : Stream[( UUID, Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse] )] = {
    uuidStream().zip( nodeStream() )
  }

  def configuredKeyNodeStream(
  ) : Stream[( UUID, Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse] )] = {
    uuidStream().zip( configuredNodeStream() )
  }

  def mkNodeSupply( initialCut : Int = 10 )(
    useConfigOpts : Boolean = false
  ): Unit = {
    if ( useConfigOpts ) {
      for( ( uuid, node ) <- configuredKeyNodeStream().take( initialCut ) ) {
        EvalNodeMapper += ( uuid.toString -> node )
      }
    } 
    else {
      for( ( uuid, node ) <- keyNodeStream().take( initialCut ) ) {
        EvalNodeMapper += ( uuid.toString -> node )
      }
    }
  }
}

object TestRace extends Serializable {
  import com.biosimilarity.evaluator.distribution.ConcreteHL.PostedExpr
  import com.biosimilarity.evaluator.distribution.diesel._
  import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
  import com.biosimilarity.lift.model.store.CnxnCtxtLabel
  import com.protegra_ati.agentservices.protocols.msgs._
  import com.protegra_ati.agentservices.store.extensions.StringExtensions._
  import java.util.UUID
  import java.net.URI
  import scala.concurrent.ops._
  import scala.util.continuations._

  private def dispatchMessage( e : Option[mTT.Resource] ) : Int = {
    e match {
      case Some( mTT.RBoundHM( Some( mTT.Ground( PostedExpr( _ ) ) ), _ ) ) => 1
      case None => 2
      case _ => 3
    }
  }

  def testInitiatorPrefix(
    nI2A : Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse],
    nA2I : Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse],
    I2A : acT.AgentCnxn,
    A2I : acT.AgentCnxn
  )(
    request : Any,
    requestLabel : CnxnCtxtLabel[String, String, String],
    responseLabel : CnxnCtxtLabel[String, String, String]
  ) : Unit = {
    println(
      (
        "Send request" 
        + "\nnode: " + nI2A 
        + "\ncnxn: " + I2A
        + "\nlabel: " + requestLabel
        + "\nrequest: " + request
      )
    )
    reset { nI2A.put( I2A )( requestLabel, mTT.Ground( PostedExpr( request ) ) ) }

    println( "Getting response label: " + responseLabel )
    println(
      (
        "Waiting for response: (1)" 
        + "\nnode: " + nA2I
        + "\ncnxn: " + A2I
        + "\nlabel: " + responseLabel
      )
    )
    reset {
      for( e <- nA2I.get( A2I )( responseLabel ) ) {
        println( "(1) Got Response: " + e )
      }
    }
  }

  def testRecipientPrefix(
    nI2A : Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse],
    nA2I : Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse],
    I2A : acT.AgentCnxn,
    A2I : acT.AgentCnxn
  )(
    requestLabel : CnxnCtxtLabel[String, String, String],
    response : Any,
    responseLabel : CnxnCtxtLabel[String, String, String]
  )(
    matchingLevel : Int
  ) : Unit = {
    println(
      (
        "Waiting for request: (2)" 
        + "\nnode: " + nI2A
        + "\ncnxn: " + I2A
        + "\nlabel: " + requestLabel
      )
    )
    reset {      
      for( e <- nI2A.get( I2A )( requestLabel ) ) {
        println( "(2) Got Request: " + e )

        if ( matchingLevel == 0 ) {
          reset { nA2I.put( A2I )( responseLabel, mTT.Ground( PostedExpr( response ) ) ) }
        } else if ( matchingLevel == 1 ) {
          e match {
            case _ =>
              reset { nA2I.put( A2I )( responseLabel, mTT.Ground( PostedExpr( response ) ) ) }
          }
        } else if ( matchingLevel == 2 ) {
          e match {
            case Some( _ ) =>
              reset { nA2I.put( A2I )( responseLabel, mTT.Ground( PostedExpr( response ) ) ) }
            case _ => ()
          }
        } else if ( matchingLevel == 3 ) {
          e match {
            case Some( mTT.RBoundHM( Some( mTT.Ground( PostedExpr( _ ) ) ), _ ) ) =>
              reset { nA2I.put( A2I )( responseLabel, mTT.Ground( PostedExpr( response ) ) ) }
            case _ => ()
          }
        } else if ( matchingLevel == 4 ) {
          val result = dispatchMessage( e )
          println( "Result: " + result )
          if ( result == 1 ) {
            reset { nA2I.put( A2I )( responseLabel, mTT.Ground( PostedExpr( response ) ) ) }
          }
        } else if ( matchingLevel == 5) {
          if ( e != None ) {
            reset { nA2I.put( A2I )( responseLabel, mTT.Ground( PostedExpr( response ) ) ) }
          }
        }
      }
    }
  }

  def testRecipientPrefix2(
    nI2A : Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse],
    nA2I : Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse],
    I2A : acT.AgentCnxn,
    A2I : acT.AgentCnxn
  )(
    requestLabel : CnxnCtxtLabel[String, String, String],
    response : Any,
    responseLabel : CnxnCtxtLabel[String, String, String]
  )(
    matchingLevel : Int
  ) : Unit = {
    println( "Getting request label: " + requestLabel )
    reset {
      for( e <- nI2A.get( I2A )( requestLabel ) ) {
        println( "Got Request: " + e )

        val result = dispatchMessage( e )
        println( "Result: " + result )
        if ( result == 1 ) {
          println( "Send response label: " + responseLabel )
          reset { nA2I.put( A2I )( responseLabel, mTT.Ground( PostedExpr( response ) ) ) }
        }
      }
    }
  }

  def testRecipientPrefix3(
    nI2A : Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse],
    nA2I : Being.AgentKVDBNode[PersistedKVDBNodeRequest, PersistedKVDBNodeResponse],
    I2A : acT.AgentCnxn,
    A2I : acT.AgentCnxn
  )(
    requestLabel : CnxnCtxtLabel[String, String, String],
    response : Any,
    responseLabel : CnxnCtxtLabel[String, String, String]
  ) : Unit = {
    println(
      (
        "Waiting for request: (2)" 
        + "\nnode: " + nI2A
        + "\ncnxn: " + I2A
        + "\nlabel: " + requestLabel
      )
    )
    reset {
      for( e <- nI2A.get( I2A )( requestLabel ) ) {
        println( "(2) Got Request: " + e )

        e match {
          case Some( mTT.RBoundHM( Some( mTT.Ground( PostedExpr( _ ) ) ), _ ) ) => {
            println(
              (
                "Send response" 
                + "\nnode: " + nA2I
                + "\ncnxn: " + A2I
                + "\nlabel: " + responseLabel
                + "\nresponse: " + response
              )
            )
            reset { nA2I.put( A2I )( responseLabel, mTT.Ground( PostedExpr( response ) ) ) }
          }
          case Some( mTT.Ground( PostedExpr( _ ) ) ) => {
            println(
              (
                "Send response" 
                + "\nnode: " + nA2I
                + "\ncnxn: " + A2I
                + "\nlabel: " + responseLabel
                + "\nresponse: " + response
              )
            )
            reset { nA2I.put( A2I )( responseLabel, mTT.Ground( PostedExpr( response ) ) ) }
          }
          case None => {
            println( "waiting for request label" )
          }
        }
      }
    }
  }

  def race(
    singleNode : Boolean,
    waitingTime : Int,
    protocolMessage : Boolean,
    matchingLevel : Int
  ) : Unit = {
    NodeSupply.mkNodeSupply( 2 )()
    val nodeKeys = EvalNodeMapper.keys.toList
    val nI2A = EvalNodeMapper( nodeKeys( 0 ) )
    val nA2I = if ( singleNode ) nI2A else EvalNodeMapper( nodeKeys( 1 ) )

    val I2A = acT.AgentCnxn( new URI( "a" ), "", new URI( "b" ) )
    val A2I = acT.AgentCnxn( new URI( "b" ), "", new URI( "a" ) )

    def runTest(
      request : Any,
      requestLabel : CnxnCtxtLabel[String, String, String],
      response : Any,
      responseLabel : CnxnCtxtLabel[String, String, String]
    ) {
      spawn {
        testRecipientPrefix2( nI2A, nA2I, I2A, A2I )( requestLabel, response, responseLabel )( matchingLevel )
      }

      Thread.sleep( waitingTime )

      spawn {
        testInitiatorPrefix( nI2A, nA2I, I2A, A2I )( request, requestLabel, responseLabel )
      }
    }

    if ( protocolMessage ) {
      val sessionId = UUID.randomUUID().toString
      val correlationId = UUID.randomUUID().toString

      val request = GetIntroductionProfileRequest( sessionId, correlationId, null )
      val requestLabel = request.toLabel
      val response = GetIntroductionProfileResponse( sessionId, correlationId, "profileData" )
      val responseLabel = response.toLabel

      runTest( request, requestLabel, response, responseLabel )
    } else {
      val request = "request-" + UUID.randomUUID().toString
      val requestLabel = "protocolMessage(getIntroductionProfileRequest(sessionId(true)))".toLabel
      val response = "response-" + UUID.randomUUID().toString
      val responseLabel = "protocolMessage(getIntroductionProfileResponse(sessionId(true),correlationId(true)))".toLabel

      runTest( request, requestLabel, response, responseLabel )
    }
  }
  def doRace(
    singleNode : Boolean,
    waitingTime : Int,
    protocolMessage : Boolean,
    useConfigOpts : Boolean = false
  ) : Unit = {
    NodeSupply.mkNodeSupply( 2 )( true )
    val nodeKeys = EvalNodeMapper.keys.toList
    val nI2A = EvalNodeMapper( nodeKeys( 0 ) )
    val nA2I = if ( singleNode ) nI2A else EvalNodeMapper( nodeKeys( 1 ) )

    val CnxnLabel = UUID.randomUUID.toString.split( "-" )( 0 )
    val I2A = acT.AgentCnxn( new URI( "a" ), CnxnLabel, new URI( "b" ) )
    val A2I = acT.AgentCnxn( new URI( "b" ), CnxnLabel, new URI( "a" ) )

    def runTest(
      request : Any,
      requestLabel : CnxnCtxtLabel[String, String, String],
      response : Any,
      responseLabel : CnxnCtxtLabel[String, String, String]
    ) {
      spawn {
        testRecipientPrefix3( nI2A, nA2I, I2A, A2I )( requestLabel, response, responseLabel )
      }

      Thread.sleep( waitingTime )

      spawn {
        testInitiatorPrefix( nI2A, nA2I, I2A, A2I )( request, requestLabel, responseLabel )
      }
    }

    val sessionId = UUID.randomUUID().toString
    val correlationId = UUID.randomUUID().toString
    
    val request = GetIntroductionProfileRequest( sessionId, correlationId, null )
    val requestLabel = request.toLabel
    val response = GetIntroductionProfileResponse( sessionId, correlationId, "profileData" )
    val responseLabel = response.toLabel
    
    runTest( request, requestLabel, response, responseLabel )
  }
}
