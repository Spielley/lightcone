/*
 * Copyright 2018 Loopring Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.loopring.lightcone.actors.core

import akka.actor.ActorRef
import akka.testkit.TestActorRef
import org.loopring.lightcone.actors.core.CoreActorsIntegrationCommonSpec._
import org.loopring.lightcone.proto.core._
import org.loopring.lightcone.proto.actors.XStart

//todo:impl it after tested accountMangerRecovery
class CoreActorsIntegrationSpec_MarketManagerRecovery
  extends CoreActorsIntegrationCommonSpec(XMarketId(GTO_TOKEN.address, WETH_TOKEN.address)) {

  "when an marketManager starts" must {
    "first recover it and then receive order" in {

      val marketManagerActorRecovery: ActorRef = TestActorRef(
        new MarketManagerActor(
          actors,
          XMarketId(GTO_TOKEN.address, WETH_TOKEN.address),
          config,
          skipRecovery = false
        )
      )

      marketManagerActorRecovery ! XStart()

    }
  }
}