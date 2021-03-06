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

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import io.lightcone.ethereum._
import io.lightcone.ethereum.extractor._
import io.lightcone.ethereum.event.BlockEvent
import io.lightcone.lib._
import io.lightcone.persistence._
import io.lightcone.relayer.base._
import io.lightcone.relayer.data._
import io.lightcone.relayer.ethereum._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util._

trait EventExtraction {
  me: InitializationRetryActor =>
  implicit val timeout: Timeout
  implicit val actors: Lookup[ActorRef]
  implicit val eventExtractor: EventExtractor[BlockWithTxObject, AnyRef]
  implicit val eventDispatcher: EventDispatcher

  implicit val dbModule: DatabaseModule

  var preBlockData: BlockWithTxObject = _
  var blockData: BlockWithTxObject = _

  val maxRetryTimes = 10
  var retryTimes = 0

  val GET_BLOCK = Notify("get_block")
  val RETRIEVE_RECEIPTS = Notify("retrieve_receipts")
  val PROCESS_EVENTS = Notify("process_events")
  val BLOCK_REORG_DETECTED = Notify("block_reorg_detected")

  var untilBlock: Long

  @inline def ethereumAccessorActor = actors.get(EthereumAccessActor.name)

  def handleMessage: Receive = handleBlockReorganization orElse {
    case GET_BLOCK =>
      assert(blockData != null)

      getBlockData(NumericConversion.toBigInt(blockData.number) + 1).map {
        case Some(block) =>
          if (block.parentHash == blockData.hash || NumericConversion
                .toBigInt(blockData.number) == -1) {
            preBlockData = blockData
            blockData = block
            self ! RETRIEVE_RECEIPTS
          } else {
            self ! BLOCK_REORG_DETECTED
          }
        case None =>
          context.system.scheduler
            .scheduleOnce(1 seconds, self, GET_BLOCK)
      }
    case RETRIEVE_RECEIPTS =>
      for {
        receipts <- getAllReceipts
      } yield {
        if (receipts.forall(_.nonEmpty)) {
          blockData = blockData.withReceipts(receipts.map(_.get))
          self ! PROCESS_EVENTS
        } else if (retryTimes < maxRetryTimes) {
          retryTimes += 1
          context.system.scheduler
            .scheduleOnce(500 millis, self, RETRIEVE_RECEIPTS)
        } else {
          retryTimes = 0
          blockData = preBlockData
          self ! GET_BLOCK
        }
      }
    case PROCESS_EVENTS =>
      processEvents onComplete {
        case Success(_) =>
          if (NumericConversion.toBigInt(blockData.number) < untilBlock)
            self ! GET_BLOCK
        case Failure(e) =>
          log.error(
            s" Actor: ${self.path} extracts ethereum events failed with error:${e.getMessage}"
          )
      }
  }

  def handleBlockReorganization: Receive

  def getBlockData(blockNum: BigInt): Future[Option[BlockWithTxObject]] = {
    for {
      blockOpt <- (ethereumAccessorActor ? GetBlockWithTxObjectByNumber.Req(
        blockNum
      )).mapAs[GetBlockWithTxObjectByNumber.Res]
        .map(_.result)
      uncleMiners <- if (blockOpt.isDefined && blockOpt.get.uncles.nonEmpty) {
        val batchGetUnclesReq = BatchGetUncle.Req(
          blockOpt.get.uncles.indices
            .map(index => GetUncle.Req(blockOpt.get.number, BigInt(index)))
        )

        (ethereumAccessorActor ? batchGetUnclesReq)
          .mapAs[BatchGetUncle.Res]
          .map(_.resps.map(_.result.get.miner))
      } else {
        Future.successful(Seq.empty)
      }
      rawBlock = blockOpt.map(block => block.copy(uncleMiners = uncleMiners))
    } yield rawBlock
  }

  def getAllReceipts: Future[Seq[Option[TransactionReceipt]]] =
    (ethereumAccessorActor ? BatchGetTransactionReceipts.Req(
      blockData.transactions
        .map(tx => GetTransactionReceipt.Req(tx.hash))
    )).mapAs[BatchGetTransactionReceipts.Res]
      .map(_.resps.map(_.result))

  def processEvents: Future[Unit] = {
    for {
      events_ <- eventExtractor.extractEvents(blockData)
      events = if (unSupportedEvents.isEmpty) events_
      else events_.filterNot(evt => unSupportedEvents.contains(evt.getClass))
      (blockEvent, otherEvents) = events.partition(_.isInstanceOf[BlockEvent])
      //TODO: 首先发送BlockEvent，如何发送，是否需要等待返回结果之后再发送其余的events，否则会导致数据不一致
      _ = blockEvent.foreach(eventDispatcher.dispatch)
      _ = otherEvents.foreach(eventDispatcher.dispatch)
      _ <- dbModule.blockService.saveBlock(
        BlockData(
          hash = blockData.hash,
          height = NumericConversion.toBigInt(blockData.number).longValue(),
          timestamp = NumericConversion.toBigInt(blockData.timestamp).longValue
        )
      )
      _ <- postProcessEvents()
    } yield Unit
  }

  def postProcessEvents(): Future[Unit] = Future.unit

  def unSupportedEvents: Seq[Class[_]] =
    Seq.empty //MissingBlocksEventExtractorActor与EthereumEventExtractorActor需要处理的事件不一致，有些事件不需要Miss的发送

}
