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

package io.lightcone.ethereum.abi

import java.util.Arrays

import org.apache.commons.collections4.Predicate
import org.ethereum.solidity.Abi
import org.web3j.utils.Numeric

import scala.annotation.StaticAnnotation
import scala.collection.JavaConverters._
import scala.reflect.Manifest

case class ContractAnnotation(
    name: String,
    idx: Int)
    extends StaticAnnotation

trait AbiFunction[P, R] {
  val entry: Abi.Function

  //与原函数区分，使用pack与unpack
  def pack(t: P)(implicit m: Manifest[P]): String = {
    val inputs = Serialization.serialize(t)
    Numeric.toHexString(entry.encode(inputs: _*))
  }

  def unpackInput(data: String)(implicit m: Manifest[P]): Option[P] = {
    val dataBytes = Numeric.hexStringToByteArray(data)
    val list = entry.decode(dataBytes).asScala.toList
    if (list.isEmpty && getContractAnnontationIdx[P]().nonEmpty) None
    else Some(Deserialization.deserialize[P](list))
  }

  def unpackResult(data: String)(implicit m: Manifest[R]): Option[R] = {
    val dataBytes = Numeric.hexStringToByteArray(data)
    val list = entry.decodeResult(dataBytes).asScala.toList
    if (list.isEmpty && getContractAnnontationIdx[R]().nonEmpty) None
    else Some(Deserialization.deserialize[R](list))
  }
}

trait AbiEvent[R] {
  val entry: Abi.Event

  def unpack(
      data: String,
      topics: Array[String]
    )(
      implicit
      m: Manifest[R]
    ): Option[R] = {
    val dataBytes = Numeric.hexStringToByteArray(data)
    val topicBytes = topics.map(Numeric.hexStringToByteArray)
    val list = entry.decode(dataBytes, topicBytes).asScala.toList
    if (list.isEmpty && getContractAnnontationIdx[R]().nonEmpty) None
    else Some(Deserialization.deserialize[R](list))
  }
}

// TODO:最好是再彻底重写Abi,不再使用SolidityAbi
abstract class AbiWrap(abiJson: String) {

  protected val abi = Abi.fromJson(abiJson)

  def getTransactionHeader(txInput: String): BigInt =
    Numeric.decodeQuantity(txInput)

  private[abi] def searchByName[T <: Abi.Entry](name: String): Predicate[T] =
    x => x.name.equals(name)

  // TODO: test 字节数组的相等
  private[abi] def searchBySignature[T <: Abi.Entry](
      signature: Array[Byte]
    ): Predicate[T] =
    x => Arrays.equals(x.encodeSignature(), signature)

  def unpackEvent(
      data: String,
      topics: Array[String]
    ): Option[Any]

  def unpackFunctionInput(data: String): Option[Any] = None
}
