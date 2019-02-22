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

package io.lightcone.relayer.socketio.notifiers

import com.corundumstudio.socketio.SocketIOClient
import com.google.inject.Inject
import io.lightcone.lib.Address
import io.lightcone.relayer.socketio.{
  SocketIONotifier,
  SocketIOSubscriber,
  SubscribeTicker,
  TickerResponse
}

class TickerNotifier @Inject() extends SocketIONotifier[SubscribeTicker] {
  val eventName = "tickers"

  def wrapClient(
      client: SocketIOClient,
      subscription: SubscribeTicker
    ): SocketIOSubscriber[SubscribeTicker] =
    new SocketIOSubscriber[SubscribeTicker](
      client,
      subscription.copy(
        market = subscription.market.copy(
          baseToken = Address.normalize(subscription.market.baseToken),
          quoteToken = Address.normalize(subscription.market.quoteToken)
        )
      )
    )

  def shouldNotifyClient(
      subscription: SubscribeTicker,
      event: AnyRef
    ): Boolean = {
    event match {
      case ticker: TickerResponse =>
        subscription.market == ticker.market
      case _ => false

    }
  }

}