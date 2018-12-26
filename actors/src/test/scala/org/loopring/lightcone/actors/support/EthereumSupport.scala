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

package org.loopring.lightcone.actors.support

import akka.actor.Props
import org.loopring.lightcone.actors.core._
import org.loopring.lightcone.actors.ethereum.{
  EthereumAccessActor,
  EthereumClientMonitor
}
import org.loopring.lightcone.actors.validator.{
  EthereumQueryMessageValidator,
  MessageValidationActor
}

trait EthereumSupport {
  my: CommonSpec =>

  actors.add(
    EthereumQueryActor.name,
    EthereumQueryActor.startShardRegion()
  )
  actors.add(
    EthereumQueryMessageValidator.name,
    MessageValidationActor(
      new EthereumQueryMessageValidator(),
      EthereumQueryActor.name,
      EthereumQueryMessageValidator.name
    )
  )

  if (!actors.contains(GasPriceActor.name)) {
    actors.add(GasPriceActor.name, GasPriceActor.startShardRegion())
  }
  actors.add(EthereumAccessActor.name, EthereumAccessActor.startSingleton())
  actors.add(EthereumClientMonitor.name, EthereumClientMonitor.startSingleton())
}
