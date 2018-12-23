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

package org.loopring.lightcone.core.base

import org.loopring.lightcone.core.data._
import org.loopring.lightcone.proto._

class TokenManager(defaultBurnRate: Double = 0.2) {

  private var addressMap = Map.empty[String, Token]

  def reset(metas: Seq[XTokenMeta]) = this.synchronized {
    addressMap = Map.empty
    metas.foreach(addToken)
  }

  def addToken(meta: XTokenMeta) = this.synchronized {
    addressMap += meta.address -> new Token(meta)
    this
  }

  def addTokens(meta: Seq[XTokenMeta]) = {
    meta.foreach(addToken)
    this
  }

  def hasToken(addr: String) = addressMap.contains(addr)

  def getToken(addr: String) = {
    assert(hasToken(addr), s"token no found for address $addr")
    addressMap.get(addr).get
  }

  def getBurnRate(addr: String) =
    addressMap.get(addr).map(_.meta.burnRate).getOrElse(defaultBurnRate)

  override def toString() = addressMap.toString
}