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

package io.lightcone.persistence

import com.google.inject.Inject
import io.lightcone.lib.cache._
import io.lightcone.persistence.dals._
import io.lightcone.core._
import io.lightcone.ethereum._
import scala.concurrent.{ExecutionContext, Future}

class BlockServiceImpl @Inject()(
    implicit
    blockDal: BlockDal,
    basicCache: Cache[String, Array[Byte]],
    val ec: ExecutionContext)
    extends BlockService {

  def saveBlock(block: BlockData): Future[ErrorCode] =
    blockDal.saveBlock(block)

  def findByHash(hash: String): Future[Option[BlockData]] =
    blockDal.findByHash(hash)

  def findByHeight(height: Long): Future[Option[BlockData]] =
    blockDal.findByHeight(height)

  def findMaxHeight(): Future[Option[Long]] = blockDal.findMaxHeight()

  def findBlocksInHeightRange(
      heightFrom: Long,
      heightTo: Long
    ): Future[Seq[(Long, String)]] =
    blockDal.findBlocksInHeightRange(heightFrom, heightTo)

  def count(): Future[Int] = blockDal.count()

  def obsolete(height: Long): Future[Unit] = blockDal.obsolete(height)
}
