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
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Logger
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.trustregistration.metrics.Metrics
import uk.gov.hmrc.trustregistration.models.{BadRequestResponse, NotFoundResponse, SuccessResponse, TrustResponse}
import uk.gov.hmrc.trustregistration.services.RegisterTrustService

import scala.concurrent.Future


trait TrustBaseController extends BaseController {
  val metrics: Metrics = Metrics
  val registerTrustService: RegisterTrustService
  val className: String = getClass.getSimpleName

  def respond(methodName: String, result: Future[TrustResponse]): Future[Status] = {
    result map {
      case SuccessResponse => {
        Logger.info(s"$className:$methodName API returned OK")
        metrics.incrementApiSuccessResponse(methodName)
        Ok
      }
      case BadRequestResponse => {
        Logger.info(s"$className:$methodName API returned Bad Request")
        metrics.incrementBadRequestResponse(methodName)
        BadRequest
      }
      case NotFoundResponse => {
        Logger.info(s"$className:$methodName API returned Not Found")
        metrics.incrementNotFoundResponse(methodName)
        NotFound
      }
      case _ => {
        Logger.info(s"$className:$methodName API returned Internal Server Error")
        metrics.incrementInternalServerErrorResponse(methodName)
        InternalServerError
      }
    }
  }
}