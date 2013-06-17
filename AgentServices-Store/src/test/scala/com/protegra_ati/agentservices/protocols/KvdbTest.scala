package com.protegra_ati.agentservices.protocols

import com.biosimilarity.evaluator.distribution.ConcreteHL
import com.biosimilarity.evaluator.distribution.diesel.DieselEngine
import com.biosimilarity.evaluator.distribution.diesel.DieselEngineScope._
import com.protegra_ati.agentservices.store.extensions.StringExtensions._
import java.net.URI
import org.specs2.mutable._
import org.specs2.specification.Scope
import scala.concurrent.ops._
import scala.util.continuations._

class KvdbTest extends SpecificationWithJUnit {
  class Setup extends Scope {
    val engine = new DieselEngine(Some("eval.conf"))
    val node = engine.agent("/dieselProtocol")
    val cnxn = new acT.AgentCnxn(new URI("a"), "ab", new URI("b"))
    val label = "test(\"test\")".toLabel
  }

  "KVDB" should {
    "Get and Put" in new Setup() {
      var complete = false

      spawn {
        reset {
          for (e <- node.get(cnxn)(label)) {
            e match {
              case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, value: String))) => {
                complete = true
              }
              case _ => throw new Exception("unexpected protocol message")
            }
          }
        }
      }

      Thread.sleep(5000)

      spawn {
        reset {
          node.put(cnxn)(label, mTT.Ground(ConcreteHL.InsertContent(label, Nil, "")))
        }
      }

      complete must be_==(true).eventually(10, new org.specs2.time.Duration(1000))
    }

    "Put and Get" in new Setup() {
      var complete = false

      spawn {
        reset {
          node.put(cnxn)(label, mTT.Ground(ConcreteHL.InsertContent(label, Nil, "")))
        }
      }

      Thread.sleep(5000)

      spawn {
        reset {
          for (e <- node.get(cnxn)(label)) {
            e match {
              case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, value: String))) => {
                complete = true
              }
              case _ => throw new Exception("unexpected protocol message")
            }
          }
        }
      }

      complete must be_==(true).eventually(10, new org.specs2.time.Duration(1000))
    }

    "Subscribe and Publish" in new Setup() {
      var complete = false

      spawn {
        reset {
          for (e <- node.subscribe(cnxn)(label)) {
            e match {
              case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, value: String))) => {
                complete = true
              }
              case _ => throw new Exception("unexpected protocol message")
            }
          }
        }
      }

      Thread.sleep(5000)

      spawn {
        reset {
          node.publish(cnxn)(label, mTT.Ground(ConcreteHL.InsertContent(label, Nil, "")))
        }
      }

      complete must be_==(true).eventually(10, new org.specs2.time.Duration(1000))
    }

    "Publish and Subscribe" in new Setup() {
      var complete = false

      spawn {
        reset {
          node.publish(cnxn)(label, mTT.Ground(ConcreteHL.InsertContent(label, Nil, "")))
        }
      }

      Thread.sleep(5000)

      spawn {
        reset {
          for (e <- node.subscribe(cnxn)(label)) {
            e match {
              case Some(mTT.Ground(ConcreteHL.InsertContent(_, _, value: String))) => {
                complete = true
              }
              case _ => throw new Exception("unexpected protocol message")
            }
          }
        }
      }

      complete must be_==(true).eventually(10, new org.specs2.time.Duration(1000))
    }
  }
}
