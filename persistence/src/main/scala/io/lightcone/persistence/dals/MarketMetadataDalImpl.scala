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

package io.lightcone.persistence.dals

import com.google.inject.name.Named
import com.google.inject.Inject
import io.lightcone.core._
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.JdbcProfile
import slick.basic._
import scala.concurrent._
import org.slf4s.Logging
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import io.lightcone.lib.TimeProvider
import io.lightcone.persistence.base.enumColumnType
import scala.util.{Failure, Success}

class MarketMetadataDalImpl @Inject()(
    implicit
    val ec: ExecutionContext,
    @Named("dbconfig-dal-market-metadata") val dbConfig: DatabaseConfig[
      JdbcProfile
    ],
    timeProvider: TimeProvider)
    extends MarketMetadataDal
    with Logging {

  import ErrorCode._

  val query = TableQuery[MarketMetadataTable]
  implicit val statusColumnType = enumColumnType(MarketMetadata.Status)

  def saveMarket(marketMetadata: MarketMetadata): Future[ErrorCode] =
    db.run((query += marketMetadata).asTry).map {
      case Failure(e: MySQLIntegrityConstraintViolationException) =>
        ERR_PERSISTENCE_DUPLICATE_INSERT
      case Failure(ex) =>
        logger.error(s"error : ${ex.getMessage}")
        ERR_PERSISTENCE_INTERNAL
      case Success(x) => ERR_NONE
    }

  def saveMarkets(marketMetadatas: Seq[MarketMetadata]): Future[Seq[String]] =
    for {
      _ <- Future.sequence(marketMetadatas.map(saveMarket))
      query <- getMarketsByKey(marketMetadatas.map(_.marketHash))
    } yield query.map(_.marketHash)

  def updateMarket(marketMetadata: MarketMetadata): Future[ErrorCode] =
    for {
      result <- db.run(query.insertOrUpdate(marketMetadata))
    } yield {
      if (result == 1) {
        ERR_NONE
      } else {
        ERR_PERSISTENCE_INTERNAL
      }
    }

  def getMarkets(): Future[Seq[MarketMetadata]] =
    db.run(query.result)

  def getMarketsByKey(marketHashs: Seq[String]): Future[Seq[MarketMetadata]] =
    db.run(query.filter(_.marketHash inSet marketHashs).result)

  def terminateMarketByKey(marketHash: String): Future[ErrorCode] =
    for {
      result <- db.run(
        query
          .filter(_.marketHash === marketHash)
          .map(c => (c.status, c.updateAt))
          .update(
            MarketMetadata.Status.TERMINATED,
            timeProvider.getTimeMillis()
          )
      )
    } yield {
      if (result >= 1) ERR_NONE
      else ERR_PERSISTENCE_UPDATE_FAILED
    }
}
