package de.cboerner.akka.basket

import akka.actor.ActorSystem


object Main {

  def main(args: Array[String]): Unit = {

    val system = ActorSystem("akka-basket")
    system.actorOf(Root.props, "root")
  }

}
