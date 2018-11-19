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

import akka.actor._
import akka.event.LoggingReceive
import akka.util.Timeout
import akka.pattern.ask
import org.loopring.lightcone.actors.Routers
import org.loopring.lightcone.core.data.Order
import org.loopring.lightcone.core.account._
import org.loopring.lightcone.core.base.DustOrderEvaluator
import org.loopring.lightcone.proto.actors._
import org.loopring.lightcone.proto.core._
import org.loopring.lightcone.actors.data._

import scala.concurrent._

import XOrderStatus._
import XErrorCode._

object AccountManagerActor {
  def name = "account_manager"
}

class AccountManagerActor()(
    implicit
    ec: ExecutionContext,
    timeout: Timeout,
    routers: Routers,
    dustEvaluator: DustOrderEvaluator
)
  extends Actor
  with ActorLogging {

  implicit val orderPool = new AccountOrderPoolImpl()
  val manager = AccountManager.default()
  val accountBalanceActor: ActorRef = Routers.accountBalanceActor()
  val marketManagerActor: ActorRef = Routers.marketManagerActor()

  def receive: Receive = LoggingReceive {

    case XSubmitOrderReq(Some(xorder)) ⇒
      for {
        _ ← getTokenManager(xorder.tokenS)
        _ ← getTokenManager(xorder.tokenFee)
        order: Order = xorder
        _ = log.debug(s"submitting order to AccountManager: $order")
        successful = manager.submitOrder(order)
        updatedOrders = orderPool.takeUpdatedOrdersAsMap()
        xorder_ : XOrder = updatedOrders(order.id)
      } yield {
        if (successful) {
          log.debug(s"submitting order to market manager actor: $xorder_")
          // TODO(hongyu): Make sure marketManagerActor send response to sender
          marketManagerActor forward XSubmitOrderReq(Some(xorder_))
        } else {
          val error = convertOrderStatusToErrorCode(xorder_.status)
          sender ! XSubmitOrderRes(error = error)
        }
      }

    case req: XCancelOrderReq ⇒
      if (manager.cancelOrder(req.id)) {
        // TODO(hongyu): Make sure marketManagerActor send response to sender
        marketManagerActor forward req
      } else {
        sender ! XCancelOrderRes(error = ORDER_NOT_EXIST)
      }

    case XAddressBalanceUpdated(_, token, newBalance) ⇒
      updateBalanceOrAllowance(token, newBalance, _.setBalance(_))

    case XAddressAllowanceUpdated(_, token, newBalance) ⇒
      updateBalanceOrAllowance(token, newBalance, _.setAllowance(_))
  }

  private def convertOrderStatusToErrorCode(status: XOrderStatus): XErrorCode = status match {
    case INVALID_DATA ⇒ INVALID_ORDER_DATA
    case UNSUPPORTED_MARKET ⇒ INVALID_MARKET
    case CANCELLED_TOO_MANY_ORDERS ⇒ TOO_MANY_ORDERS
    case CANCELLED_DUPLICIATE ⇒ ORDER_ALREADY_EXIST
    case _ ⇒ UNKNOWN_ERROR
  }

  private def getTokenManager(token: String): Future[AccountTokenManager] = {
    if (manager.hasTokenManager(token))
      Future.successful(manager.getTokenManager(token))
    else for {
      res ← (accountBalanceActor ? XGetBalanceAndAllowancesReq)
        .mapTo[XGetBalanceAndAllowancesRes]
      tm = new AccountTokenManagerImpl(token, 1000)
      ba: BalanceAndAllowance = res.balanceAndAllowanceMap(token)
      _ = tm.setBalanceAndAllowance(ba.balance, ba.allowance)
      _ = manager.addTokenManager(tm)
    } yield {
      manager.getTokenManager(token)
    }
  }

  private def updateBalanceOrAllowance(
    token: String,
    amount: BigInt,
    method: (AccountTokenManager, BigInt) ⇒ Unit
  ) = for {
    tm ← getTokenManager(token)
    _ = method(tm, amount)
    updatedOrders = orderPool.takeUpdatedOrders()
  } yield {
    updatedOrders.foreach { order ⇒
      order.status match {
        case CANCELLED_LOW_BALANCE | CANCELLED_LOW_FEE_BALANCE ⇒
          marketManagerActor ! XCancelOrderReq(order.id)
        case s ⇒
          log.error(s"unexpected order status caused by balance/allowance upate: $s")
      }
    }
  }
}