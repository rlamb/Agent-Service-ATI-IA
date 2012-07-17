package com.protegra.agentservicesstore

// -*- mode: Scala;-*-
// Filename:    SerializationTest.scala
// Authors:     lgm
// Creation:    Tue Apr  5 20:51:35 2011
// Copyright:   Not supplied
// Description:
// ------------------------------------------------------------------------

import org.specs.runner.JUnit4
import org.specs.runner.ConsoleRunner

import com.protegra.agentservicesstore.extensions.StringExtensions._
import com.protegra.agentservicesstore.extensions.ResourceExtensions._

import scala.util.continuations._

import com.protegra.agentservicesstore.usage.AgentKVDBScope._
import com.protegra.agentservicesstore.usage.AgentKVDBScope.acT._
import com.protegra.agentservicesstore.usage.AgentKVDBScope.mTT._
import Being.AgentKVDBNodeFactory

import com.protegra.agentservicesstore._
import com.biosimilarity.lift.lib.moniker._
import java.io.{ObjectOutputStream, ByteArrayOutputStream}
import java.util.{HashMap, UUID}
import org.specs.Specification
import biz.source_code.base64Coder.Base64Coder
import com.protegra.agentservicesstore.extensions.URIExtensions._
import java.net.URI

class SerializationTest
  extends JUnit4(SerializationTestSpecs)

object SerializationTestSpecsRunner
  extends ConsoleRunner(SerializationTestSpecs)

object SerializationTestSpecs extends Specification
with SpecsKVDBHelpers
with Timeouts
with RabbitTestSetup
{

   val timeoutBetween = 0

  "Serialize" should {
    "cnxn" in {
      val cnxn = new acT.AgentCnxn("localhost".toURI, "none", "localhost2".toURI)
      val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
      val oos: ObjectOutputStream = new ObjectOutputStream(baos)
      oos.writeObject(cnxn)
      oos.close()
      println(new String(Base64Coder.encode(baos.toByteArray())))
    }

    "empty hashmap" in {
      val map = new HashMap[ acT.AgentCnxn, String ]()
      val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
      val oos: ObjectOutputStream = new ObjectOutputStream(baos)
      oos.writeObject(map)
      oos.close()
      println(new String(Base64Coder.encode(baos.toByteArray())))
    }

    "full hashmap" in {
      val cnxnPartition = new HashMap[ acT.AgentCnxn, String ]()
      val cnxn = new acT.AgentCnxn("localhost".toURI, "none", "localhost2".toURI)
      cnxnPartition.put(cnxn, "test1")
      val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
      val oos: ObjectOutputStream = new ObjectOutputStream(baos)
      oos.writeObject(cnxnPartition)
      oos.close()
      println(new String(Base64Coder.encode(baos.toByteArray())))
    }

    "junction" in {
      val writer = createNode("127.0.0.1".toURI, List[ URI ]())
      val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
      val oos: ObjectOutputStream = new ObjectOutputStream(baos)
      oos.writeObject(writer)
      oos.close()
      println(new String(Base64Coder.encode(baos.toByteArray())))
    }

    "full stream" in {
      val stream = Stream(1)
      val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
      val oos: ObjectOutputStream = new ObjectOutputStream(baos)
      oos.writeObject(stream)
      oos.close()
    }

    //expected to fail
//    "empty stream" in {
//      val stream = Stream()
//      val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
//      val oos: ObjectOutputStream = new ObjectOutputStream(baos)
//      oos.writeObject(stream)
//      oos.close()
//    }

    "junction with get" in {
      val writer = createNode("127.0.0.1".toURI, List[ URI ]())
      val cnxn = new acT.AgentCnxn("localhost".toURI, "none", "localhost2".toURI)

      val keyPrivate = "channelPrivate(_)"
      println("get")
      reset {
        for ( e <- writer.get(cnxn)(keyPrivate.toLabel) ) {
          //removing e!= None causes it to work
          if ( e != None ) {
            println("listen received - " + e.toString)
          }
          else {
            println("listen received - none")
          }
        }
      }

      //sleep for clean output
      Thread.sleep(3000)

      val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
      val oos: ObjectOutputStream = new ObjectOutputStream(baos)
      oos.writeObject(writer)
      oos.close()
      println(new String(Base64Coder.encode(baos.toByteArray())))
    }
  }

}