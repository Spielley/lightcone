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

package org.loopring.lightcone.actors.core

import akka.actor._
import akka.cluster.sharding._
import akka.event.LoggingReceive
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.Config
import org.loopring.lightcone.lib._
import org.loopring.lightcone.actors._
import org.loopring.lightcone.actors.base._
import org.loopring.lightcone.actors.data._
import org.loopring.lightcone.core.account._
import org.loopring.lightcone.core.base._
import org.loopring.lightcone.core.data.Order
import org.loopring.lightcone.persistence.service.OrderService
import org.loopring.lightcone.proto.XErrorCode._
import org.loopring.lightcone.proto.XOrderStatus._
import org.loopring.lightcone.proto._

import scala.concurrent._

// main owner: 杜永丰
object DatabaseQueryActor extends ShardedEvenly {
  val name = "database_query"

  def startShardRegion()(implicit
    system: ActorSystem,
    config: Config,
    ec: ExecutionContext,
    timeProvider: TimeProvider,
    timeout: Timeout,
    actors: Lookup[ActorRef],
    orderService: OrderService
  ): ActorRef = {

    val selfConfig = config.getConfig(name)
    numOfShards = selfConfig.getInt("num-of-shards")
    entitiesPerShard = selfConfig.getInt("entities-per-shard")

    ClusterSharding(system).start(
      typeName = name,
      entityProps = Props(new DatabaseQueryActor()),
      settings = ClusterShardingSettings(system).withRole(name),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId
    )
  }
}

class DatabaseQueryActor()(
    implicit
    val config: Config,
    val ec: ExecutionContext,
    val timeProvider: TimeProvider,
    val timeout: Timeout,
    val actors: Lookup[ActorRef],
    val orderService: OrderService
) extends ActorWithPathBasedConfig(DatabaseQueryActor.name) {

  def receive: Receive = LoggingReceive {
    case req: XGetOrdersReq ⇒
      (for {
        result ← req.market match {
          //TODO du:等order service合并后，改为按单个owner查询的接口，改1l为value
          case XGetOrdersReq.Market.MarketHash(value) ⇒ orderService.getOrdersForUser(Set.empty, Set(req.owner),
            Set.empty, Set.empty, Set(1l), Set.empty, Some(req.sort), req.skip)
          case XGetOrdersReq.Market.Pair(value) ⇒ orderService.getOrdersForUser(Set.empty, Set(req.owner),
            Set(value.tokenS), Set(value.tokenB), Set.empty, Set.empty, Some(req.sort), req.skip)
        }
      } yield result) pipeTo sender
    case _ ⇒ ;
  }

}
