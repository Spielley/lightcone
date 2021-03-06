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

package io.lightcone.relayer.integration

import io.lightcone.core.OrderStatus._
import io.lightcone.relayer._
import io.lightcone.relayer.data._
import io.lightcone.relayer.integration.AddedMatchers._
import org.scalatest._

import scala.math.BigInt

class CancelOrderSpec_cancelAffectOtherOrders
    extends FeatureSpec
    with GivenWhenThen
    with CommonHelper
    with CancelHelper
    with ValidateHelper
    with Matchers {

  feature("cancel orders of status=STATUS_PENDING") {
    scenario("2: cancel order will affect other orders ") {

      Given("an account with enough Balance")
      implicit val account = getUniqueAccount()
      val getAccountReq = GetAccount.Req(
        address = account.getAddress,
        allTokens = true
      )
      val accountInitRes = getAccountReq.expectUntil(
        check((res: GetAccount.Res) => res.accountBalance.nonEmpty)
      )
      val baseTokenBalance =
        accountInitRes.getAccountBalance.tokenBalanceMap(
          dynamicMarketPair.baseToken
        )

      Then("submit two orders that sum amountS of them bigger than balance.")
      val order1 = createRawOrder(
        tokenS = dynamicMarketPair.baseToken,
        tokenB = dynamicMarketPair.quoteToken,
        tokenFee = dynamicMarketPair.baseToken,
        amountS = (baseTokenBalance.availableBalance * 3) / 5,
        amountFee = baseTokenBalance.availableBalance / 10
      )
      val submitRes1 = SubmitOrder
        .Req(Some(order1))
        .expect(check((res: SubmitOrder.Res) => res.success))
      info(s"the result of submit the first order is ${submitRes1.success}")
      Thread.sleep(1000)
      val order2 = createRawOrder(
        tokenS = dynamicMarketPair.baseToken,
        tokenB = dynamicMarketPair.quoteToken,
        tokenFee = dynamicMarketPair.baseToken,
        amountS = (baseTokenBalance.availableBalance * 3) / 5,
        amountFee = baseTokenBalance.availableBalance / 10
      )
      val submitRes2 = SubmitOrder
        .Req(Some(order2))
        .expect(check((res: SubmitOrder.Res) => res.success))
      info(s"the result of submit the second order is ${submitRes2.success}")

      Then("cancel the first order by hash.")
      val cancelReq =
        CancelOrder.Req(
          owner = order1.owner,
          id = order1.hash,
          status = STATUS_SOFT_CANCELLED_BY_USER,
          time = BigInt(timeProvider.getTimeSeconds())
        )
      val sig = generateCancelOrderSig(cancelReq)
      val cancelRes = cancelReq
        .withSig(sig)
        .expect(check { res: CancelOrder.Res =>
          res.status == cancelReq.status
        })

      Then("check the cancel result.")
      val lrcExpectedBalance = baseTokenBalance.copy(
        availableBalance = baseTokenBalance.availableBalance - order2.amountS - order2.getFeeParams.amountFee,
        availableAllowance = baseTokenBalance.availableAllowance - order2.amountS - order2.getFeeParams.amountFee
      )
      defaultValidate(
        containsInGetOrders(
          STATUS_SOFT_CANCELLED_BY_USER,
          order1.hash
        )
          and containsInGetOrders(
            STATUS_PENDING,
            order2.hash
          ),
        accountBalanceMatcher(dynamicMarketPair.baseToken, lrcExpectedBalance),
        Map(
          dynamicMarketPair -> (not(orderBookIsEmpty()),
          userFillsIsEmpty(),
          marketFillsIsEmpty())
        )
      )
    }
  }
}
