package com.protegra_ati.agentservices.store.util

import java.util.UUID
import org.specs2.mutable.SpecificationWithJUnit
import scala.collection.JavaConversions._

class MultiCacheMapTest extends SpecificationWithJUnit {

  "MultiCacheMap" should {
    "Add correct number of listeners" in {
      val listeners = new MultiCacheMap[String]("T")
      val agentSessionId1 = UUID.randomUUID
      val agentSessionId2 = UUID.randomUUID

      listeners.add(agentSessionId1.toString, "tag1")
      listeners.add(agentSessionId2.toString, "tag2")
      listeners.add(agentSessionId2.toString, "tag3")

      1 must be_==(listeners.get(agentSessionId1.toString).toList.length)
      2 must be_==(listeners.get(agentSessionId2.toString).toList.length)
    }
  }
}
