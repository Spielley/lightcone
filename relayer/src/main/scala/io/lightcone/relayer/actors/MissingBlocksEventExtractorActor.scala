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

package io.lightcone.relayer.actors

import akka.actor._
import akka.util.Timeout
import com.typesafe.config.Config
import io.lightcone.ethereum.event._
import io.lightcone.ethereum.extractor._
import io.lightcone.relayer.base._
import io.lightcone.relayer.ethereum._
import io.lightcone.lib.{NumericConversion, TimeProvider}
import io.lightcone.persistence.DatabaseModule
import io.lightcone.relayer.data._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object MissingBlocksEventExtractorActor extends DeployedAsSingleton {
  val name = "missing_blocks_event_extractor"

  def start(
      implicit
      system: ActorSystem,
      config: Config,
      ec: ExecutionContext,
      timeProvider: TimeProvider,
      timeout: Timeout,
      actors: Lookup[ActorRef],
      dbModule: DatabaseModule,
      eventDispatcher: EventDispatcher,
      eventExtractor: EventExtractor[BlockWithTxObject, AnyRef],
      deployActorsIgnoringRoles: Boolean
    ): ActorRef = {
    startSingleton(Props(new MissingBlocksEventExtractorActor()))
  }

}

//TODO: 补全的事件需要单独处理，可能会与分叉事件冲突
class MissingBlocksEventExtractorActor(
    implicit
    val config: Config,
    val ec: ExecutionContext,
    val timeout: Timeout,
    val actors: Lookup[ActorRef],
    val eventDispatcher: EventDispatcher,
    val eventExtractor: EventExtractor[BlockWithTxObject, AnyRef],
    val dbModule: DatabaseModule)
    extends InitializationRetryActor
    with EventExtraction {

  val selfConfig = config.getConfig(MissingBlocksEventExtractorActor.name)
  val NEXT_RANGE = Notify("next_range")
  var sequenceId = 0L
  val delayInSeconds = selfConfig.getLong("delay-in-seconds")

  var untilBlock: Long = 0L //初始化为0，开始不需要获取区块

  override def initialize() = Future.successful {
    becomeReady()
    self ! NEXT_RANGE
  }

  def ready: Receive = handleMessage orElse {
    case NEXT_RANGE =>
      for {
        missingBlocksOpt <- dbModule.missingBlocksRecordDal.getOldestOne()
        lastBlockData <- if (missingBlocksOpt.isDefined && missingBlocksOpt.get.lastHandledBlock >= 0)
          getBlockData(missingBlocksOpt.get.lastHandledBlock)
        else Future.successful(None)
      } yield {
        if (missingBlocksOpt.isDefined) {
          if (missingBlocksOpt.get.lastHandledBlock >= 0)
            blockData = lastBlockData.get
          else
            blockData = BlockWithTxObject(number = BigInt(-1))
          val missingBlocks = missingBlocksOpt.get
          untilBlock = missingBlocks.blockEnd
          sequenceId = missingBlocks.sequenceId
          self ! GET_BLOCK
        } else {
          context.system.scheduler
            .scheduleOnce(delayInSeconds seconds, self, NEXT_RANGE)
        }
      }
  }

  def handleBlockReorganization: Receive = {
    case BLOCK_REORG_DETECTED =>
    //This Actor will never receive this message
  }

  override def postProcessEvents = {
    val blockNumber = NumericConversion.toBigInt(blockData.number).longValue()
    for {
      _ <- dbModule.missingBlocksRecordDal
        .updateProgress(sequenceId, blockNumber)
      needDelete = blockNumber >= untilBlock
      _ <- if (!needDelete) Future.unit
      else dbModule.missingBlocksRecordDal.deleteRecord(sequenceId)
      _ = if (needDelete) {
        self ! NEXT_RANGE
      }
    } yield Unit
  }

  override def unSupportedEvents: Seq[Class[_]] = Seq(classOf[BlockEvent])

}
