package de.cboerner.akka.basket

import java.net.InetSocketAddress

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import de.cboerner.akka.basket.Basket.{Item, UnknownPositionForReplacement}
import de.cboerner.akka.basket.Baskets.CommandEnvelop

import scala.concurrent.duration._


object Api extends BasketJsonSupport {

  val Name: String = "basket-api"

  def props(baskets: ActorRef) = Props(new Api(baskets))

  implicit val timeout: Timeout = 3.seconds

  import akka.http.scaladsl.server.Directives._


  def route(baskets: ActorRef): Route = {
    basketPath(baskets) ~ healthPath
  }

  private def basketPath(baskets: ActorRef): Route = {
    pathPrefix("basket" / IntNumber) { basketId =>
      pathEndOrSingleSlash {
        get {
          onSuccess(baskets ? CommandEnvelop(basketId, Basket.Get)) {
            case state: Basket.State => complete(state)
          }
        } ~
          post {
            entity(as[Item]) { item =>
              onSuccess(baskets ? CommandEnvelop(basketId, Basket.Add(item))) {
                case state: Basket.State => complete(state)
                case alreadyInBasket: Basket.AlreadyInBasket => complete(StatusCodes.Conflict -> alreadyInBasket)
              }
            }
          } ~
          put {
            entity(as[Item]) { item =>
              onSuccess(baskets ? CommandEnvelop(basketId, Basket.Replace(item))) {
                case state: Basket.State => complete(state)
                case unknownPositionForReplacement: Basket.UnknownPositionForReplacement => complete(StatusCodes.Conflict -> unknownPositionForReplacement)
              }
            }
          } ~
          delete {
            entity(as[Position]) { position =>
              onSuccess(baskets ? CommandEnvelop(basketId, Basket.Remove(position.value))) {
                case state: Basket.State => complete(state)
                case unknownPositionForRemoval: Basket.UnknownPositionForRemoval => complete(StatusCodes.Conflict -> unknownPositionForRemoval)
              }
            }
          }
      } ~
        path("logout") {
          onSuccess(baskets ? CommandEnvelop(basketId, Basket.Logout)) {
            case Basket.LoggedOut => complete(StatusCodes.OK)
          }
        } ~
        path("checkout") {
          onSuccess(baskets ? CommandEnvelop(basketId, Basket.Checkout)) {
            case state: Basket.State => complete(state)
          }
        }

    }
  }

  private val healthPath: Route = path("health") {
    complete("Up and Running!")
  }


}

final class Api(baskets: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher

  implicit val mat = ActorMaterializer()

  Http(context.system)
    .bindAndHandle(Api.route(baskets), "localhost", 8080)
    .pipeTo(self)

  override def receive: Receive = {
    case ServerBinding(address) => handleBinding(address)
    case Failure(cause) => handleBindFailure(cause)
  }

  private def handleBinding(address: InetSocketAddress) = {
    log.info("Listening on {}", address)
    context.become(Actor.emptyBehavior)
  }

  private def handleBindFailure(cause: Throwable) = {
    log.error("Can't bind. Reason: {}", cause)
    context.stop(self)
  }
}
