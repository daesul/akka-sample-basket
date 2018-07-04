package de.cboerner.akka.basket

import akka.actor.{Actor, ActorLogging, Props, SupervisorStrategy, Terminated}

object Root {
  def props = Props(new Root)
}

final class Root extends Actor with ActorLogging {

  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  private val baskets = context.actorOf(Baskets.props, Baskets.Name)
  private val api = context.actorOf(Api.props(baskets), Api.Name)

  context.watch(baskets)
  context.watch(api)


  override def receive = {
    case Terminated(actor) =>
      log.error("Terminating the system because {} terminated!", actor.path)
      context.system.terminate()
  }}
