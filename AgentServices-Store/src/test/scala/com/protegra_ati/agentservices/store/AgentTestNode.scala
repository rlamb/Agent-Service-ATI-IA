package com.protegra_ati.agentservices.store

import com.biosimilarity.evaluator.distribution.PortableAgentCnxn
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.biosimilarity.lift.lib.MonadicGenerators
import com.biosimilarity.lift.model.store.CnxnCtxtLabel
import scala.collection.mutable
import scala.util.continuations._

class AgentTestNode extends MonadicGenerators {
  val map = mutable.Map[String, mTT.Resource]()

  def get(cnxn: PortableAgentCnxn)(path: CnxnCtxtLabel[String, String, String]): Generator[Option[mTT.Resource],Unit,Unit] = {
    Generator {
      rk: (Option[mTT.Resource] => Unit @suspendable) =>
        val key = getKey(cnxn, path)

        shift {
          k: (Unit => Unit) => {
            while (!map.contains(key)) {
              Thread.sleep(100)
            }
            k()
          }
        }

        rk(map.remove(key))
    }
  }

  def subscribe(cnxn: PortableAgentCnxn)(path: CnxnCtxtLabel[String, String, String]): Generator[Option[mTT.Resource],Unit,Unit] = {
    Generator {
      rk: (Option[mTT.Resource] => Unit @suspendable) =>
        val key = getKey(cnxn, path)

        shift {
          k: (Unit => Unit) => {
            // TODO: Keep calling k() every time the result is found
            while (!map.contains(key)) {
              Thread.sleep(100)
            }
            k()
          }
        }

        rk(map.remove(key))
    }
  }

  def put(cnxn: PortableAgentCnxn)(ptn: mTT.GetRequest, rsrc: mTT.Resource): Unit = {
    val key = getKey(cnxn, ptn)
    map.put(key, rsrc)
  }

  def publish(cnxn: PortableAgentCnxn)(ptn: mTT.GetRequest, rsrc: mTT.Resource): Unit = {
    val key = getKey(cnxn, ptn)
    map.put(key, rsrc)
  }

  private def getKey(cnxn: PortableAgentCnxn, label: CnxnCtxtLabel[String, String, String]): String = {
    cnxn.src.toString + cnxn.trgt.toString + cnxn.label + label.toString
  }
}
