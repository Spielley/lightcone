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

package org.loopring.lightcone.actors.event

import com.typesafe.config.ConfigFactory
import org.loopring.lightcone.actors.ethereum.event._
import org.loopring.lightcone.actors.support._
import org.loopring.lightcone.ethereum.data.Address
import org.loopring.lightcone.proto.AddressBalanceUpdated

import scala.concurrent._

class EventExtractorSpec
    extends CommonSpec
    with EventExtractorSupport
    with EthereumSupport {

  override def beforeAll() {
    info(s">>>>>> To run this spec, use `testOnly *${getClass.getSimpleName}`")
  }

  "extract all kind of events " must {

    "correctly extract transfers, balance addresses and allowance addresses" in {

      val weth = Address("0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2")
      val selfConfigStr = s"""
                             |weth {
                             |address = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
                             |}
                             |loopring_protocol {
                             |  protocol-address = "0x8d8812b72d1e4ffCeC158D25f56748b7d67c1e78"
                             |  delegate-address = "0x17233e07c67d086464fD408148c3ABB56245FA64"
                             |  gas-limit-per-ring-v2 = "1000000",
                             |    burn-rate-table {
                             |    base = 1000,
                             |    tiers = [
                             |      {
                             |        name = "TIER_1"
                             |        tier = 3,
                             |        rate = 25
                             |      },
                             |      {
                             |        name = "TIER_2"
                             |        tier = 2,
                             |        rate = 150
                             |      },
                             |      {
                             |        name = "TIER_3"
                             |        tier = 1,
                             |        rate = 300
                             |      },
                             |      {
                             |        name = "TIER_4"
                             |        tier = 0,
                             |        rate = 500
                             |      },
                             |    ]
                             |  }
                             |}
     """.stripMargin

      val selfConfig = ConfigFactory.parseString(selfConfigStr)

      val transferExtractor = new TransferEventExtractor()(selfConfig, ec)

      val transfers =
        Await.result(
          transferExtractor.extract(blockData),
          timeout.duration
        )
      val (eths, tokens) = transfers.partition(tr => Address(tr.token).isZero)

      val cutOffExtractor = new CutoffEventExtractor()

      val cutOffs =
        Await.result(
          cutOffExtractor.extract(blockData),
          timeout.duration
        )

      cutOffs.isEmpty should be(true)

      val onChainOrderExtractor = new OnchainOrderExtractor()

      val onChainOrders =
        Await.result(
          onChainOrderExtractor
            .extract(blockData),
          timeout.duration
        )
      onChainOrders.isEmpty should be(true)

      val orderCanceledExtractor = new OrdersCancelledEventExtractor()

      val orderCancelledEvents =
        Await.result(
          orderCanceledExtractor
            .extract(blockData),
          timeout.duration
        )
      orderCancelledEvents.isEmpty should be(true)

      val ringMinedEventExtractor = new RingMinedEventExtractor()

      val rings =
        Await.result(
          ringMinedEventExtractor
            .extract(blockData),
          timeout.duration
        )
      rings.isEmpty should be(true)

      val tokenBurnRateExtractor =
        new TokenBurnRateEventExtractor()(selfConfig, ec)

      val tokenBurnRates =
        Await.result(
          tokenBurnRateExtractor
            .extract(blockData),
          timeout.duration
        )
      tokenBurnRates.isEmpty should be(true)

      val balanceExtractor = new BalanceChangedAddressExtractor

      val balances =
        Await
          .result(
            balanceExtractor.extract(blockData),
            timeout.duration
          )
          .distinct

      val transferBalances = (transfers
        .filter(_.header.get.txStatus.isTxStatusSuccess)
        .flatMap(transfer => {
          Seq(
            AddressBalanceUpdated(transfer.from, transfer.token),
            AddressBalanceUpdated(transfer.to, transfer.token)
          )
        }) ++ blockData.txs.map(
        tx => AddressBalanceUpdated(tx.from, Address.ZERO.toString())
      ) ++ blockData.uncles.+:(blockData.miner).map { miner =>
        AddressBalanceUpdated(miner, Address.ZERO.toString())
      }).distinct.filterNot(ba => Address(ba.address).equals(weth))
      (balances.size == transferBalances.size) should be(true)

      val allowanceExtractor =
        new AllowanceChangedAddressExtractor()(
          selfConfig,
          brb,
          timeout,
          actors,
          ec
        )

      val allowances =
        Await
          .result(allowanceExtractor.extract(blockData), timeout.duration)
          .distinct

      allowances.size should be(2)
    }
  }
}