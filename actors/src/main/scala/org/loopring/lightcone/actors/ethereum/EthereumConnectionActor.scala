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

package org.loopring.lightcone.actors.ethereum

import akka.actor._
import akka.routing._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.loopring.lightcone.proto.actors._

import scala.concurrent.ExecutionContextExecutor

class EthereumConnectionActor(
    settings: XEthereumProxySettings
)(
    implicit
    materilizer: ActorMaterializer,
    timeout: Timeout,
    ec: ExecutionContextExecutor
) extends Actor
  with ActorLogging {

  private var monitor: ActorRef = _
  private var router: ActorRef = _
  private var connectorGroups: Seq[ActorRef] = Nil
  private var currentSettings: Option[XEthereumProxySettings] = None

  updateSettings(settings)

  def receive: Receive = {
    case settings: XEthereumProxySettings ⇒
      updateSettings(settings)

    case req ⇒
      router.forward(req)
  }

  def updateSettings(settings: XEthereumProxySettings) {
    if (router != null) {
      context.stop(router)
    }
    connectorGroups.foreach(context.stop)

    connectorGroups = settings.nodes.zipWithIndex.map {
      case (node, index) ⇒
        val ipc = node.ipcPath.nonEmpty

        val nodeName =
          if (ipc) s"ethereum_connector_ipc_$index"
          else s"ethereum_connector_http_$index"

        val props =
          if (ipc) Props(new IpcConnector(node))
          else Props(new HttpConnector(node))

        context.actorOf(
          RoundRobinPool(
            settings.poolSize
          ).props(props),
          nodeName
        )
    }

    router = context.actorOf(
      Props(new EthereumServiceRouter(connectorGroups.map(_.path.toString))),
      "r_ethereum_connector"
    )

    monitor = context.actorOf(
      Props(
        new EthereumClientMonitor(
          router,
          connectorGroups,
          settings.checkIntervalSeconds
        )
      ),
      "ethereum_connector_monitor"
    )

    currentSettings = Some(settings)
  }
}

