package de.cboerner.akka.basket

import akka.actor.{ActorLogging, Props, ReceiveTimeout}
import akka.persistence.{PersistentActor, SnapshotOffer}

import scala.concurrent.duration._

object Basket {

  sealed trait Command
  final case class Add(item: Item) extends Command
  final case class Remove(position: String) extends Command
  final case class Replace(item: Item) extends Command
  final case object Get extends Command
  final case object Checkout extends Command
  final case object Logout extends Command

  sealed trait Event
  final case class Added(item: Item) extends Event
  final case class Removed(position: String) extends Event
  final case class Replaced(item: Item) extends Event

  final case class AlreadyInBasket(item: Item)
  final case class UnknownPositionForRemoval(position: String)
  final case class UnknownPositionForReplacement(item: Item)
  final case object LoggedOut

  def props: Props = Props(new Basket)

  def name(id: Int) = s"basket-$id"

  final case class State(items: Map[String, Item] = Map.empty) {

    def empty: Boolean = items.isEmpty

    def +(item: Item): State = copy(items = this.items + (item.position -> item))

    def -(position: String): State = copy(items - position)
  }
  final case class Item(position: String,
                        name: String,
                        quantity: Int,
                        unitPrice: Double,
                        price: Double)
}

final class Basket extends PersistentActor with ActorLogging {

  import de.cboerner.akka.basket.Basket._

  override def persistenceId: String = self.path.name

  private var state = State()

  log.info("Basket {} is in use", persistenceId)

  context.setReceiveTimeout(1.minute)


  override def receiveCommand: Receive = {
    case cmd: Command => handleCommand(cmd)
    case ReceiveTimeout => handlePassivation()
    case unknown => log.info("Unknown command: {}! Nothing to do.", unknown)
  }

  private def handlePassivation(): Unit = {
    log.info("I'm bored. Having a nap.")
    context.stop(self)
  }

  private def handleRemove(position: String): Unit =
    state.items.get(position) match {
      case None =>
        log.info("Position {} does not exist in basket for removal", position)
        sender() ! UnknownPositionForRemoval(position)
      case Some(_) =>
        persist(Removed(position)) { evt =>
          handleEvent(evt)
          sender() ! state
        }
    }

  private def handleAdd(item: Item): Unit =
    state.items.get(item.position) match {
      case Some(presentItem) if presentItem.name != item.name =>
        log.info("Item already Exists")
        sender() ! AlreadyInBasket(item)
      case None =>
        persist(Added(item)) { evt =>
          handleEvent(evt)
          log.info("Added item {} to state", item)
          sender() ! state
        }
    }

  private def handleReplace(item: Item): Unit = state.items.get(item.position) match {
    case None =>
      log.info("Position {} does not exists for replacement with {}", item.position, item)
      sender() ! UnknownPositionForReplacement(item)
    case Some(_) =>
      persist(Replaced(item)) { evt =>
        handleEvent(evt)
        sender() ! state
      }
  }

  private def handleGet(): Unit = {
    sender() ! state

  }

  private def handleLogout(): Unit = {
    log.info("Basket {} has been logged out", persistenceId)
    saveSnapshot(state)
    sender() ! LoggedOut
    context.stop(self)

  }

  private def handleCheckout(): Unit = {
    // Do something with basket state
    log.info("Basket {} has been checked out", persistenceId)
    state = State()
    saveSnapshot(state)
    sender() ! state
  }

  private def handleCommand(command: Command): Unit = command match {
    case add: Add => handleAdd(add.item)
    case remove: Remove => handleRemove(remove.position)
    case replace: Replace => handleReplace(replace.item)
    case Get => handleGet()
    case Logout => handleLogout()
    case Checkout => handleCheckout()
  }

  private def handleEvent(evt: Event): Unit = evt match {
    case added: Added => state = state + added.item
    case removed: Removed => state = state - removed.position
    case replaced: Replaced => state = state + replaced.item
  }

  override def receiveRecover: Receive = {
    case evt: Event =>
      log.info(s"Recovering baskets from event $evt for $persistenceId")
      handleEvent(evt)
    case SnapshotOffer(_, snapshot: State) =>
      log.info(s"Recovering baskets from snapshot: $snapshot for $persistenceId")
      state = snapshot
  }

}
