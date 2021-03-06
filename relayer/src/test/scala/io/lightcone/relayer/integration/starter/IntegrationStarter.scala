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

package io.lightcone.relayer.integration.starter

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import com.google.inject.{Guice, Injector}
import com.typesafe.config.ConfigFactory
import io.lightcone.core.MarketMetadata.Status._
import io.lightcone.core.MetadataManager
import io.lightcone.lib.TimeProvider
import io.lightcone.persistence._
import io.lightcone.relayer.CoreModule
import io.lightcone.relayer.actors._
import io.lightcone.relayer.base.Lookup
import io.lightcone.relayer.data._
import io.lightcone.relayer.integration._
import io.lightcone.relayer.integration.Metadatas._
import io.lightcone.relayer.integration.helper._
import net.codingwell.scalaguice.InjectorExtensions._
import org.rnorth.ducttape.TimeoutException
import org.rnorth.ducttape.unreliables.Unreliables
import org.testcontainers.containers.ContainerLaunchException

import scala.concurrent._

class IntegrationStarter extends MockHelper with DbHelper with MetadataHelper {

  var injector: Injector = _
  var actors: Lookup[ActorRef] = _

  def starting(
    )(
      implicit
      timeout: Timeout,
      timeProvider: TimeProvider
    ) = {
    setDefaultEthExpects()
    val anotherPortConfigStr =
      """
        |akka {
        | remote {
        |    netty.tcp {
        |      hostname = "127.0.0.1"
        |      port = 9095
        |    }
        |  }
        |  cluster {
        |    seed-nodes = ["akka.tcp://Lightcone@127.0.0.1:9095"]
        |  }
        |}
        |jsonrpc {
        |  http {
        |     host = "0.0.0.0"
        |     port = 8085
        |  }
        |}
        |socketio {
        |  host:"0.0.0.0",
        |  port:9099
        |}
      """.stripMargin
    val config = ConfigFactory
      .parseString(anotherPortConfigStr)
      .withFallback(ConfigFactory.load())
    injector = Guice.createInjector(new CoreModule(config, true))
    implicit val dbModule = injector.instance[DatabaseModule]
    implicit val metadataManager = injector.instance[MetadataManager]
    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

    prepareDbModule(dbModule)
    metadataManager.reset(Seq.empty, Seq.empty)
    prepareMetadata(TOKENS, MARKETS, TOKEN_SLUGS_SYMBOLS)

    injector
      .instance[CoreDeployerForTest]
      .deploy()

    actors = injector.instance[Lookup[ActorRef]]

    Thread.sleep(3000) //waiting for system
    waiting()
  }

  def waiting(
    )(
      implicit
      metadataManager: MetadataManager,
      timeout: Timeout,
      ec: ExecutionContext
    ) = {
    //waiting for market

    try Unreliables.retryUntilTrue(
      10,
      TimeUnit.SECONDS,
      () => {
        val f =
          Future.sequence(metadataManager.getMarkets(ACTIVE, READONLY).map {
            meta =>
              val marketPair = meta.getMetadata.marketPair.get
              actors.get(MarketManagerActor.name) ? Notify(
                KeepAliveActor.NOTIFY_MSG,
                s"${marketPair.baseToken}-${marketPair.quoteToken}"
              )
          })
        val res =
          Await.result(f, timeout.duration)
        res.nonEmpty
      }
    )
    catch {
      case e: TimeoutException =>
        throw new ContainerLaunchException(
          "Timed out waiting for marketMangerActor init.)"
        )
    }
    //waiting for orderbookmanager
    try Unreliables.retryUntilTrue(
      10,
      TimeUnit.SECONDS,
      () => {
        val f =
          Future.sequence(metadataManager.getMarkets(ACTIVE, READONLY).map {
            meta =>
              val marketPair = meta.getMetadata.marketPair.get
              val orderBookInit = GetOrderbook.Req(0, 100, Some(marketPair))
              actors.get(OrderbookManagerActor.name) ? orderBookInit
          })
        val res =
          Await.result(f.mapTo[Seq[GetOrderbook.Res]], timeout.duration)
        res.nonEmpty
      }
    )
    catch {
      case e: TimeoutException =>
        throw new ContainerLaunchException(
          "Timed out waiting for orderbookManger init.)"
        )
    }

    //waiting activity
    try Unreliables.retryUntilTrue(
      10,
      TimeUnit.SECONDS,
      () => {
        val f =
          (actors.get(ActivityActor.name) ? GetActivities.Req(
            paging = Some(CursorPaging(size = 10))
          )).mapTo[GetActivities.Res]
        val res = Await.result(f, timeout.duration)
        res.activities.isEmpty || res.activities.nonEmpty
      }
    )
    catch {
      case e: TimeoutException =>
        throw new ContainerLaunchException(
          "Timed out waiting for MetadataManagerActor init.)"
        )
    }

    //waiting for metadata
    try Unreliables.retryUntilTrue(
      10,
      TimeUnit.SECONDS,
      () => {
        val f = (actors.get(MetadataRefresher.name) ? GetTokens.Req())
          .mapTo[GetTokens.Res]
        val res = Await.result(f, timeout.duration)
        res.tokens.nonEmpty
        true
      }
    )
    catch {
      case e: TimeoutException =>
        throw new ContainerLaunchException(
          "Timed out waiting for MetadataRefresher init.)"
        )
    }

    try Unreliables.retryUntilTrue(
      10,
      TimeUnit.SECONDS,
      () => {
        val f = (actors.get(MetadataRefresher.name) ? GetTokens.Req())
          .mapTo[GetTokens.Res]
        val res = Await.result(f, timeout.duration)
        res.tokens.nonEmpty
        true
      }
    )
    catch {
      case e: TimeoutException =>
        throw new ContainerLaunchException(
          "Timed out waiting for MetadataRefresher init.)"
        )
    }
    //waiting for accountmanager

    //waiting for ethereum

  }

}
