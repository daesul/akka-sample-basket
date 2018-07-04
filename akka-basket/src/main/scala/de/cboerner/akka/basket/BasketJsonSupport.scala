package de.cboerner.akka.basket

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait BasketJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  final case class Position(value: String)

  implicit val positionFormat: RootJsonFormat[Position] = jsonFormat1(Position)
  implicit val itemFormat: RootJsonFormat[Basket.Item] = jsonFormat5(Basket.Item)
  implicit val stateFormat: RootJsonFormat[Basket.State] = jsonFormat1(Basket.State)
  implicit val alreadyInBasketFormat: RootJsonFormat[Basket.AlreadyInBasket] = jsonFormat1(Basket.AlreadyInBasket)
  implicit val unknownPositionForReplacementFormat: RootJsonFormat[Basket.UnknownPositionForReplacement] = jsonFormat1(Basket.UnknownPositionForReplacement)
  implicit val unknownPositionForRemovalFormat: RootJsonFormat[Basket.UnknownPositionForRemoval] = jsonFormat1(Basket.UnknownPositionForRemoval)
}
