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

import io.lightcone.core.{Amount, _}
import io.lightcone.ethereum.TxStatus
import io.lightcone.ethereum.event.{AddressBalanceUpdatedEvent, BlockEvent}
import io.lightcone.ethereum.persistence.{Activity, TxEvents}
import io.lightcone.lib.Address
import io.lightcone.lib.NumericConversion._
import io.lightcone.relayer._
import io.lightcone.relayer.actors.ActivityActor
import io.lightcone.relayer.data.{AccountBalance, GetAccount, GetActivities, GetPendingActivityNonce}
import io.lightcone.relayer.integration.AddedMatchers._
import io.lightcone.relayer.integration.Metadatas._
import io.lightcone.relayer.integration.helper._
import org.scalatest._
import scala.math.BigInt

class WETHWrapSpec_success
    extends FeatureSpec
    with GivenWhenThen
    with CommonHelper
      with AccountHelper
      with ActivityHelper
    with Matchers {

  feature("WETH wrap success") {
    scenario("wrap WETH") {
      implicit val account = getUniqueAccount()
      val txHash =
        "0xbc6331920f91aa6f40e10c3e6c87e6d58aec01acb6e9a244983881d69bc0cff4"
      val blockNumber = 987L
      val nonce = 11L

      Given("initialize eth balance")
      mockAccountWithFixedBalance(account.getAddress, dynamicMarketPair)
      val getFromAddressBalanceReq = GetAccount.Req(
        account.getAddress,
        allTokens = true
      )
      getFromAddressBalanceReq.expectUntil(
        check((res: GetAccount.Res) => {
          val balanceOpt = res.accountBalance
          val ethBalance = toBigInt(
            balanceOpt.get.tokenBalanceMap(Address.ZERO.toString).balance.get
          )
          val wethBalance = toBigInt(
            balanceOpt.get.tokenBalanceMap(WETH_TOKEN.address).balance.get
          )
          ethBalance == "20".zeros(18) && wethBalance == "30".zeros(18)
        })
      )

      When("send some convert events")
      Seq(
        TxEvents(
          TxEvents.Events.Activities(
            TxEvents.Activities(
              Seq(
                Activity(
                  owner = account.getAddress,
                  block = blockNumber,
                  txHash = txHash,
                  activityType = Activity.ActivityType.ETHER_WRAP,
                  timestamp = timeProvider.getTimeSeconds,
                  token = Address.ZERO.toString(),
                  detail = Activity.Detail.EtherConversion(
                    Activity.EtherConversion(
                      Some(
                        toAmount("10000000000000000000")
                      )
                    )
                  ),
                  nonce = 11
                ),
                Activity(
                  owner = account.getAddress,
                  block = blockNumber,
                  txHash = txHash,
                  activityType = Activity.ActivityType.ETHER_WRAP,
                  timestamp = timeProvider.getTimeSeconds,
                  token = WETH_TOKEN.address,
                  detail = Activity.Detail.EtherConversion(
                    Activity.EtherConversion(
                      Some(
                        toAmount("10000000000000000000")
                      )
                    )
                  ),
                  nonce = 11
                )
              )
            )
          )
        )
      ).foreach(eventDispatcher.dispatch)

      Thread.sleep(1000)
      Then("the account should query 2 pending activity")
      GetActivities
        .Req(account.getAddress)
        .expectUntil(
          check((res: GetActivities.Res) => {
            res.activities.length == 2 && !res.activities
              .exists(a => a.txStatus != TxStatus.TX_STATUS_PENDING)
          })
        )

      When("activities confirmed")
      val blockEvent = BlockEvent(
        blockNumber = blockNumber,
        txs = Seq(
          BlockEvent.Tx(
            from = account.getAddress,
            nonce = 11,
            txHash = txHash
          )
        )
      )
      ActivityActor.broadcast(blockEvent)
      Thread.sleep(2000)

      Seq(
        TxEvents(
          TxEvents.Events.Activities(
            TxEvents.Activities(
              Seq(
                Activity(
                  owner = account.getAddress,
                  block = blockNumber,
                  txHash = txHash,
                  activityType = Activity.ActivityType.ETHER_WRAP,
                  timestamp = timeProvider.getTimeSeconds,
                  token = Address.ZERO.toString(),
                  detail = Activity.Detail.EtherConversion(
                    Activity.EtherConversion(
                      Some(
                        toAmount("10000000000000000000")
                      )
                    )
                  ),
                  nonce = 11,
                  txStatus = TxStatus.TX_STATUS_SUCCESS
                ),
                Activity(
                  owner = account.getAddress,
                  block = blockNumber,
                  txHash = txHash,
                  activityType = Activity.ActivityType.ETHER_WRAP,
                  timestamp = timeProvider.getTimeSeconds,
                  token = WETH_TOKEN.address,
                  detail = Activity.Detail.EtherConversion(
                    Activity.EtherConversion(
                      Some(
                        toAmount("10000000000000000000")
                      )
                    )
                  ),
                  nonce = 11,
                  txStatus = TxStatus.TX_STATUS_SUCCESS
                )
              )
            )
          )
        ),
        AddressBalanceUpdatedEvent(
          address = account.getAddress,
          token = Address.ZERO.toString(),
          balance = Some(
            toAmount("10000000000000000000")
          ),
          block = blockNumber
        ),
        AddressBalanceUpdatedEvent(
          address = account.getAddress,
          token = WETH_TOKEN.address,
          balance = Some(
            toAmount("30000000000000000000")
          ),
          block = blockNumber
        )
      ).foreach(eventDispatcher.dispatch)
      Thread.sleep(1000)

      GetActivities
        .Req(account.getAddress)
        .expectUntil(
          check((res: GetActivities.Res) => {
            res.activities.length == 2 && !res.activities
              .exists(a => a.txStatus != TxStatus.TX_STATUS_SUCCESS)
          })
        )

      getFromAddressBalanceReq.expectUntil(
        check((res: GetAccount.Res) => {
          val balanceOpt = res.accountBalance
          val ethBalance = toBigInt(
            balanceOpt.get.tokenBalanceMap(Address.ZERO.toString).balance.get
          )
          val ethAvailableBalance = toBigInt(
            balanceOpt.get
              .tokenBalanceMap(Address.ZERO.toString)
              .availableBalance
              .get
          )
          val wethBalance = toBigInt(
            balanceOpt.get.tokenBalanceMap(WETH_TOKEN.address).balance.get
          )
          val wethAvailableBalance = toBigInt(
            balanceOpt.get
              .tokenBalanceMap(WETH_TOKEN.address)
              .availableBalance
              .get
          )
          ethBalance == BigInt("10000000000000000000") && ethBalance == ethAvailableBalance && wethBalance == BigInt(
            "30000000000000000000"
          ) && wethBalance == wethAvailableBalance
        })
      )
    }
  }
}
