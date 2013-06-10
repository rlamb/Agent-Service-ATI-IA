package com.protegra_ati.agentservices.store

import com.biosimilarity.evaluator.distribution.ConcreteHL
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.biosimilarity.lift.lib.MonadicGenerators
import com.biosimilarity.lift.model.store.CnxnCtxtLabel
import scala.collection.mutable
import scala.concurrent._
import scala.util.continuations._
import ExecutionContext.Implicits.global

class AgentTestNode extends MonadicGenerators {
  val map = mutable.Map[String, mTT.Resource]()

  def get(cnxn: ConcreteHL.PortableAgentCnxn)(path: CnxnCtxtLabel[String, String, String]): Generator[Option[mTT.Resource],Unit,Unit] = {
    Generator {
      rk: (Option[mTT.Resource] => Unit @suspendable) =>
        val key = getKey(cnxn, path)

        shift {
          k: (Unit => Unit) => {
            val f = future {
              while (!map.contains(key)) {
                Thread.sleep(100)
              }
            }

            f.onComplete {
              case _ => k()
            }
          }
        }

        rk(map.remove(key))
    }
  }

  def subscribe(cnxn: ConcreteHL.PortableAgentCnxn)(path: CnxnCtxtLabel[String, String, String]): Generator[Option[mTT.Resource],Unit,Unit] = {
    Generator {
      rk: (Option[mTT.Resource] => Unit @suspendable) =>
        val key = getKey(cnxn, path)

        shift {
          k: (Unit => Unit) => {
            // TODO: Keep calling k() every time the result is found
            val f = future {
              while (!map.contains(key)) {
                Thread.sleep(100)
              }
            }

            f.onComplete {
              case _ => k()
            }
          }
        }

        rk(map.remove(key))
    }
  }

  def put(cnxn: ConcreteHL.PortableAgentCnxn)(ptn: mTT.GetRequest, rsrc: mTT.Resource): Unit = {
    val key = getKey(cnxn, ptn)
    map.put(key, rsrc)
  }

  private def getKey(cnxn: ConcreteHL.PortableAgentCnxn, label: CnxnCtxtLabel[String, String, String]): String = {
    cnxn.src.toString + cnxn.trgt.toString + cnxn.label + label.toString
  }
}
