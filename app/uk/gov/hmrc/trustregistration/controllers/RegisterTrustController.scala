/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.trustregistration.controllers

import play.api.Logger
import play.api.libs.json.{JsError, JsResult, JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.trustregistration.metrics.Metrics
import uk.gov.hmrc.trustregistration.models._
import uk.gov.hmrc.trustregistration.services.RegisterTrustService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RegisterTrustController extends BaseController {

  val metrics: Metrics = Metrics
  val registerTrustService: RegisterTrustService
  val className: String = getClass.getSimpleName

  def register(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    Logger.info("Register API invoked")

    val jsonBody: JsResult[RegistrationDocument] = request.body.validate[RegistrationDocument]
    jsonBody.map { regDoc: RegistrationDocument => {
        val futureEither: Future[Either[String, TRN]] = registerTrustService.registerTrust(regDoc)
        futureEither.map {
          case Right(identifier) => Created(Json.toJson(identifier))
          case _ => BadRequest("Error:")
        }
      }
    }.recoverTotal {
      e => {
        Future.successful(BadRequest("Detected error:" + JsError.toFlatJson(e)))
      }
    }
  }

  def noChange(identifier: String): Action[AnyContent] = Action.async{ implicit request =>

    Logger.info(s"$className:noChange API invoked")
    Logger.debug(s"$className:noChange($identifier) API invoked")

    val authorised: Option[(String, String)] = hc.headers.find((tup) => tup._1 == AUTHORIZATION)

    authorised match {
      case Some((key, "NOT_AUTHORISED")) => {
        Logger.info(s"$className:noChange API returned unauthorised")
        metrics.incrementUnauthorisedRequest("noChange")

        Future.successful(Unauthorized)
      }
      case _ => {
        Logger.info(s"$className:noChange API authorised")
        metrics.incrementAuthorisedRequest("noChange")

        registerTrustService.noChange(identifier) map {
          case SuccessResponse => {
            Logger.info(s"$className:noChange API returned OK")
            metrics.incrementApiSuccessResponse("noChange")
            Ok
          }
          case BadRequestResponse => {
            Logger.info(s"$className:noChange API returned Bad Request")
            metrics.incrementBadRequestResponse("noChange")
            BadRequest
          }
          case NotFoundResponse => {
            Logger.info(s"$className:noChange API returned Not Found")
            metrics.incrementNotFoundResponse("noChange")
            NotFound
          }
          case _ => {
            Logger.info(s"$className:noChange API returned Internal Server Error")
            metrics.incrementInternalServerErrorResponse("noChange")
            InternalServerError
          }
        }
      }
    }
  }


}

object RegisterTrustController extends RegisterTrustController {
  override val registerTrustService = RegisterTrustService
  override val metrics = Metrics
}
