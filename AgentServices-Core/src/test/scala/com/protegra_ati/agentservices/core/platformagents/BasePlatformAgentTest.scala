/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/

package com.protegra_ati.agentservices.core.platformagents


import scala.collection.JavaConversions._
import org.specs._
import org.specs.util._
import org.specs.runner.JUnit4
import org.specs.runner.ConsoleRunner

import com.protegra.agentservicesstore.extensions.StringExtensions._
import com.protegra.agentservicesstore.extensions.OptionExtensions._
import com.protegra.agentservicesstore.extensions.URIExtensions._
import com.protegra.agentservicesstore.extensions.URIExtensions._
import java.util.UUID
import com.protegra_ati.agentservices.core.schema._
import com.protegra_ati.agentservices.core.schema.util._
import org.junit._
import com.protegra_ati.agentservices.core.messages.content._
import com.protegra.agentservicesstore.usage.AgentKVDBScope.acT._
import com.protegra_ati.agentservices.core.schema._
import com.protegra_ati.agentservices.core.messages._
import scala.util.Random
import java.net.URI
import org.specs.runner._
import com.protegra.agentservicesstore.usage.AgentKVDBScope._
import java.net.URI
import com.protegra_ati.agentservices.core.schema.util._
import com.protegra_ati.agentservices.core._
import com.protegra.agentservicesstore.util.Severity
import com.protegra_ati.agentservices.core.util.serializer.Serializer


class BasePlatformAgentTest
  extends JUnit4(BasePlatformAgentTestSpecs)

object BasePlatformAgentTestSpecsRunner
  extends ConsoleRunner(BasePlatformAgentTestSpecs)

