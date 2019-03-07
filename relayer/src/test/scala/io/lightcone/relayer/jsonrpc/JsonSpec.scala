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

package io.lightcone.relayer.jsonrpc

import io.lightcone.relayer.support.CommonSpec
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.native.JsonMethods.parse

class JsonSpec extends CommonSpec {

  "merge json" must {
    "merge into json correctly" in {

      implicit val formats = DefaultFormats

      val schema = config.getString("order_cancel.schema")

      val message = Map(
        "id" -> "0x0",
        "owner" -> "0xb94065482ad64d4c2b9252358d746b39e820a582",
        "market" -> "0x0",
        "time" -> "0x0"
      )

      val merged = parse(schema.stripMargin) merge render(
        Map("message" -> message)
      )

      println(compact(merged))

    }
  }

}
