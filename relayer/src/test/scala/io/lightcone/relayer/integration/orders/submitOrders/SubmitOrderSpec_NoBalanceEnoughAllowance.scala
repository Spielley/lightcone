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

package io.lightcone.relayer.integration.orders.submitOrders

import io.lightcone.core.ErrorException
import io.lightcone.lib.NumericConversion
import io.lightcone.relayer._
import io.lightcone.relayer.data._
import io.lightcone.relayer.integration.AddedMatchers.check
import io.lightcone.relayer.integration.Metadatas.LRC_TOKEN
import io.lightcone.relayer.integration._
import org.scalatest._

import scala.math.BigInt

class SubmitOrderSpec_NoBalanceEnoughAllowance
    extends FeatureSpec
    with GivenWhenThen
    with CommonHelper
    with Matchers {

  feature("submit order") {
    scenario("no balance and enough allowance") {
      implicit val account = getUniqueAccount()
      Given("an new account with no balance and enough allowance")

      addAccountExpects({
        case req =>
          GetAccount.Res(
            Some(
              AccountBalance(
                address = req.address,
                tokenBalanceMap = req.tokens.map { t =>
                  t -> AccountBalance.TokenBalance(
                    token = t,
                    balance = BigInt("0"),
                    allowance = "100000".zeros(LRC_TOKEN.decimals),
                    availableAlloawnce = BigInt("0"),
                    availableBalance = "100000".zeros(LRC_TOKEN.decimals)
                  )
                }.toMap
              )
            )
          )
      })

      val getBalanceReq = GetAccount.Req(
        account.getAddress,
        tokens = Seq(LRC_TOKEN.name)
      )
      val res = getBalanceReq.expectUntil(
        check((res: GetAccount.Res) => {
          println(res)
          val lrc_ba = res.getAccountBalance.tokenBalanceMap(LRC_TOKEN.address)
          NumericConversion.toBigInt(lrc_ba.getBalance) == 0 &&
          NumericConversion.toBigInt(lrc_ba.getAvailableBalance) == 0 &&
          NumericConversion.toBigInt(lrc_ba.getAllowance) > "100".zeros(
            LRC_TOKEN.decimals
          ) &&
          NumericConversion.toBigInt(lrc_ba.getAvailableAlloawnce) > "100"
            .zeros(LRC_TOKEN.decimals)
        })
      )

      When("submit an order.")

      try {
        val submitRes = SubmitOrder
          .Req(Some(createRawOrder()))
          .expect(check((res: SubmitOrder.Res) => !res.success))
      } catch {
        case e: ErrorException =>
      }
      val getOrdersRes = GetOrders
        .Req(owner = account.getAddress)
        .expectUntil(
          check((res: GetOrders.Res) => {
            res.orders.head.getState.status.isStatusSoftCancelledLowBalance
          })
        )

      Then(
        s"the status of the order just submitted is ${getOrdersRes.orders.head.getState.status}"
      )

    }
  }
}
