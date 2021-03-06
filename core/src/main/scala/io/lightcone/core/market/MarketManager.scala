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

package io.lightcone.core

object MarketManager {
  case class MatchResult(
      taker: Matchable,
      rings: Seq[MatchableRing] = Nil,
      orderbookUpdate: Orderbook.InternalUpdate = Orderbook.InternalUpdate())
}

trait MarketManager {
  import MarketManager._

  val marketPair: MarketPair
  val pendingRingPool: PendingRingPool

  def getStats(): MarketStats

  def getOrder(orderId: String): Option[Matchable]

  def cancelOrder(orderId: String): Option[Orderbook.InternalUpdate]

  def deleteRing(
      ringId: String,
      ringSettledSuccessfully: Boolean
    ): Seq[MatchResult]

  def deleteRingsBefore(timestamp: Long): Seq[MatchResult]
  def deleteRingsOlderThan(ageInSeconds: Long): Seq[MatchResult]

  def getSellOrders(
      num: Int,
      skip: Int = 0
    ): Seq[Matchable]

  def getBuyOrders(
      num: Int,
      skip: Int = 0
    ): Seq[Matchable]

  def getNumOfOrders(): Int
  def getNumOfBuyOrders(): Int
  def getNumOfSellOrders(): Int

  def submitOrder(
      order: Matchable,
      minFiatValue: Double = 0
    ): MatchResult

  def triggerMatch(
      sellOrderAsTaker: Boolean,
      minFiatValue: Double,
      offset: Int = 0
    ): Option[MatchResult]
}
