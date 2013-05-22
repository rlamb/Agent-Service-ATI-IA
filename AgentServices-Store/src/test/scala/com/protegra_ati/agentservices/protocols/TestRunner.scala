package com.protegra_ati.agentservices.protocols

import com.protegra_ati.agentservices.protocols.msgs.ProtocolMessageTest

object TestRunner {
  def run() {
    specs2.run(new ProtocolMessageTest)
  }
}

// com.protegra_ati.agentservices.protocols.TestRunner.run()