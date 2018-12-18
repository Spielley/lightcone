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
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import org.loopring.lightcone.actors.base.MapBasedLookup
import org.loopring.lightcone.lib.SystemTimeProvider
import org.scalatest._
import org.slf4s.Logging
import akka.pattern._
import org.loopring.lightcone.ethereum.abi.{
  AllowanceFunction,
  BalanceOfFunction,
  TransferFunction,
  WETHABI
}
import org.loopring.lightcone.proto._
import org.web3j.utils.Numeric

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class EthereumAccessorSpec extends FlatSpec with Matchers with Logging {

  implicit val system = ActorSystem("Lightcone")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()(system)
  implicit val timeout = Timeout(2 second)
  implicit val timeProvider = new SystemTimeProvider()
  implicit val actors = new MapBasedLookup[ActorRef]()
  implicit val config: Config = ConfigFactory.load()

  val wethAbi = WETHABI()
  val delegateAdderess = "0x17233e07c67d086464fD408148c3ABB56245FA64"

  val ethereumAccessActor =
    system.actorOf(Props(new EthereumAccessActor()))

  Thread.sleep(6000)

  val fu = for {
    blockNum ← (ethereumAccessActor ? XEthBlockNumberReq())
      .mapTo[XEthBlockNumberRes]
      .map(_.result)
    blockWithTxHash ← (ethereumAccessActor ? XGetBlockWithTxHashByNumberReq(
      blockNum
    )).mapTo[XGetBlockWithTxHashByNumberRes]
      .map(_.result.get)
    blockWithTxObjcet ← (ethereumAccessActor ? XGetBlockWithTxObjectByNumberReq(
      blockNum
    )).mapTo[XGetBlockWithTxObjectByNumberRes]
      .map(_.result.get)
    blockByHashWithHash ← (ethereumAccessActor ? XGetBlockWithTxHashByHashReq(
      blockWithTxHash.hash
    )).mapTo[XGetBlockWithTxHashByHashRes]
      .map(_.result.get)
    blockByHashwithObject ← (ethereumAccessActor ? XGetBlockWithTxObjectByHashReq(
      blockWithTxHash.hash
    )).mapTo[XGetBlockWithTxObjectByHashRes]
      .map(_.result.get)
    txs ← Future.sequence(blockWithTxHash.transactions.take(1).map { hash ⇒
      (ethereumAccessActor ? XGetTransactionByHashReq(hash))
        .mapTo[XGetTransactionByHashRes]
        .map(_.result.get)
    })
    receipts ← Future
      .sequence(blockWithTxHash.transactions.take(1).map { hash ⇒
        (ethereumAccessActor ? XGetTransactionReceiptReq(hash))
          .mapTo[XGetTransactionReceiptRes]
          .map(_.result.get)
      })
    batchReceipts ← (ethereumAccessActor ? XBatchGetTransactionReceiptsReq(
      blockWithTxHash.transactions.map(XGetTransactionReceiptReq(_))
    )).mapTo[XBatchGetTransactionReceiptsRes]
      .map(_.resps.map(_.result.get))
    nonce ← (ethereumAccessActor ? XGetNonceReq(
      owner = "0xdce9e65ba38d4249c38d00d664d41e5f6d7e83b3",
      tag = "latest"
    )).mapTo[XGetNonceRes]
      .map(_.result)
    txCount ← (ethereumAccessActor ? XGetBlockTransactionCountReq(
      blockWithTxHash.hash
    )).mapTo[XGetBlockTransactionCountRes]
      .map(_.result)
    batchTx ← (ethereumAccessActor ? XBatchGetTransactionsReq(
      blockWithTxHash.transactions.map(XGetTransactionByHashReq(_))
    )).mapTo[XBatchGetTransactionsRes]
      .map(_.resps.map(_.result))
    lrcBalance ← (ethereumAccessActor ? XEthCallReq(tag = "latest")
      .withParam(
        XTransactionParam()
          .withData(
            wethAbi.balanceOf.pack(
              BalanceOfFunction
                .Parms("0xb94065482ad64d4c2b9252358d746b39e820a582")
            )
          )
          .withTo("0xef68e7c694f40c8202821edf525de3782458639f")
      ))
      .mapTo[XEthCallRes]
      .map(resp ⇒ wethAbi.balanceOf.unpackResult(resp.result))
    lrcbalances ← (ethereumAccessActor ? XBatchContractCallReq(
      batchTx.map(
        tx ⇒
          XEthCallReq(tag = "latest")
            .withParam(
              XTransactionParam()
                .withData(
                  wethAbi.balanceOf.pack(
                    BalanceOfFunction
                      .Parms(tx.get.from)
                  )
                )
                .withTo("0xef68e7c694f40c8202821edf525de3782458639f")
            )
      )
    )).mapTo[XBatchContractCallRes]
      .map(_.resps.map(res ⇒ wethAbi.balanceOf.unpackResult(res.result)))

    allowance ← (ethereumAccessActor ? XEthCallReq(tag = "latest")
      .withParam(
        XTransactionParam()
          .withData(
            wethAbi.allowance.pack(
              AllowanceFunction
                .Parms(
                  _owner = "0xb94065482ad64d4c2b9252358d746b39e820a582",
                  _spender = delegateAdderess
                )
            )
          )
          .withTo("0xef68e7c694f40c8202821edf525de3782458639f")
      ))
      .mapTo[XEthCallRes]
      .map(resp ⇒ wethAbi.allowance.unpackResult(resp.result))

    allowances ← (ethereumAccessActor ? XBatchContractCallReq(
      batchTx.map(
        tx ⇒
          XEthCallReq(tag = "latest")
            .withParam(
              XTransactionParam()
                .withData(
                  wethAbi.allowance.pack(
                    AllowanceFunction
                      .Parms(tx.get.from, _spender = delegateAdderess)
                  )
                )
                .withTo("0xef68e7c694f40c8202821edf525de3782458639f")
            )
      )
    )).mapTo[XBatchContractCallRes]
      .map(_.resps.map(res ⇒ wethAbi.allowance.unpackResult(res.result)))
    uncle ← (ethereumAccessActor ? XGetUncleByBlockNumAndIndexReq(
      blockNum = "0x69555e",
      index = "0x0"
    )).mapTo[XGetBlockWithTxHashByHashRes]
      .map(_.result.get)
    uncles ← (ethereumAccessActor ? XBatchGetUncleByBlockNumAndIndexReq()
      .withReqs(
        Seq(
          XGetUncleByBlockNumAndIndexReq(
            blockNum = "0x69555e",
            index = "0x0"
          )
        )
      ))
      .mapTo[XBatchGetUncleByBlockNumAndIndexRes]
      .map(_.resps.map(_.result.get))
    gas ← (ethereumAccessActor ? XGetEstimatedGasReq(
      to = "0xef68e7c694f40c8202821edf525de3782458639f"
    ).withData(
      wethAbi.transfer.pack(
        TransferFunction.Parms(
          to = "0xb94065482ad64d4c2b9252358d746b39e820a582",
          amount = BigInt("10000")
        )
      )
    )).mapTo[XGetEstimatedGasRes]
      .map(_.result)
  } yield {
    //    println(
    //      s"BlockNum: ${blockNum} --- ${Numeric.toBigInt(blockNum).intValue()}"
    //    )
    //    println(s"txHashs:${blockWithTxHash}")
    //    println(s"txHashs:${blockByHashWithHash}")
    //    println(s"Txs:${blockWithTxObjcet}")
    //    println(s"Txs:${blockByHashwithObject}")
    //    println(s"Txs:${txs}")
    //    println(s"Receipts:${receipts}")
    //    println(s"Receipts:${batchReceipts.map(_.from)}")
    //    println(s"nonce:${nonce}")
    //    println(s"TxCount:${txCount}")
    //    println(s"txs:${batchTx.map(_.get.from)}")
    //    println(s"Lrc Balance:${lrcBalance.get.balance.toString()}")
    //    println(s"balances:${lrcbalances.map(_.get.balance.toString)}")
    //    println(s"Allowance:${allowance.get.allowance.toString}")
    //    println(s"Allowances:${allowances.map(_.get.allowance.toString)}")
    //    println(s"Uncle:${uncle}")
    //    println(s"Uncles:${uncles}")
    println(s"Gas:$gas")
    println("test success")
  }

  Await.result(fu, 2 minute)

}
