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

package org.loopring.lightcone.ethereum.abi

import org.loopring.lightcone.ethereum.data._
import org.web3j.utils.Numeric
import org.scalatest._

class ERC20ABISpec extends FlatSpec with Matchers {

  val erc20abi = ERC20ABI()

  "encodeTransferFunction" should "encode class Parms of Transfer to function input" in {
    info("[sbt ethereum/'testOnly *ERC20ABISpec -- -z encodeTransferFunction']")
    val parms = TransferFunction.Parms(to = "0xf105c622edc68b9e4e813e631cb534940f5cc509", amount = BigInt("29558242000000000000000"))
    val input = erc20abi.transfer.pack(parms)
    println(Numeric.toHexString(input))
    Numeric.toHexString(input) should be("0xa9059cbb000000000000000000000000f105c622edc68b9e4e813e631cb534940f5cc5090000000000000000000000000000000000000000000006425b02acb8d7bd0000")
  }

  "decodeTransferFunction" should "decode function input and assemble to class Transfer" in {
    info("[sbt ethereum/'testOnly *ERC20ABISpec -- -z decodeTransferFunction']")

    val from = "0x0681d8db095565fe8a346fa0277bffde9c0edbbf"
    val input = "0xa9059cbb000000000000000000000000f105c622edc68b9e4e813e631cb534940f5cc5090000000000000000000000000000000000000000000006425b02acb8d7bd0000"
    val transfer = erc20abi.transfer.unpackInput(Numeric.hexStringToByteArray(input))
    println(transfer)
  }

  "decodeTransferEvent" should "decode event data and assemble to class Transfer" in {
    info("[sbt ethereum/'testOnly *ERC20ABISpec -- -z decodeTransferEvent']")

    val from = "0x0681d8db095565fe8a346fa0277bffde9c0edbbf"
    val input = Numeric.hexStringToByteArray("0xa9059cbb000000000000000000000000f105c622edc68b9e4e813e631cb534940f5cc5090000000000000000000000000000000000000000000006425b02acb8d7bd0000")
    val data = Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000006425b02acb8d7bd0000")
    val topics = Seq(
      Numeric.hexStringToByteArray("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"),
      Numeric.hexStringToByteArray("0x0000000000000000000000000681d8db095565fe8a346fa0277bffde9c0edbbf"),
      Numeric.hexStringToByteArray("0x000000000000000000000000f105c622edc68b9e4e813e631cb534940f5cc509")
    )

    val transferOpt = erc20abi.transferEvent.unpack(data, topics.toArray)
    transferOpt match {
      case None ⇒
      case Some(transfer) ⇒
        transfer.sender should be("0x0681d8db095565fe8a346fa0277bffde9c0edbbf")
        transfer.receiver should be("0xf105c622edc68b9e4e813e631cb534940f5cc509")
        transfer.amount should be(BigInt("29558242000000000000000"))
    }
  }
  //
  "decodeApproveFunction" should "decode function input and assemble to class Approve" in {
    info("[sbt ethereum/'testOnly *ERC20ABISpec -- -z decodeApproveFunction']")

    val from = "0x85194623225c1a0576abf8e2bdc0951351fcddda"
    val input = Numeric.hexStringToByteArray("0x095ea7b30000000000000000000000008fd3121013a07c57f0d69646e86e7a4880b467b70000000000000000000000000000000000000000004a817c7ffffffb57e83800")
    val approveOpt = erc20abi.approve.unpackInput(input)

    approveOpt match {
      case None ⇒
      case Some(approve) ⇒
        approve.spender should be("0x8fd3121013a07c57f0d69646e86e7a4880b467b7")
        approve.amount should be(BigInt("90071992547409900000000000"))
    }
  }

  "decodeApproveEvent" should "decode event data and assemble to class Approve" in {
    info("[sbt ethereum/'testOnly *ERC20ABISpec -- -z decodeApproveEvent']")

    val from = "0x85194623225c1a0576abf8e2bdc0951351fcddda"
    val input = "0x095ea7b30000000000000000000000008fd3121013a07c57f0d69646e86e7a4880b467b70000000000000000000000000000000000000000004a817c7ffffffb57e83800"
    val data = Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000004a817c7ffffffb57e83800")
    val topics = Array(
      Numeric.hexStringToByteArray("0x8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925"),
      Numeric.hexStringToByteArray("0x00000000000000000000000085194623225c1a0576abf8e2bdc0951351fcddda"),
      Numeric.hexStringToByteArray("0x0000000000000000000000008fd3121013a07c57f0d69646e86e7a4880b467b7")
    )

    val approveOpt = erc20abi.approvalEvent.unpack(data, topics)

    approveOpt match {
      case None ⇒
      case Some(approve) ⇒
        approve.owner should be("0x85194623225c1a0576abf8e2bdc0951351fcddda")
        approve.spender should be("0x8fd3121013a07c57f0d69646e86e7a4880b467b7")
        approve.amount.toString() should be("90071992547409900000000000")
    }
  }

}
