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

import io.lightcone.lib.Address
import io.lightcone.relayer._
import io.lightcone.relayer.data.{
  GetAccount,
  GetActivities,
  GetPendingActivityNonce
}
import io.lightcone.relayer.integration.AddedMatchers._
import io.lightcone.lib.NumericConversion._
import org.scalatest._
import io.lightcone.ethereum.TxStatus
import io.lightcone.relayer.actors.ActivityActor
import io.lightcone.relayer.integration.helper.{AccountHelper, ActivityHelper}

class TransferETHSpec_success
    extends FeatureSpec
    with GivenWhenThen
    with CommonHelper
    with AccountHelper
    with ActivityHelper
    with Matchers {

  feature("transfer success") {
    scenario("transfer ETH") {
      implicit val account = getUniqueAccount()
      val txHash =
        "0xbc6331920f91aa6f40e10c3e6c87e6d58aec01acb6e9a244983881d69bc0cff4"
      val to = "0xf51df14e49da86abc6f1d8ccc0b3a6b7b7c90ca6"
      val blockNumber = 987L
      val nonce = 11L

      Given("initialize eth balance")
      mockAccountWithFixedBalance(account.getAddress, dynamicMarketPair)

      val getFromAddressBalanceReq = GetAccount.Req(
        account.getAddress,
        allTokens = true
      )
      val getToAddressBalanceReq = GetAccount.Req(
        to,
        allTokens = true
      )
      getFromAddressBalanceReq.expectUntil(
        check((res: GetAccount.Res) => {
          val balanceOpt = res.accountBalance
          val ethBalance = toBigInt(
            balanceOpt.get.tokenBalanceMap(Address.ZERO.toString).balance.get
          )
          ethBalance == "20".zeros(18)
        })
      )
      getToAddressBalanceReq.expectUntil(
        check((res: GetAccount.Res) => {
          val balanceOpt = res.accountBalance
          val ethBalance = toBigInt(
            balanceOpt.get.tokenBalanceMap(Address.ZERO.toString).balance.get
          )
          ethBalance == "20".zeros(18)
        })
      )

      When("send some transfer events")
      ethTransferPendingActivities(
        account.getAddress,
        to,
        blockNumber,
        txHash,
        "10".zeros(18),
        nonce
      ).foreach(eventDispatcher.dispatch)

      Thread.sleep(1000)

      Then("the each account should query one pending activity")
      GetActivities
        .Req(account.getAddress)
        .expectUntil(
          check((res: GetActivities.Res) => {
            res.activities.length == 1 && res.activities.head.txStatus == TxStatus.TX_STATUS_PENDING
          })
        )
      GetActivities
        .Req(to)
        .expectUntil(
          check((res: GetActivities.Res) => {
            res.activities.length == 1 && res.activities.head.txStatus == TxStatus.TX_STATUS_PENDING
          })
        )

      GetPendingActivityNonce
        .Req(account.getAddress, 2)
        .expectUntil(
          check((res: GetPendingActivityNonce.Res) => {
            res.nonces.head == 11
          })
        )

      When("activities confirmed")
      val blockEvent =
        blockConfirmedEvent(account.getAddress, blockNumber, txHash, nonce)
      ActivityActor.broadcast(blockEvent)
      Thread.sleep(2000)

      ethTransferConfirmedActivities(
        account.getAddress,
        to,
        blockNumber,
        txHash,
        "10".zeros(18),
        nonce,
        "10".zeros(18),
        "30".zeros(18)
      ).foreach(eventDispatcher.dispatch)
      Thread.sleep(1000)

      GetActivities
        .Req(account.getAddress)
        .expectUntil(
          check((res: GetActivities.Res) => {
            res.activities.length == 1 && res.activities.head.txStatus == TxStatus.TX_STATUS_SUCCESS
          })
        )
      GetActivities
        .Req(to)
        .expectUntil(
          check((res: GetActivities.Res) => {
            res.activities.length == 1 && res.activities.head.txStatus == TxStatus.TX_STATUS_SUCCESS
          })
        )

      GetPendingActivityNonce
        .Req(account.getAddress, 2)
        .expectUntil(
          check((res: GetPendingActivityNonce.Res) => {
            res.nonces.head == 11
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
          ethBalance == "10".zeros(18) && ethBalance == ethAvailableBalance
        })
      )
      getToAddressBalanceReq.expectUntil(
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
          ethBalance == "30".zeros(18) && ethBalance == ethAvailableBalance
        })
      )
    }
  }
}
