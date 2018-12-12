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

package org.loopring.lightcone.actors.utils

import akka.actor._
import akka.cluster.sharding._
import akka.event.LoggingReceive
import akka.pattern._
import akka.util.Timeout
import org.loopring.lightcone.proto._

class BadMessageListener extends Actor with ActorLogging {
  def receive = {
    case u: UnhandledMessage ⇒
      log.debug(s"invalid request: $u")
      sender ! XError(code = XErrorCode.ERR_INVALID_REQ, message = "invalid request")

    case d: DeadLetter ⇒
      log.warning(s"failed to handle request: $d")
      sender ! XError(code = XErrorCode.ERR_FAILED_HANDLE_MES, message = "failed to handle request")
  }
}