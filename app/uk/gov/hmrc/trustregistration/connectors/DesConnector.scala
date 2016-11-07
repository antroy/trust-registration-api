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

package uk.gov.hmrc.trustregistration.connectors

import play.api.libs.json.JsValue
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.trustregistration.models._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.trustregistration.audit.TrustsAudit
import uk.gov.hmrc.trustregistration.config.WSHttp
import uk.gov.hmrc.trustregistration.metrics.{TrustMetrics}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait DesConnector extends ServicesConfig with RawResponseReads {
  val httpPost: HttpPost = WSHttp
  val httpPut: HttpPut = WSHttp
  val httpGet: HttpGet = WSHttp

  val audit: TrustsAudit
  val metrics: TrustMetrics

  val AuditNoChangeIdentifier: String = "trustRegistration_noAnnualChangeTrust"
  val AuditCloseTrustIdentifier: String = "trustRegistration_closeTrust"
  val AuditGetTrusteesIdentifier: String = "trustRegistration_getTrustees"
  val AuditGetNaturalPersonsIdentifier: String = "trustRegistration_getNaturalPersons"
  val AuditGetTrustContactDetailsIdentifier: String = "trustRegistration_getTrustContactDetails"
  val AuditGetLeadTrusteeIdentifier: String = "trustRegistration_getLeadTrustee"

  lazy val desUrl = baseUrl("des")
  lazy val serviceUrl = s"$desUrl/trust-registration-stub/trusts"

  def registerTrust(doc: RegistrationDocument)(implicit hc : HeaderCarrier) = {

    val uri: String = s"$serviceUrl/register"

    val result: Future[HttpResponse] = httpPost.POST[RegistrationDocument,HttpResponse](uri,doc)(implicitly, httpReads, implicitly)

    result.map(f=> {
      f.status match{
        case 201 => Right(TRN("TRN-1234"))
        case _ => Left("503")
      }
    }).recover({
      case _ => Left("400")
    })
  }

  def noChange(identifier: String)(implicit hc : HeaderCarrier): Future[TrustResponse] = {
    val uri: String = s"$serviceUrl/$identifier/no-change"

    val timerStart = metrics.startDesConnectorTimer("no-change")

    val result: Future[HttpResponse] = httpPut.PUT[String, HttpResponse](uri, identifier)(implicitly, httpReads, implicitly)

    result.map(f=> {
      timerStart.stop()
      f.status match {
        case 204 => {
          audit.doAudit("noChangeTrustSuccessful", AuditNoChangeIdentifier)
          SuccessResponse
        }
        case 400 => {
          audit.doAudit("noChangeTrustFailure", AuditNoChangeIdentifier)
          BadRequestResponse
        }
        case 404 => {
          audit.doAudit("noChangeTrustFailure", AuditNoChangeIdentifier)
          NotFoundResponse
        }
        case _ => {
          audit.doAudit("noChangeTrustFailure", AuditNoChangeIdentifier)
          InternalServerErrorResponse
        }
      }
    }).recover {
      case _ => {
        audit.doAudit("noChangeTrustFailure", AuditNoChangeIdentifier)
        InternalServerErrorResponse
      }
    }
  }

  def closeTrust(identifier: String)(implicit hc : HeaderCarrier): Future[TrustResponse] = {
    val uri: String = s"$serviceUrl/$identifier/closeTrust"

    val timerStart = metrics.startDesConnectorTimer("closeTrust")

    val result: Future[HttpResponse] = httpPut.PUT[String, HttpResponse](uri, identifier)(implicitly, httpReads, implicitly)

    result.map(f=> {
      timerStart.stop()
      f.status match {
        case 204 => {
          audit.doAudit("closeTrustSuccessful", AuditCloseTrustIdentifier)
          SuccessResponse
        }
        case 400 => {
          audit.doAudit("closeTrustFailure", AuditCloseTrustIdentifier)
          BadRequestResponse
        }
        case 404 => {
          audit.doAudit("closeTrustFailure", AuditCloseTrustIdentifier)
          NotFoundResponse
        }
        case _ => {
          audit.doAudit("closeTrustFailure", AuditCloseTrustIdentifier)
          InternalServerErrorResponse
        }
      }
    }).recover {
      case _ => {
        audit.doAudit("closeTrustFailure", AuditCloseTrustIdentifier)
        InternalServerErrorResponse
      }
    }
  }

  def getTrustees(identifier: String)(implicit hc : HeaderCarrier): Future[TrustResponse] = {

    val uri: String = s"$serviceUrl/$identifier/trustees"

    val timerStart = metrics.startDesConnectorTimer("getTrustees")

    val result: Future[HttpResponse] = httpGet.GET[HttpResponse](uri)(httpReads, implicitly)

    result.map(f => {
      timerStart.stop()
      f.status match {
        case 200 => {
          val trustees = f.json.asOpt[List[Individual]]

          trustees match {
            case Some(value: List[Individual]) => {
              audit.doAudit("getTrusteesSuccessful", AuditGetTrusteesIdentifier)
              GetSuccessResponse(value)
            }
            case _ => {
              audit.doAudit("getTrusteesFailure", AuditGetTrusteesIdentifier)
              InternalServerErrorResponse
            }
          }
        }
        case 400 => {
          audit.doAudit("getTrusteesFailure", AuditGetTrusteesIdentifier)
          BadRequestResponse
        }
        case 404 => {
          audit.doAudit("getTrusteesFailure", AuditGetTrusteesIdentifier)
          NotFoundResponse
        }
        case _ => {
          audit.doAudit("getTrusteesFailure", AuditGetTrusteesIdentifier)
          InternalServerErrorResponse
        }
      }
    }).recover {
      case _ => {
        audit.doAudit("getTrusteesFailure", AuditGetTrusteesIdentifier)
        InternalServerErrorResponse
      }
    }
  }


  def getSettlors(identifier: String)(implicit hc: HeaderCarrier): Future[TrustResponse] = {

    val uri: String = s"$serviceUrl/$identifier/settlors"

    val result: Future[HttpResponse] = httpGet.GET[HttpResponse](uri)(httpReads, implicitly)

    result.map(f => {
      f.status match {
        case 200 => {
          val settlors = f.json.asOpt[Settlors]
          settlors match {
            case Some(value: Settlors) => {
              GetSuccessResponse(value)
            }
          }
        }
        case 400 => {
          BadRequestResponse
        }
        case 404 => {
          NotFoundResponse
        }
        case _ =>{
          InternalServerErrorResponse
        }
      }
    }).recover {
      case _ => {
        InternalServerErrorResponse
      }
    }
  }

  def getNaturalPersons(identifier: String)(implicit hc : HeaderCarrier): Future[TrustResponse] = {

    val uri: String = s"$serviceUrl/$identifier/naturalPersons"

    val timerStart = metrics.startDesConnectorTimer("getNaturalPersons")

    val result: Future[HttpResponse] = httpGet.GET[HttpResponse](uri)(httpReads, implicitly)

    result.map(f => {
      timerStart.stop()
      f.status match {
        case 200 => {
          val naturalPersons = f.json.asOpt[List[Individual]]

          naturalPersons match {
            case Some(value: List[Individual]) => {
              audit.doAudit("getNaturalPersonsSuccessful", AuditGetNaturalPersonsIdentifier)
              GetSuccessResponse(value)
            }
            case _ => {
              audit.doAudit("getNaturalPersonsFailure", AuditGetNaturalPersonsIdentifier)
              InternalServerErrorResponse
            }
          }
        }
        case 400 => {
          audit.doAudit("getNaturalPersonsFailure", AuditGetNaturalPersonsIdentifier)
          BadRequestResponse
        }
        case 404 => {
          audit.doAudit("getNaturalPersonsFailure", AuditGetNaturalPersonsIdentifier)
          NotFoundResponse
        }
        case _ => {
          audit.doAudit("getNaturalPersonsFailure", AuditGetNaturalPersonsIdentifier)
          InternalServerErrorResponse
        }
      }
    }).recover {
      case _ => {
        audit.doAudit("getNaturalPersonsFailure", AuditGetNaturalPersonsIdentifier)
        InternalServerErrorResponse
      }
    }
  }

  def getTrustContactDetails(identifier: String)(implicit hc : HeaderCarrier): Future[TrustResponse] = {

    val uri: String = s"$serviceUrl/$identifier/contactDetails"

    val timerStart = metrics.startDesConnectorTimer("getTrustContactDetails")

    val result: Future[HttpResponse] = httpGet.GET[HttpResponse](uri)(httpReads, implicitly)

    result.map(f => {
      timerStart.stop()
      f.status match {
        case 200 => {
          val details = f.json.asOpt[TrustContactDetails]

          details match {
            case Some(value: TrustContactDetails) => {
              audit.doAudit("getTrustContactDetailsSuccessful", AuditGetTrustContactDetailsIdentifier)
              GetSuccessResponse(value)
            }
            case _ => {
              audit.doAudit("getTrustContactDetailsFailure", AuditGetTrustContactDetailsIdentifier)
              InternalServerErrorResponse
            }
          }
        }
        case 400 => {
          audit.doAudit("getTrustContactDetailsFailure", AuditGetTrustContactDetailsIdentifier)
          BadRequestResponse
        }
        case 404 => {
          audit.doAudit("getTrustContactDetailsFailure", AuditGetTrustContactDetailsIdentifier)
          NotFoundResponse
        }
        case _ => {
          audit.doAudit("getTrustContactDetailsFailure", AuditGetTrustContactDetailsIdentifier)
          InternalServerErrorResponse
        }
      }
    }).recover {
      case _ => {
        audit.doAudit("getTrustContactDetailsFailure", AuditGetTrustContactDetailsIdentifier)
        InternalServerErrorResponse
      }
    }

  }

  def getLeadTrustee(identifier: String)(implicit hc : HeaderCarrier): Future[TrustResponse] = {

    val uri: String = s"$serviceUrl/$identifier/leadTrustee"

    val timerStart = metrics.startDesConnectorTimer("getTrustContactDetails")

    val result: Future[HttpResponse] = httpGet.GET[HttpResponse](uri)(httpReads, implicitly)

    result.map(f => {
      timerStart.stop()
      f.status match {
        case 200 => {
          val details = f.json.asOpt[LeadTrustee]

          details match {
            case Some(value: LeadTrustee) => {
              audit.doAudit("getLeadTrusteeSuccessful", AuditGetLeadTrusteeIdentifier)
              GetSuccessResponse(value)
            }
            case _ => {
              audit.doAudit("getLeadTrusteeFailure", AuditGetLeadTrusteeIdentifier)
              InternalServerErrorResponse
            }
          }
        }
        case 400 => {
          audit.doAudit("getLeadTrusteeFailure", AuditGetLeadTrusteeIdentifier)
          BadRequestResponse
        }
        case 404 => {
          audit.doAudit("getLeadTrusteeFailure", AuditGetLeadTrusteeIdentifier)
          NotFoundResponse
        }
      }
    }).recover {
      case _ => {
        audit.doAudit("getLeadTrusteeFailure", AuditGetLeadTrusteeIdentifier)
        InternalServerErrorResponse
      }
    }
  }

}

object DesConnector extends DesConnector {
  override val audit: TrustsAudit = TrustsAudit
  override val metrics: TrustMetrics = TrustMetrics
}
