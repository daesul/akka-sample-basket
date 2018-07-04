package de.cboerner.akka.basket

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import de.cboerner.akka.basket.Baskets.CommandEnvelop

object Baskets {

  final case class CommandEnvelop(basketId: Int, command: Basket.Command)

  val Name: String = "baskets"

  def props = Props(new Baskets)


}

final class Baskets extends Actor with ActorLogging {

  private var baskets: Map[String, ActorRef] = Map.empty

  private def handleCommandEnvelop(basketId: Int, command: Basket.Command): Unit = {
    log.info("Handle Command {} for basket {}", command, basketId)
    val basketName = Basket.name(basketId)
    val currentBasket = baskets.get(basketName) match {
      case Some(actor) =>
        log.info("Using existing actor for basket {}", basketName)
        actor
      case None =>
        log.info("Creating new actor for basket {}", basketName)
        val newBasket = context.actorOf(Basket.props, basketName)
        baskets = baskets + (basketName -> newBasket)
        context.watch(newBasket)
        newBasket
    }
    currentBasket forward command
  }


  private def handlePassivation(name: String): Unit = {
    log.info("Basket {} has been passivated", name)
    baskets = baskets - name
  }

  override def receive: Receive = {
    case commandEnvelop: CommandEnvelop => handleCommandEnvelop(commandEnvelop.basketId, commandEnvelop.command)
    case terminated:Terminated => handlePassivation(terminated.actor.path.name)
  }

}
