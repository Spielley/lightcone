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

import akka.actor.{ Actor, ActorLogging }
import akka.event.LoggingReceive
import akka.pattern._
import akka.util.Timeout
import org.loopring.lightcone.actors.Routers
import org.loopring.lightcone.core.base.TokenValueEstimator
import org.loopring.lightcone.core.market._
import org.loopring.lightcone.proto.actors._
import org.loopring.lightcone.proto.core._
import org.loopring.lightcone.actors.data._

import scala.concurrent.{ ExecutionContext, Future }

object MarketManagerActor {
  def name = "market_manager"
}

class MarketManagerActor(
    manager: MarketManager
)(
    implicit
    ec: ExecutionContext,
    timeout: Timeout,
    tve: TokenValueEstimator,
    routers: Routers
)
  extends Actor
  with ActorLogging {

  val gasPriceProviderActor = Routers.gasPriceProviderActor()
  val orderbookManagerActor = Routers.orderbookManagerActor()

  def receive: Receive = LoggingReceive {

    // TODO(hongyu): send a response to the sender
    case XSubmitOrderReq(Some(order)) ⇒
      order.status match {
        case XOrderStatus.NEW | XOrderStatus.PENDING ⇒ for {
          gasPriceRes ← (gasPriceProviderActor ? XGetGasPriceReq())
            .mapTo[XGetGasPriceRes]
          res = manager.submitOrder(
            order,
            getCostBySingleRing(BigInt(gasPriceRes.gasPrice))
          )
          _ = orderbookManagerActor ! res.orderbookUpdate
        } yield Unit

        case s ⇒
          log.error(s"unexpected order status in XSubmitOrderReq: $s")
      }

    case XCancelOrderReq(orderId, hardCancel) ⇒
      manager.cancelOrder(orderId)

    case updatedGasPrce: XUpdatedGasPrice ⇒
      for {
        gasPriceRes ← (gasPriceProviderActor ? XGetGasPriceReq())
          .mapTo[XGetGasPriceRes]
        resOpt = manager.triggerMatch(
          true,
          getCostBySingleRing(BigInt(gasPriceRes.gasPrice))
        )
      } yield {
        resOpt foreach { res ⇒
          gasPriceProviderActor ! res.orderbookUpdate
        }
      }
  }

  private def getCostBySingleRing(gasPrice: BigInt) = {
    val costedEth = BigInt(400000) * gasPrice
    //todo:eth的标识符
    tve.getEstimatedValue("ETH", costedEth)
  }

}