object BasePlatformAgentTestSpecs extends Specification
with RabbitTestSetup
with Timeouts
{
  val rand = new Random()

  val sourceId = UUID.randomUUID()
  val targetId = sourceId
  val cnxn = new AgentCnxnProxy(sourceId.toString.toURI, "", targetId.toString.toURI)

  def createPA: MockPlatformAgent =
  {
    val dbAddress = "127.0.0.1".toURI.withPort(RABBIT_PORT_STORE_DB)
    val privateAddress = "127.0.0.1".toURI.withPort(RABBIT_PORT_STORE_PRIVATE)
    val privateAcquaintanceAddresses = List[ URI ]()
    val pa = new MockPlatformAgent()
    // pa._cnxnUIStore = cnxn
    pa.initForTest(privateAddress, privateAcquaintanceAddresses, dbAddress, UUID.randomUUID)
    pa
  }

  //can't conflict on DB port
  def createPA1: MockPlatformAgent =
  {
    val dbAddress = "127.0.0.1".toURI.withPort(RABBIT_PORT_STORE_PUBLIC_UNRELATED)
    val privateAddress = "127.0.0.1".toURI.withPort(RABBIT_PORT_UI_PRIVATE)
    val privateAcquaintanceAddresses = List[ URI ]("127.0.0.1".toURI.withPort(RABBIT_PORT_STORE_PRIVATE))
    val pa = new MockPlatformAgent()
    // pa._cnxnUIStore = cnxn
    pa.initForTest(privateAddress, privateAcquaintanceAddresses, dbAddress, UUID.randomUUID)
    pa
  }

  def createPA2: MockPlatformAgent =
  {
    val dbAddress = "127.0.0.1".toURI.withPort(RABBIT_PORT_STORE_PUBLIC_UNRELATED)
    val privateAddress = "127.0.0.1".toURI.withPort(RABBIT_PORT_STORE_PRIVATE)
    val privateAcquaintanceAddresses = List[ URI ]("127.0.0.1".toURI.withPort(RABBIT_PORT_UI_PRIVATE))
    val pa = new MockPlatformAgent()
    // pa._cnxnUIStore = cnxn
    pa.initForTest(privateAddress, privateAcquaintanceAddresses, dbAddress, UUID.randomUUID)
    pa
  }

  @transient val pa = createPA

  "putGet" should {
    val mockDataGet = new Profile("test", "me",  "test Description",  "123@test.com","CA", "someCAprovince", "city", "postalCode","website",null )
    val parentIds = new Identification()
    val mockMsg = new GetContentResponse(parentIds.copyAsChild(), null, List(mockDataGet))
    var result: Option[ GetContentResponse ] = None

    "retrieve" in {
      val key = "profile(\"putGet\")"
      pa.put(pa._dbQ, cnxn, key, Serializer.serialize[ Message ](mockMsg))

      SleepToPreventContinuation()
      pa.get(pa._dbQ, cnxn, key, handleGet)
      result.value must be_==(mockMsg).eventually(5, TIMEOUT_EVENTUALLY)
    }

    //this assumes there is no continuation on get
    def handleGet(cnxn: AgentCnxnProxy, msg: Message) =
    {
      msg match {
        case x: GetContentResponse => {
          result = Some(x)
        }
        case _ => {
        }
      }
    }
  }

  "storeFetch Profile" should {
    val mockDataFetch = new Profile("test", "me",  "test Description", "123@test.com","CA", "someCAprovince", "city", "postalCode", "website" )
    //remove mockDataSearch not needed
    val mockDataSearch = new Profile("test", "me",  "test Description", "123@test.com","CA", "someCAprovince", "city", "postalCode", "website" )
    var result: Option[ Profile ] = None

    "retrieve" in {
      pa.store(pa._dbQ, cnxn, mockDataFetch.toStoreKey, Serializer.serialize[ Data ](mockDataFetch))
      fetchData(pa._dbQ, cnxn, mockDataFetch.toStoreKey) must be_==(mockDataFetch).eventually(5, TIMEOUT_EVENTUALLY)
    }
    "search" in {
      pa.store(pa._dbQ, cnxn, mockDataSearch.toStoreKey, Serializer.serialize[ Data ](mockDataFetch))
      val search = new Profile ()
      search.id = mockDataSearch.id.toString
      fetchData(pa._dbQ, cnxn, search.toSearchKey) must be_==(mockDataFetch).eventually(5, TIMEOUT_EVENTUALLY)
    }

    def fetchData(queue: Being.AgentKVDBNode[ PersistedKVDBNodeRequest, PersistedKVDBNodeResponse ], cnxn: AgentCnxnProxy, searchKey: String): Any =
    {
      pa.fetch[ Data ](queue, cnxn, searchKey, handleFetch)
      return result.value
    }

    //this assumes there is no continuation on fetch
    def handleFetch(cnxn: AgentCnxnProxy, data: Data) =
    {
      data match {
        case x: Profile => {
          result = Some(x)
        }
        case _ => {
        }
      }
    }
  }

  "storeListen" should {
    var result: Option[ GetContentRequest ] = None

    "listen and handle request" in {
      val mockProfile = new Profile("test", "me",  "test Description",  "123@test.com","CA", "someCAprovince", "city", "postalCode", "website" )
      val mockSearch = new Profile ()
      mockSearch.id = mockProfile.id.toString
      val mockMsg = new GetContentRequest(new EventKey(UUID.randomUUID(), ""), mockSearch)

      pa.store(pa._privateQ, cnxn, mockMsg.getChannelKey, Serializer.serialize[ Message ](mockMsg))
      SleepToPreventContinuation()
      listen(pa._privateQ, cnxn, handlePrivateContentRequestChannel(_: AgentCnxnProxy, _: Message)).value must be_==(mockMsg).eventually(5, TIMEOUT_EVENTUALLY)
    }

    def listen(q: Being.AgentKVDBNode[ PersistedKVDBNodeRequest, PersistedKVDBNodeResponse ], cnxn: AgentCnxnProxy, handler: (AgentCnxnProxy, Message) => Unit) =
    {
      pa.listen(q, cnxn, Channel.Content, ChannelType.Request, ChannelLevel.Private, handler(_: AgentCnxnProxy, _: Message))
      result
    }

    //this assumes there is no continuation on listen
    def handlePrivateContentRequestChannel[T <:Data](cnxn: AgentCnxnProxy, msg: Message) =
    {
      msg match {
        case x: GetContentRequest => {
          result = Some(x)
        }
        case _ => {
        }
      }
    }
  }

  "delete" should {
    val mockProfile1 = new Profile("test1", "me",  "test Description",  "123@test.com","CA", "someCAprovince", "city", "postalCode", "website" )
    var result: Option[ Profile ] = None

    "delete 1 record" in {
      pa.store(pa._dbQ, cnxn, mockProfile1.toStoreKey, Serializer.serialize[ Data ](mockProfile1))
      fetchData(pa._dbQ, cnxn, mockProfile1.toStoreKey) must be_==(Some(mockProfile1)).eventually(5, TIMEOUT_EVENTUALLY)
      Thread.sleep(TIMEOUT_LONG)
      pa.delete(pa._dbQ, cnxn, mockProfile1.toStoreKey)
      fetchData(pa._dbQ, cnxn, mockProfile1.toStoreKey) must be_==(None).eventually(5, TIMEOUT_EVENTUALLY)
    }

    def fetchData(queue: Being.AgentKVDBNode[ PersistedKVDBNodeRequest, PersistedKVDBNodeResponse ], cnxn: AgentCnxnProxy, key: String): Option[ Data ] =
    {
      result = None
      pa.fetch[ Data ](queue, cnxn, key, handleFetch)
      return result
    }

    def handleFetch(cnxn: AgentCnxnProxy, data: Data) =
    {
      data match {
        case x: Profile => {
          result = Some(x)
        }
        case _ => {
          result = None
        }
      }
    }
  }

  "createDrop" should {
    val newId = "Collection" + UUID.randomUUID
    val newCnxn = new AgentCnxnProxy(newId.toURI, "", newId.toURI)

    val newProfile = new Profile("firstName", "lastName", "test Description", "111111111@test.com","CA", "someCAprovince", "city", "postalCode", "website" )
    var result: Option[ Profile ] = None

    "create and drop collection" in {
      pa.store(pa._dbQ, newCnxn, newProfile.toStoreKey, Serializer.serialize[ Data ](newProfile))
      Thread.sleep(TIMEOUT_LONG)

      pa.drop(pa._dbQ, newCnxn)
      Thread.sleep(TIMEOUT_LONG)

      fetchData(pa._dbQ, newCnxn, newProfile.toStoreKey) must be_==(None).eventually(5, TIMEOUT_EVENTUALLY)
    }

    def fetchData(queue: Being.AgentKVDBNode[ PersistedKVDBNodeRequest, PersistedKVDBNodeResponse ], cnxn: AgentCnxnProxy, key: String): Option[ Data ] =
    {
      result = None
      pa.fetch[ Data ](queue, cnxn, key, handleFetch)
      return result
    }

    def handleFetch(cnxn: AgentCnxnProxy, data: Data) =
    {
      data match {
        case x: Profile => result = Some(x)
        case _ => None
      }
    }
  }

//  "listen from stored connection" should {
//    @transient val pa1 = createPA1
////    @transient val pa2 = createPA2
//    var msgReceived = false
//
//    //store connection
//    val JenId = "Jen" + UUID.randomUUID
//    val MikeId = "Mike" + UUID.randomUUID
//    val conn = ConnectionFactory.createConnection("Jen", ConnectionCategory.Person.toString, ConnectionCategory.Person.toString, "Full", JenId.toString, MikeId.toString)
//    val newCnxn = new AgentCnxnProxy(UUID.randomUUID.toString.toURI, "", UUID.randomUUID.toString.toURI)
//    pa1.store(pa1._dbQ, newCnxn, conn.toStoreKey, Serializer.serialize[ Data ](conn))
//    Thread.sleep(TIMEOUT_LONG)
//
//    //fetch connection
//    val connSearch = new  Connection ()
//    pa1.fetch[ Data ](pa1._dbQ, newCnxn, connSearch.toSearchKey, handleFetchConnection)
//    //Thread.sleep(TIMEOUT_LONG)
//
//    //listen on handler
//    "receive message" in {
//      val profileSearch = new Profile ()
//      val request = GetContentRequest(EventKey(UUID.randomUUID(), ""), profileSearch)
//      pa1.send(pa1._privateQ, conn.readCnxn, request)
//      msgReceived must be_==(true).eventually(5, TIMEOUT_EVENTUALLY)
//    }
//
//    def handleFetchConnection(cnxn: AgentCnxnProxy, data: Data) =
//    {
//      data match {
//        case x: Connection => {
//          pa1.listen(pa1._privateQ, x.readCnxn, Channel.Content, ChannelType.Request, ChannelLevel.Private, listenHandler(_: AgentCnxnProxy, _: Message))
//        }
//        case _ => {}
//      }
//    }
//
//    def listenHandler(cnxn: AgentCnxnProxy, msg: Message)
//    {
//      msgReceived = true
//    }
//  }

}
