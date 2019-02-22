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

package io.lightcone.relayer.socketio

import com.corundumstudio.socketio._
import com.corundumstudio.socketio.protocol.JacksonJsonSupport
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.config.Config

class SocketServer(
  )(
    implicit
    val config: Config,
    val balanceNotifier: SocketIONotifier[SubscribeBalanceAndAllowance],
    val transactionNotifier: SocketIONotifier[SubscribeTransaction],
    val orderNotifier: SocketIONotifier[SubscribeOrder],
    val tradeNotifier: SocketIONotifier[SubscribeFill],
    val tickerNotifier: SocketIONotifier[SubscribeTicker],
    val orderBookNotifier: SocketIONotifier[SubscribeOrderBook],
    val transferNotifier: SocketIONotifier[SubscribeTransfer]) {

  val selfConfig = config.getConfig("socketio")
  val socketConfig = new Configuration()
  socketConfig.setHostname(selfConfig.getString("host"))
  socketConfig.setPort(selfConfig.getInt("port"))
  socketConfig.setJsonSupport(new JacksonJsonSupport(DefaultScalaModule))

  val server = new SocketIOServer(socketConfig)

  server.addEventListener(
    balanceNotifier.eventName,
    classOf[SubscribeBalanceAndAllowance],
    balanceNotifier
  )

  server.addEventListener(
    transactionNotifier.eventName,
    classOf[SubscribeTransaction],
    transactionNotifier
  )

  server.addEventListener(
    orderNotifier.eventName,
    classOf[SubscribeOrder],
    orderNotifier
  )

  server.addEventListener(
    tradeNotifier.eventName,
    classOf[SubscribeFill],
    tradeNotifier
  )

  server.addEventListener(
    tickerNotifier.eventName,
    classOf[SubscribeTicker],
    tickerNotifier
  )

  server.addEventListener(
    orderBookNotifier.eventName,
    classOf[SubscribeOrderBook],
    orderBookNotifier
  )

  server.addEventListener(
    transferNotifier.eventName,
    classOf[SubscribeTransfer],
    transferNotifier
  )
  // def addNotifier()

  def start(): Unit = server.start()
}
