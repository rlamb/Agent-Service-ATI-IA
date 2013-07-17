package com.protegra_ati.agentservices.protocols

import com.biosimilarity.evaluator.distribution.PortableAgentCnxn
import com.biosimilarity.lift.model.store.CnxnCtxtLabel
import com.protegra_ati.agentservices.protocols.msgs.ProtocolMessage
import com.protegra_ati.agentservices.store.extensions.StringExtensions._
import com.protegra_ati.agentservices.store.{AgentTestNode, TestNodeWrapper}
import java.net.URI
import java.util.UUID
import org.specs2.mutable._
import org.specs2.specification.Scope
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ListenerHelperTest extends SpecificationWithJUnit with Tags {
  class ConcreteBaseProtocols extends BaseProtocols

  case class TestMessage(id: Option[UUID]) extends ProtocolMessage {
    def this() = this(None)
    override def toCnxnCtxtLabel: CnxnCtxtLabel[String, String, String] = "protocolMessage(testMessage(_))".toLabel
    override def isValid: Boolean = id != None
  }

  case class TestMessage2(id: Option[UUID]) extends ProtocolMessage {
    def this() = this(None)
    override def toCnxnCtxtLabel: CnxnCtxtLabel[String, String, String] = "protocolMessage(testMessage(_))".toLabel
    override def isValid: Boolean = id != None
  }

  class Setup extends Scope {
    val node = new TestNodeWrapper(new AgentTestNode)
    val baseProtocols = new ConcreteBaseProtocols
    val cnxn = new PortableAgentCnxn(new URI("a"), "ab", new URI("b"))
    val cnxn2 = new PortableAgentCnxn(new URI("b"), "ba", new URI("a"))
  }

  section("unit", "ia")

  "ListenHelper.listen" should {
    "get a correct message" in new Setup() {
      val id = UUID.randomUUID()
      val f = baseProtocols.listen(node, cnxn, new TestMessage())
      node.put(cnxn)(new TestMessage(Some(id)))

      val message = Await.result(f, Duration(10, "seconds"))

      message.id must be_==(Some(id))
    }

    "throw an exception on invalid message" in new Setup() {
      val f = baseProtocols.listen(node, cnxn, new TestMessage())
      node.put(cnxn)(new TestMessage())

      Await.result(f, Duration(10, "seconds")) must throwA[Exception]("invalid protocol message")
    }

    "throw an exception on wrong message" in new Setup() {
      val f = baseProtocols.listen(node, cnxn, new TestMessage())
      node.put(cnxn)(new TestMessage2())

      Await.result(f, Duration(10, "seconds")) must throwA[Exception]("unexpected protocol message")
    }
  }

  "ListenHelper.listenJoin" should {
    "get 2 correct messages" in new Setup() {
      val id1 = UUID.randomUUID()
      val id2 = UUID.randomUUID()
      val f = baseProtocols.listenJoin(node, cnxn, cnxn2, new TestMessage(), new TestMessage())
      node.put(cnxn)(new TestMessage(Some(id1)))
      node.put(cnxn2)(new TestMessage(Some(id2)))

      val messages = Await.result(f, Duration(10, "seconds"))

      messages._1.id must be_==(Some(id1)) and (messages._2.id must be_==(Some(id2)))
    }

    "throw an exception on invalid message 1" in new Setup() {
      val id = UUID.randomUUID()
      val f = baseProtocols.listenJoin(node, cnxn, cnxn2, new TestMessage(), new TestMessage())
      node.put(cnxn)(new TestMessage())
      node.put(cnxn2)(new TestMessage(Some(id)))

      Await.result(f, Duration(10, "seconds")) must throwA[Exception]("invalid protocol message")
    }

    "throw an exception on invalid message 2" in new Setup() {
      val id = UUID.randomUUID()
      val f = baseProtocols.listenJoin(node, cnxn, cnxn2, new TestMessage(), new TestMessage())
      node.put(cnxn)(new TestMessage(Some(id)))
      node.put(cnxn2)(new TestMessage())

      Await.result(f, Duration(10, "seconds")) must throwA[Exception]("invalid protocol message")
    }

    "throw an exception on wrong message 1" in new Setup() {
      val id = UUID.randomUUID()
      val f = baseProtocols.listenJoin(node, cnxn, cnxn2, new TestMessage(), new TestMessage())
      node.put(cnxn)(new TestMessage2())
      node.put(cnxn2)(new TestMessage(Some(id)))

      Await.result(f, Duration(10, "seconds")) must throwA[Exception]("unexpected protocol message")
    }

    "throw an exception on wrong message 2" in new Setup() {
      val id = UUID.randomUUID()
      val f = baseProtocols.listenJoin(node, cnxn, cnxn2, new TestMessage(), new TestMessage())
      node.put(cnxn)(new TestMessage(Some(id)))
      node.put(cnxn2)(new TestMessage2())

      Await.result(f, Duration(10, "seconds")) must throwA[Exception]("unexpected protocol message")
    }
  }

  "ListenHelper.listenSubscribe" should {
    "get a correct message" in new Setup() {
      val id = UUID.randomUUID()
      var result: Option[Either[Throwable, TestMessage]] = None

      baseProtocols.listenSubscribe(node, cnxn, new TestMessage(), (r: Either[Throwable, TestMessage]) => {
        result = Some(r)
      }: Unit)

      node.publish(cnxn)(new TestMessage(Some(id)))

      result must be_!=(None).eventually and
        (result.get.isRight must be_==(true)) and
        (result.get.right.get.id must be_==(Some(id)))
    }

    "get an exception on invalid message" in new Setup() {
      var result: Option[Either[Throwable, TestMessage]] = None

      baseProtocols.listenSubscribe(node, cnxn, new TestMessage(), (r: Either[Throwable, TestMessage]) => {
        result = Some(r)
      }: Unit)

      node.publish(cnxn)(new TestMessage())

      result must be_!=(None).eventually and
        (result.get.isLeft must be_==(true)) and
        (result.get.left.get.isInstanceOf[Exception] must be_==(true)) and
        (result.get.left.get.getMessage must be_==("invalid protocol message"))
    }

    "get an exception on wrong message" in new Setup() {
      var result: Option[Either[Throwable, TestMessage]] = None

      baseProtocols.listenSubscribe(node, cnxn, new TestMessage(), (r: Either[Throwable, TestMessage]) => {
        result = Some(r)
      }: Unit)

      node.publish(cnxn)(new TestMessage2())

      result must be_!=(None).eventually and
        (result.get.isLeft must be_==(true)) and
        (result.get.left.get.isInstanceOf[Exception] must be_==(true)) and
        (result.get.left.get.getMessage must be_==("unexpected protocol message"))
    }
  }
}
