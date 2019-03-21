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

import io.lightcone.core.OrderStatus.STATUS_PENDING
import io.lightcone.core._
import io.lightcone.lib.NumericConversion
import io.lightcone.relayer.data.AccountBalance.TokenBalance
import io.lightcone.relayer.data._
import io.lightcone.relayer.getUniqueAccount
import io.lightcone.relayer.integration.AddedMatchers.{
  accountBalanceMatcher,
  check,
  containsInGetOrders,
  outStandingMatcherInGetOrders
}
import io.lightcone.relayer.integration.Metadatas._
import io.lightcone.relayer.integration._
import org.scalatest._

class SubmitOrderSpec_EnoughBalanceAndAllowance
    extends FeatureSpec
    with GivenWhenThen
    with CommonHelper
    with ValidateHelper
    with Matchers {

  feature("submit  order ") {
    scenario("enough balance and enough allowance") {
      implicit val account = getUniqueAccount()
      Given("an new account with enough balance and enough allowance")
      GetAccount
        .Req(
          address = account.getAddress,
          tokens = Seq(dynamicBaseToken.getAddress())
        )
        .expectUntil(
          check((res: GetAccount.Res) => res.accountBalance.nonEmpty)
        )

      When("submit an order.")
      val order1 = createRawOrder(
        tokenS = dynamicBaseToken.getAddress(),
        tokenB = dynamicQuoteToken.getAddress(),
        tokenFee = dynamicBaseToken.getAddress(),
        amountS = "40".zeros(dynamicBaseToken.getMetadata.decimals),
        amountFee = "10".zeros(dynamicBaseToken.getMetadata.decimals)
      )
      SubmitOrder
        .Req(Some(order1))
        .expect(check((res: SubmitOrder.Res) => res.success))

      defaultValidate(
        getOrdersMatcher = containsInGetOrders(STATUS_PENDING, order1.hash) and
          outStandingMatcherInGetOrders(
            RawOrder.State(
              outstandingAmountS =
                "40".zeros(dynamicBaseToken.getMetadata.decimals),
              outstandingAmountB =
                "1".zeros(dynamicQuoteToken.getMetadata.decimals),
              outstandingAmountFee =
                "10".zeros(dynamicBaseToken.getMetadata.decimals)
            ),
            order1.hash
          ),
        accountMatcher = accountBalanceMatcher(
          dynamicBaseToken.getAddress(),
          TokenBalance(
            token = dynamicBaseToken.getAddress(),
            balance = "1000".zeros(dynamicBaseToken.getMetadata.decimals),
            allowance = "1000".zeros(dynamicBaseToken.getMetadata.decimals),
            availableBalance =
              "950".zeros(dynamicBaseToken.getMetadata.decimals),
            availableAlloawnce =
              "950".zeros(dynamicBaseToken.getMetadata.decimals)
          )
        ),
        marketMatchers = Map(
          dynamicMarketPair -> (check(
            (res: GetOrderbook.Res) =>
              res.getOrderbook.sells.map(_.amount.toDouble).sum == 40
          ), defaultMatcher, defaultMatcher)
        )
      )

      Then("the status of the order just submitted is status pending")
      And(
        "balance and allowance is 1000, available balance and available allowance is 950"
      )
      And(s" sell amount of order book is 40")
    }
  }

}
