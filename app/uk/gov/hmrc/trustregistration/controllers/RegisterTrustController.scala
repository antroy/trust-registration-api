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

import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.trustregistration.metrics.ApplicationMetrics
import uk.gov.hmrc.trustregistration.models._
import uk.gov.hmrc.trustregistration.services.RegisterTrustService
import uk.gov.hmrc.trustregistration.utils.{FailedValidation, JsonSchemaValidator}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RegisterTrustController extends ApplicationBaseController {

  val jsonSchemaValidator: JsonSchemaValidator

  def register(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    authorised("register", "") {
          val jsonString = request.body.toString()
          val validationResult = jsonSchemaValidator.validateAgainstSchema(jsonString)

          validationResult match {
            case fail: FailedValidation => {
              Future.successful(BadRequest(Json.toJson(fail)))
            }
            case _ => {
              try {
                request.body.validate[TrustEstateRequest].map {
                  trustEstate: TrustEstateRequest => {
                    val futureEither: Future[Either[String, TRN]] = registerTrustService.registerTrust(trustEstate.trustEstate.trust.get)
                    futureEither.map {
                      case Right(identifier) => Created(Json.toJson(identifier))
                      case Left("503") => InternalServerError
                      case _ => BadRequest("""{"message": "Failed serialization"}""")
                    }
                  }
                }.recoverTotal {
                  e => {
                    Future.successful(BadRequest(JsError.toFlatJson(e)))
                  }
                }
              }
              catch {
                case e => Future.successful(BadRequest(s"""{"message": "Exception: ${e.getMessage()}"}"""))
              }
            }
          }
      }
    }


  def noChange(identifier: String): Action[AnyContent] = Action.async{ implicit request =>
    authorised("noChange", identifier) {
      respond("noChange", registerTrustService.noChange(identifier))
    }
  }

  def closeTrust(identifier: String): Action[AnyContent] = Action.async{ implicit request =>
    authorised("closeTrust",identifier){
      respond("closeTrust", registerTrustService.closeTrust(identifier))
    }
  }

  def getTrustees(identifier: String): Action[AnyContent] = Action.async{ implicit request =>
    authorised("getTrustees",identifier){
      respond("getTrustees", registerTrustService.getTrustees(identifier))
    }
  }

  def getSettlors(identifier: String): Action[AnyContent] = Action.async{implicit request =>
    authorised("getSettlors",identifier){
      respond("getSettlors", registerTrustService.getSettlors(identifier))
    }
  }

  def getNaturalPersons(identifier: String): Action[AnyContent] = Action.async{ implicit request =>
    authorised("getNaturalPersons",identifier){
      respond("getNaturalPersons", registerTrustService.getNaturalPersons(identifier))
    }
  }

  def getTrustContactDetails(identifier: String): Action[AnyContent] = Action.async{ implicit request =>
    authorised("getTrustContactDetails",identifier){
      respond("getTrustContactDetails", registerTrustService.getTrustContactDetails(identifier))
    }
  }

  def getLeadTrustee(identifier: String): Action[AnyContent] = Action.async { implicit request =>
    authorised("getLeadTrustee",identifier){
      respond("getLeadTrustee", registerTrustService.getLeadTrustee(identifier))
    }
  }

  def getBeneficiaries(identifier: String): Action[AnyContent] = Action.async { implicit request =>
    authorised("getBeneficiaries",identifier){
      respond("getBeneficiaries", registerTrustService.getBeneficiaries(identifier))
    }
  }

  def getProtectors(identifier: String): Action[AnyContent] = Action.async { implicit request =>
    authorised("getProtectors",identifier){
      respond("getProtectors", registerTrustService.getProtectors(identifier))
    }
  }

  def getTrust(identifier: String): Action[AnyContent] = Action.async{ implicit request =>
    authorised("getTrust",identifier){
      respond("getTrust", registerTrustService.getTrust(identifier))
    }
  }
}

object RegisterTrustController extends RegisterTrustController {
  override val registerTrustService = RegisterTrustService
  override val jsonSchemaValidator = JsonSchemaValidator
  override val metrics = ApplicationMetrics
}
