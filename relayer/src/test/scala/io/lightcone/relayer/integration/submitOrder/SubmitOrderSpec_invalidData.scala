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

package io.lightcone.relayer.integration.submitOrder

import io.lightcone.core.ErrorCode._
import io.lightcone.core.ErrorException
import io.lightcone.relayer.data.AccountBalance.TokenBalance
import io.lightcone.relayer.data._
import io.lightcone.relayer.getUniqueAccount
import io.lightcone.relayer.integration.AddedMatchers._
import io.lightcone.relayer.integration._
import org.scalatest._

class SubmitOrderSpec_invalidData
    extends FeatureSpec
    with GivenWhenThen
    with CommonHelper
    with ValidateHelper
    with Matchers {

  feature("submit order") {
    scenario("invalid data of order") {
      implicit val account = getUniqueAccount()
      Given(
        s"an new account with enough balance and enough allowance: ${account.getAddress}"
      )

      addAccountExpects({
        case req =>
          GetAccount.Res(
            Some(
              AccountBalance(
                address = req.address,
                tokenBalanceMap = req.tokens.map { t =>
                  t -> AccountBalance.TokenBalance(
                    token = t,
                    balance = "1000".zeros(dynamicBaseToken.getDecimals()),
                    allowance = "1000".zeros(dynamicBaseToken.getDecimals())
                  )
                }.toMap
              )
            )
          )
      })

      When("submit an order with an invalid order sig ")
      val order1 = createRawOrder(
        tokenS = dynamicBaseToken.getAddress(),
        tokenB = dynamicQuoteToken.getAddress(),
        tokenFee = dynamicBaseToken.getAddress()
      )
      SubmitOrder
        .Req(
          Some(
            order1.copy(
              params = order1.params
                .map(p => p.copy(sig = "0x0"))
            )
          )
        )
        .expect(
          check(
            (err: ErrorException) =>
              err.error.code == ERR_ORDER_VALIDATION_INVALID_SIG
          )
        )
      Then("the error code of submit order is ERR_ORDER_VALIDATION_INVALID_SIG")

      defaultValidate(
        getOrdersMatcher = check((res: GetOrders.Res) => res.orders.isEmpty),
        accountMatcher = accountBalanceMatcher(
          dynamicBaseToken.getAddress(),
          TokenBalance(
            token = dynamicBaseToken.getAddress(),
            balance = "1000".zeros(dynamicBaseToken.getMetadata.decimals),
            allowance = "1000".zeros(dynamicBaseToken.getMetadata.decimals),
            availableBalance =
              "1000".zeros(dynamicBaseToken.getMetadata.decimals),
            availableAllowance =
              "1000".zeros(dynamicBaseToken.getMetadata.decimals)
          )
        ),
        marketMatchers = Map(
          dynamicMarketPair -> (orderBookIsEmpty(), defaultMatcher, defaultMatcher)
        )
      )

      When("submit an order that order owner is invalid")
      val order2 = createRawOrder(amountB = "20".zeros(18))
      SubmitOrder
        .Req(
          Some(
            order2.copy(
              owner = getUniqueAccount().getAddress
            )
          )
        )
        .expect(
          check(
            (err: ErrorException) =>
              err.error.code == ERR_ORDER_VALIDATION_INVALID_SIG
          )
        )

      Then("submit order failed caused by ERR_ORDER_VALIDATION_INVALID_SIG")

      Then(
        "status of order just submitted is status pending"
      )
      And(
        "balance, availableBalance, allowance and availableAllowance is 1000"
      )
      And("orderbook is empty")

      defaultValidate(
        getOrdersMatcher = check((res: GetOrders.Res) => res.orders.isEmpty),
        accountMatcher = accountBalanceMatcher(
          dynamicBaseToken.getAddress(),
          TokenBalance(
            token = dynamicBaseToken.getAddress(),
            balance = "1000".zeros(dynamicBaseToken.getMetadata.decimals),
            allowance = "1000".zeros(dynamicBaseToken.getMetadata.decimals),
            availableBalance =
              "1000".zeros(dynamicBaseToken.getMetadata.decimals),
            availableAllowance =
              "1000".zeros(dynamicBaseToken.getMetadata.decimals)
          )
        ),
        marketMatchers = Map(
          dynamicMarketPair -> (orderBookIsEmpty(), defaultMatcher, defaultMatcher)
        )
      )

      When("submit an order with an a wrong dualAuthAddr")
      val order3 = createRawOrder(amountS = "30".zeros(18))
      SubmitOrder
        .Req(
          Some(
            order3.copy(
              params = order3.params
                .map(
                  p => p.copy(dualAuthAddr = getUniqueAccount().getAddress)
                )
            )
          )
        )
        .expect(
          check(
            (err: ErrorException) => {
              err.error.code == ERR_ORDER_VALIDATION_INVALID_MISSING_DUALAUTH_PRIV_KEY
            }
          )
        )

      Then(
        "submit order failed caused by ERR_ORDER_VALIDATION_INVALID_MISSING_DUALAUTH_PRIV_KEY"
      )

      Then(
        "status of order just submitted is status pending"
      )
      And(
        "balance, availableBalance, allowance and availableAllowance is 1000"
      )
      And("orderbook is empty")

      defaultValidate(
        getOrdersMatcher = check((res: GetOrders.Res) => res.orders.isEmpty),
        accountMatcher = accountBalanceMatcher(
          dynamicBaseToken.getAddress(),
          TokenBalance(
            token = dynamicBaseToken.getAddress(),
            balance = "1000".zeros(dynamicBaseToken.getMetadata.decimals),
            allowance = "1000".zeros(dynamicBaseToken.getMetadata.decimals),
            availableBalance =
              "1000".zeros(dynamicBaseToken.getMetadata.decimals),
            availableAllowance =
              "1000".zeros(dynamicBaseToken.getMetadata.decimals)
          )
        ),
        marketMatchers = Map(
          dynamicMarketPair -> (orderBookIsEmpty(), defaultMatcher, defaultMatcher)
        )
      )
    }
  }
}
