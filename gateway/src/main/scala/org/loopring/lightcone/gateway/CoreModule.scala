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

package org.loopring.lightcone.gateway

import akka.actor.{ ActorRef, ActorSystem }
import akka.cluster.Cluster
import akka.cluster.singleton.{ ClusterSingletonProxy, ClusterSingletonProxySettings }
import akka.stream.ActorMaterializer
import akka.stream.alpakka.slick.scaladsl.SlickSession
import com.google.inject._
import com.google.inject.name.Named
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule
import org.loopring.lightcone.gateway.api.HttpAndIOServer
import org.loopring.lightcone.gateway.api.service.{ BalanceService, BalanceServiceImpl }
import org.loopring.lightcone.gateway.inject.{ AssistedInjectFactoryScalaModule, ProxyActor, ProxyActorProvider }
import org.loopring.lightcone.gateway.jsonrpc.{ JsonRpcServer, JsonRpcSettings }
import org.loopring.lightcone.gateway.socketio.{ EventRegistering, SocketIOServer }
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

object CoreModule {

  def apply(config: Config): CoreModule = new CoreModule(config)

  class ActorMaterializerProvider @Inject() (system: ActorSystem) extends Provider[ActorMaterializer] {
    override def get(): ActorMaterializer = ActorMaterializer()(system)
  }

}

class CoreModule(config: Config)
  extends AbstractModule with ScalaModule with AssistedInjectFactoryScalaModule[Binder] {

  override def configure(): Unit = {

    val system = ActorSystem("Lightcone", config)

    bind[ActorSystem].toInstance(system)

    bind[Cluster].toInstance(Cluster(system))

    bind[Config].toInstance(system.settings.config)

    bind[ActorMaterializer].toProvider[CoreModule.ActorMaterializerProvider].asEagerSingleton()

    bind[BalanceService].to[BalanceServiceImpl]

    val databaseConfig = DatabaseConfig.forConfig[JdbcProfile]("slick-mysql", system.settings.config)
    val session: SlickSession = SlickSession.forConfig(databaseConfig)
    bind[SlickSession].toInstance(session)

    system.registerOnTermination(() ⇒ session.close())

    bindFactory[ProxyActorProvider, ProxyActor]()
  }

  @Provides
  @Singleton
  @Named("cluster_proxy")
  def providerProxyMap(config: Config, system: ActorSystem): Map[String, ActorRef] = {
    import scala.collection.JavaConverters._

    def proxy(named: String, system: ActorSystem): ActorRef = {
      system.actorOf(
        ClusterSingletonProxy.props(
          singletonManagerPath = s"/user/${named}",
          settings = ClusterSingletonProxySettings(system)),
        name = s"proxy_${named}")
    }

    config.getStringList("akka.cluster.routees").asScala.map { path ⇒
      path → proxy(path, system)
    } toMap
  }

  @Provides
  @Singleton
  def provideHttpAndIOServer(
    proxy: ProxyActor)(
    implicit
    injector: Injector,
    system: ActorSystem,
    mat: ActorMaterializer): HttpAndIOServer = {
    // 这里注册需要反射类
    val settings = JsonRpcSettings().register[BalanceServiceImpl]

    val jsonRpcServer = new JsonRpcServer(settings)

    // 这里注册定时任务
    val registering = EventRegistering()
      .registering("getBalance", 10000, "balance")

    val ioServer = new SocketIOServer(jsonRpcServer, registering)

    new HttpAndIOServer(jsonRpcServer, ioServer)
  }

}
