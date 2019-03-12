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
import io.lightcone.core._
import io.lightcone.lib.Address
import io.lightcone.persistence.TokenTickerRecord
import io.lightcone.relayer.implicits._

object Metadatas {

  val WETH_TOKEN = TokenMetadata(
    address = Address("0x7Cb592d18d0c49751bA5fce76C1aEc5bDD8941Fc").toString,
    decimals = 18,
    burnRate = Some(BurnRate(0.4, 0.5)),
    symbol = "WETH",
    name = "WETH",
    status = TokenMetadata.Status.VALID
  )

  val LRC_TOKEN = TokenMetadata(
    address = Address("0x97241525fe425C90eBe5A41127816dcFA5954b06").toString,
    decimals = 18,
    burnRate = Some(BurnRate(0.4, 0.5)),
    symbol = "LRC",
    name = "LRC",
    status = TokenMetadata.Status.VALID
  )

  val GTO_TOKEN = TokenMetadata(
    address = Address("0x2D7233F72AF7a600a8EbdfA85558C047c1C8F795").toString,
    decimals = 18,
    burnRate = Some(BurnRate(0.4, 0.5)),
    symbol = "GTO",
    name = "GTO",
    status = TokenMetadata.Status.VALID
  )

  val LRC_WETH_MARKET = MarketMetadata(
    status = MarketMetadata.Status.ACTIVE,
    baseTokenSymbol = LRC_TOKEN.symbol,
    quoteTokenSymbol = WETH_TOKEN.symbol,
    maxNumbersOfOrders = 1000,
    priceDecimals = 6,
    orderbookAggLevels = 6,
    precisionForAmount = 5,
    precisionForTotal = 5,
    browsableInWallet = true,
    marketPair = Some(MarketPair(LRC_TOKEN.address, WETH_TOKEN.address)),
    marketHash =
      MarketHash(MarketPair(LRC_TOKEN.address, WETH_TOKEN.address)).toString
  )

  val GTO_WETH_MARKET = MarketMetadata(
    status = MarketMetadata.Status.ACTIVE,
    baseTokenSymbol = GTO_TOKEN.symbol,
    quoteTokenSymbol = WETH_TOKEN.symbol,
    maxNumbersOfOrders = 500,
    priceDecimals = 6,
    orderbookAggLevels = 5,
    precisionForAmount = 5,
    precisionForTotal = 5,
    browsableInWallet = true,
    marketPair = Some(
      MarketPair(baseToken = GTO_TOKEN.address, quoteToken = WETH_TOKEN.address)
    ),
    marketHash =
      MarketHash(MarketPair(GTO_TOKEN.address, WETH_TOKEN.address)).toString
  )

  val TOKENS = Seq(WETH_TOKEN, LRC_TOKEN, GTO_TOKEN)

  val TOKEN_SLUGS_SYMBOLS = Seq(
    ("ETH", "ethereum"),
    ("BTC", "bitcoin"),
    ("WETH", "weth"),
    ("LRC", "loopring"),
    ("GTO", "gifto"),
    (Currency.RMB.name, Currency.RMB.getSlug()),
    (Currency.JPY.name, Currency.JPY.getSlug()),
    (Currency.EUR.name, Currency.EUR.getSlug()),
    (Currency.GBP.name, Currency.GBP.getSlug())
  )

  val MARKETS = Seq(LRC_WETH_MARKET, GTO_WETH_MARKET)

  val externalTickers = Seq(
    TokenTickerRecord(
      "",
      "BTC",
      "bitcoin",
      3624.66357903,
      6.10383926822598e9,
      0.192219,
      -0.303795,
      6.55865,
      6.357152464717556e10,
      0,
      false,
      "CMC"
    ),
    TokenTickerRecord(
      "0x0000000000000000000000000000000000000000",
      "ETH",
      "ethereum",
      122.020909611,
      3.21120682830794e9,
      0.345868,
      -1.35733,
      16.379,
      1.2795500306946983e10,
      0,
      false,
      "CMC"
    ),
    TokenTickerRecord(
      "0x97241525fe425c90ebe5a41127816dcfa5954b06",
      "LRC",
      "loopring",
      0.0566613345897,
      5372659.102917,
      -0.618224,
      5.29829,
      9.06226,
      4.470491422715433e7,
      0,
      false,
      "CMC"
    ),
    TokenTickerRecord(
      "0x2d7233f72af7a600a8ebdfa85558c047c1c8f795",
      "GTO",
      "gifto",
      0.026678235137,
      1.02527274920197e7,
      0.994498,
      -0.880104,
      7.61296,
      1.4251839270099403e7,
      0,
      false,
      "CMC"
    ),
    TokenTickerRecord(
      "0x7cb592d18d0c49751ba5fce76c1aec5bdd8941fc",
      "WETH",
      "weth",
      117.627070345,
      174597.678357847,
      0.503386,
      11.1752,
      17.0947,
      0.0,
      0,
      false,
      "CMC"
    ),
    TokenTickerRecord(
      symbol = "RMB",
      slug = "loopring-rmb",
      price = 0.1487497,
      isValid = false,
      dataSource = "Sina"
    ),
    TokenTickerRecord(
      symbol = "JPY",
      slug = "loopring-jpy",
      price = 0.00900017,
      isValid = false,
      dataSource = "Sina"
    ),
    TokenTickerRecord(
      symbol = "EUR",
      slug = "loopring-eur",
      price = 1.12334307,
      isValid = false,
      dataSource = "Sina"
    ),
    TokenTickerRecord(
      symbol = "GBP",
      slug = "loopring-gbp",
      price = 1.2973534,
      isValid = false,
      dataSource = "Sina"
    )
  ).map(_.copy(timestamp = timeProvider.getTimeSeconds()))
}