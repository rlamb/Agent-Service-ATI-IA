package com.protegra_ati.agentservices.core.messages.invitation

import com.protegra_ati.agentservices.core.platformagents._
import com.protegra_ati.agentservices.core.messages._
import com.protegra_ati.agentservices.core.schema._
import com.protegra.agentservicesstore.AgentTS.acT._
import com.protegra.agentservicesstore.util._
import com.rits.cloning.Cloner;

trait InvitationResponseSet
{
  self: AgentHostStorePlatformAgent =>

  def listenPublicInvitationConsumerResponses(cnxn: AgentCnxn) =
  {
    listen(_publicQ, cnxn, Channel.Invitation, Some(ChannelRole.Consumer), ChannelType.Response, ChannelLevel.Public, sendPrivate(_: AgentCnxn, _: Message))
  }

  //mostly this is single listens on single but ReferralResponses will come in here
  def listenPublicInvitationCreatorResponses(cnxn: AgentCnxn) =
  {
    listen(_publicQ, cnxn, Channel.Invitation, Some(ChannelRole.Creator), ChannelType.Response, ChannelLevel.Public, handlePublicInvitationCreatorResponseChannel(_: AgentCnxn, _: Message))
  }


  protected def handlePublicInvitationCreatorResponseChannel(cnxn: AgentCnxn, msg: Message) =
  {
    //these are request coming on the public channel (from us or other PAs)
    //if we get in this handler, it means the message was meant for us and we should process it
    report("entering handlePublicInvitationCreatorResponseChannel in ConnectionBroker", Severity.Trace)

    msg match {

      case x: ReferralResponse => {
        processReferralResponse(cnxn, x)
      }

      case _ => report("***********************not doing anything in handlePublicInvitationCreatorResponseChannel", Severity.Error)
    }
    report("exiting handlePublicInvitationCreatorResponseChannel in ConnectionBroker", Severity.Trace)
  }

  protected def processReferralResponse(cnxnBroker_Broker: AgentCnxn, referralResponse: ReferralResponse): Unit =
  {
    val query = new Connection()
    fetchList[ Connection ](_dbQ, cnxnBroker_Broker, query.toSearchKey, findConnections(_: AgentCnxn, _: List[ Connection ], referralResponse))
  }

  /**
   * Finds 2 connections: Broker-Target and Broker-Source to send invite requests if ReferralResponse was successful approved and accepted
   * @param cnxnBroker_Broker
   * @param connsBroker
   * @param referralResponse
   */
  protected def findConnections(cnxnBroker_Broker: AgentCnxn, connsBroker: List[ Connection ], referralResponse: ReferralResponse) =
  {
    val createInviteRequest = referralResponse.source
    val refereePostToTarget = referralResponse.postToTarget
    val refereePostToSource = referralResponse.postToSource
    val cnxnBroker_A = referralResponse.source.originCnxn

    val findConnBroker_A = connsBroker.filter(x => x.writeCnxn == cnxnBroker_A)
    findConnBroker_A.headOption match {
      case None => report("cannot find connBroker_A", Severity.Error)
      case Some(connBroker_A) => {
        val findConnBroker_B = connsBroker.filter(x => x.id == createInviteRequest.invitationConnectionId.toString)
        findConnBroker_B.headOption match {
          case None => report("cannot find connBroker_B", Severity.Error)
          case Some(connBroker_B) => {
            if ( referralResponse.accept ) {
              // accepted requests to be forwarded to the
              generateInvitationRequests(createInviteRequest, connBroker_A, connBroker_B, refereePostToTarget, refereePostToSource)
            } else {
              generateRejectToInvitationRequest(createInviteRequest.eventKey, connBroker_A, refereePostToSource)
            }
            //mark the original ReferralRequest as archived
            //TODO: find a better spot that's more transactional
            findReferralRequestToArchive(cnxnBroker_Broker, referralResponse)
          }
        }
      }
    }
  }

  // search for ALL persisted Requests instead of just specified one ????
  protected def findReferralRequestToArchive(cnxnBroker_Broker: AgentCnxn, referralResponse: ReferralResponse) =
  {
    //TODO: fix toSearchKey to work with the nested id, for now pull back everything
    val query = new PersistedMessage[ ReferralRequest ]()
    fetchList[ PersistedMessage[ ReferralRequest ] ](_dbQ, cnxnBroker_Broker, query.toSearchKey, archivePersistedMessage(_: AgentCnxn, _: List[ PersistedMessage[ ReferralRequest ] ], referralResponse.ids.parentId, referralResponse.accept))
  }

  //TODO:refactor to common spot
  def archivePersistedMessage(cnxnBroker_Broker: AgentCnxn, messages: List[ PersistedMessage[ _ <: Message ] ], parentId: String, isAccepted: Boolean) =
  {
    //TODO: fix toSearchKey to work with the nested id, once fixed just send a SetContentRequest to self
    for ( msg <- messages ) {
      if ( msg.message.ids.id == parentId ) {
        val newData = new Cloner().deepClone(msg)
        newData.archive()
        if ( !isAccepted )
          newData.reject()
        //TODO: something is not right, its not safedeleting the old, getting 2 copies!
        updateData(cnxnBroker_Broker, newData, msg)
      }
    }
  }


}