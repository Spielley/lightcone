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

package org.loopring.lightcone.actors.base

import akka.actor._
import akka.event.LoggingReceive
import akka.util.Timeout
import org.loopring.lightcone.actors.data._
import org.loopring.lightcone.proto.actors._

import scala.concurrent._

trait OrderRecoverySupport {
  actor: Actor with ActorLogging ⇒

  implicit val ec: ExecutionContext
  implicit val timeout: Timeout

  val skipRecovery: Boolean // for testing purpose
  val recoverBatchSize: Int
  val ownerOfOrders: Option[String]
  private var batch = 1

  protected def orderDatabaseAccessActor: ActorRef

  //暂时将recovery更改为同步的
  protected def recoverOrder(xorder: XOrder): Any

  protected def functional: Receive

  protected def startOrderRecovery() = {
    if (skipRecovery) {
      log.info(s"actor recovering skipped: ${self.path}")
      context.become(functional)
    } else {
      context.become(recovering)
      log.info(s"actor recovering started: ${self.path}")
      orderDatabaseAccessActor ! XRecoverOrdersReq(ownerOfOrders.getOrElse(null), 0L, recoverBatchSize)
    }
  }

  def recovering: Receive = {

    case XRecoverOrdersRes(xraworders) ⇒
      log.info(s"recovering batch $batch (size = ${xraworders.size})")
      batch += 1

      val xorders = xraworders.map(xRawOrderToXOrder)
      xorders.foreach(recoverOrder)
      val lastUpdatdTimestamp = xorders.lastOption.map(_.updatedAt).getOrElse(0L)
      val recoverEnded = lastUpdatdTimestamp == 0 || xorders.size < recoverBatchSize
      log.debug(s"${self.path.toString} -- recoverEnded: ${recoverEnded} ")
      orderDatabaseAccessActor ! XRecoverOrdersReq(
        ownerOfOrders.getOrElse(null),
        lastUpdatdTimestamp,
        recoverBatchSize
      )
      if (recoverEnded)
        context.become(functional)
      else
        orderDatabaseAccessActor ! XRecoverOrdersReq(
          ownerOfOrders.getOrElse(null),
          lastUpdatdTimestamp,
          recoverBatchSize
        )

    //      for {
    //        _ ← Future.sequence(xorders.map(recoverOrder))
    //        lastUpdatdTimestamp = xorders.lastOption.map(_.updatedAt).getOrElse(0L)
    //        recoverEnded = lastUpdatdTimestamp == 0 || xorders.size < recoverBatchSize
    //        _ = println(s"###,recoverEnded ${recoverEnded} ")
    //      } yield {
    //        if (recoverEnded)
    //          context.become(functional)
    //        else
    //          orderDatabaseAccessActor ! XRecoverOrdersReq(
    //            ownerOfOrders.getOrElse(null),
    //            lastUpdatdTimestamp,
    //            recoverBatchSize
    //          )
    //      }

    case msg ⇒
      log.debug(s"ignored msg during recovery: ${msg.getClass.getName}")
  }

  def functionalBase: Receive = LoggingReceive {
    case XRecoverOrdersRes(xraworders) ⇒
      log.info(s"recovering last batch (size = ${xraworders.size})")
      xraworders
        .map(xRawOrderToXOrder)
        .foreach(recoverOrder)
  }
}
