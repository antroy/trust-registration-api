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


import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.http.HeaderNames.{AUTHORIZATION => _, CONTENT_TYPE => _, _}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Request, RequestHeader, Result}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.trustregistration.JsonExamples
import uk.gov.hmrc.trustregistration.metrics.TrustMetrics
import uk.gov.hmrc.trustregistration.models._
import uk.gov.hmrc.trustregistration.services.RegisterTrustService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source


class RegisterTrustControllerSpec extends PlaySpec
  with OneAppPerSuite
  with MockitoSugar
  with BeforeAndAfter
  with JsonExamples {

  before {
    when(mockRegisterTrustService.registerTrust(any[RegistrationDocument])(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(TRN("TRN-1234"))))

    when(mockHC.headers).thenReturn(List(AUTHORIZATION -> "AUTHORISED"))

  }

  "RegisterTrustController" must {
    "return created with a TRN" when {
      "the register endpoint is called with a valid json payload" in {
        withCallToPOST(regDocPayload) { result =>
          status(result) mustBe CREATED
          contentAsString(result) must include("TRN")
        }
      }
    }
    "Return a Bad Request" when {
      "The json trust document is invalid" in {
        withCallToPOST(badRegDocPayload) { result =>
          status(result) mustBe BAD_REQUEST
        }
      }
      "The json trust document is missing" in {
        withCallToPOST() { result =>
          status(result) mustBe BAD_REQUEST
        }
      }
    }
  }

  "No change endpoint" must {
    "return 200 ok" when {
      "the endpoint is called with a valid identifier" in {
        when(mockRegisterTrustService.noChange(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(SuccessResponse))

        val result = SUT.noChange("sadfg").apply(FakeRequest("PUT", ""))

        status(result) mustBe OK
      }
    }

    "return 400" when {
      "the endpoint is called with an invalid identifier" in {
        when(mockRegisterTrustService.noChange(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(BadRequestResponse))

        val result = SUT.noChange("sadfg").apply(FakeRequest("PUT", ""))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return 401" when {
      "authentication credentials are missing or incorrect" in {

        when(mockHC.headers).thenReturn(List(AUTHORIZATION -> "NOT_AUTHORISED"))
        val result = SUT.noChange("12345").apply(FakeRequest("PUT", ""))

        status(result) mustBe UNAUTHORIZED
      }
    }

    "return 404" when {
      "we pass an identifier that does not return a trust" in {
        when(mockRegisterTrustService.noChange(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(NotFoundResponse))

        val result = SUT.noChange("sadfg").apply(FakeRequest("PUT", ""))

        status(result) mustBe NOT_FOUND
      }
    }
  }

  "Close trusts endpoint" must {
    "return 200 ok" when {
      "the endpoint is called with a valid identifier" in {
        when(mockRegisterTrustService.closeTrust(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(SuccessResponse))

        val result = SUT.closeTrust("sadfg").apply(FakeRequest("PUT", ""))

        status(result) mustBe OK
      }
    }

    "return 400" when {
      "the endpoint is called with an invalid identifier" in {
        when(mockRegisterTrustService.closeTrust(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(BadRequestResponse))

        val result = SUT.closeTrust("sadfg").apply(FakeRequest("PUT", ""))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return 401" when {
      "the endpoint is called and authentication credentials are missing or incorrect" in {

        when(mockHC.headers).thenReturn(List(AUTHORIZATION -> "NOT_AUTHORISED"))
        val result = SUT.closeTrust("12345").apply(FakeRequest("PUT", ""))

        status(result) mustBe UNAUTHORIZED
      }
    }

    "return 404" when {
      "the endpoint is called and we pass an identifier that does not return a trust" in {
        when(mockRegisterTrustService.closeTrust(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(NotFoundResponse))

        val result = SUT.closeTrust("sadfg").apply(FakeRequest("PUT", ""))

        status(result) mustBe NOT_FOUND
      }
    }

    "return 500" when {
      "something is broken" in {
        when(mockRegisterTrustService.closeTrust(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(InternalServerErrorResponse))

        val result = SUT.closeTrust("sadfg").apply(FakeRequest("PUT", ""))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "Get Trustees endpoint" must {

    "return 200 ok" when {
      "the endpoint is called with a valid identifier" in {
        when(mockRegisterTrustService.getTrustees(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(new GetSuccessResponse[List[Individual]](Nil)))

        val result = SUT.getTrustees("sadfg").apply(FakeRequest("GET", ""))

        status(result) mustBe OK
      }
    }

    "return 200 ok with valid json" when {
      "the endpoint is called with a valid identifier" in {
        val individual = Individual(
          title = "Mr",
          givenName = "John",
          familyName = "Doe",
          dateOfBirth = new DateTime("1800-01-01"),
          passport = Some(Passport(
            identifier = "IDENTIFIER",
            expiryDate = new DateTime("2000-01-01"),
            countryOfIssue = "UK"
          )),
          correspondenceAddress = Some(Address(
            isNonUkAddress = false,
            addressLine1 = "Address Line 1"
          ))
        )
        when(mockRegisterTrustService.getTrustees(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(new GetSuccessResponse[List[Individual]](List(individual))))

        val result = SUT.getTrustees("sadfg").apply(FakeRequest("GET", ""))

        status(result) mustBe OK
        contentAsString(result) mustBe (
          """[{"title":"Mr","givenName":"John","familyName":"Doe","dateOfBirth":"1800-01-01",""" +
            """"passport":{"identifier":"IDENTIFIER","expiryDate":"2000-01-01","countryOfIssue":"UK"},""" +
            """"correspondenceAddress":{"isNonUkAddress":false,"addressLine1":"Address Line 1"}}]""")
      }
    }

    "return 404 not found" when {
      "the endpoint is called with an identifier that can't be found" in {
        when(mockRegisterTrustService.getTrustees(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(NotFoundResponse))

        val result = SUT.getTrustees("404NotFound").apply(FakeRequest("GET", ""))

        status(result) mustBe NOT_FOUND
      }
    }

    "return 400" when {
      "the  endpoint is called with an invalid identifier" in {
        when(mockRegisterTrustService.getTrustees(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(BadRequestResponse))

        val result = SUT.getTrustees("sadfg").apply(FakeRequest("GET", ""))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return 500" when {
      "something is broken" in {
        when(mockRegisterTrustService.getTrustees(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(InternalServerErrorResponse))

        val result = SUT.getTrustees("sadfg").apply(FakeRequest("GET", ""))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 401" when {
      "the endpoint is called and authentication credentials are missing or incorrect" in {
        when(mockHC.headers).thenReturn(List(AUTHORIZATION -> "NOT_AUTHORISED"))
        val result = SUT.getTrustees("12345").apply(FakeRequest("GET", ""))

        status(result) mustBe UNAUTHORIZED
      }
    }
  }

  "Get Settlors endpoint" must {
    "return 200 ok" when {
      "the endpoint is called with a valid identifier" in {
        when(mockRegisterTrustService.getSettlors(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(new GetSuccessResponse[List[Settlors]](Nil)))

        val result = SUT.getSettlors("1234").apply(FakeRequest("GET",""))

        status(result) mustBe OK
      }
    }

    "return 200 ok with valid json" when {
      "the endpoint is called with a valid identifier" in {
        val validAddress = Address(false, "Fake Street 123, Testland")
        val validCompanySettlors = Settlors(None,Some(List(Company("Company",validAddress,"12345",Some("AAA5221")),Company("Company",validAddress,"12345",Some("AAA5221")))))

        val expectedSettlorsJson = ("""{"companies" : [{COMPANY},{COMPANY}]}""").replace("{COMPANY}", validCompanyJson)

        when(mockRegisterTrustService.getSettlors(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(new GetSuccessResponse[Settlors](validCompanySettlors)))

        val result = SUT.getSettlors("1234").apply(FakeRequest("GET",""))
        status(result) mustBe OK
        contentAsString(result) contains (expectedSettlorsJson)
      }
    }

    "return 404 not found" when {
      "the endpoint is called with an identifier that can't be found" in {
        when(mockRegisterTrustService.getSettlors(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(NotFoundResponse))

        val result = SUT.getSettlors("404NotFound").apply(FakeRequest("GET", ""))

        status(result) mustBe NOT_FOUND
      }
    }

    "return 400" when {
      "the  endpoint is called with an invalid identifier" in {
        when(mockRegisterTrustService.getSettlors(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(BadRequestResponse))

        val result = SUT.getSettlors("sadfg").apply(FakeRequest("GET", ""))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return 500" when {
      "something is broken" in {
        when(mockRegisterTrustService.getSettlors(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(InternalServerErrorResponse))

        val result = SUT.getSettlors("sadfg").apply(FakeRequest("GET", ""))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 401" when {
      "the endpoint is called and authentication credentials are missing or incorrect" in {
        when(mockHC.headers).thenReturn(List(AUTHORIZATION -> "NOT_AUTHORISED"))
        val result = SUT.getSettlors("12345").apply(FakeRequest("GET", ""))

        status(result) mustBe UNAUTHORIZED
      }
    }
  }

  "Get Natural Persons endpoint" must {

      "return 200 ok" when {
        "the endpoint is called with a valid identifier" in {
          when(mockRegisterTrustService.getNaturalPersons(any[String])(any[HeaderCarrier]))
            .thenReturn(Future.successful(new GetSuccessResponse[List[Individual]](Nil)))

          val result = SUT.getNaturalPersons("sadfg").apply(FakeRequest("GET", ""))

          status(result) mustBe OK
        }
      }

      "return 200 ok with valid json" when {
        "the endpoint is called with a valid identifier" in {
          val individual = Individual(
            title = "Mr",
            givenName = "John",
            familyName = "Doe",
            dateOfBirth = new DateTime("1800-01-01"),
            passport = Some(Passport(
              identifier = "IDENTIFIER",
              expiryDate = new DateTime("2000-01-01"),
              countryOfIssue = "UK"
            )),
            correspondenceAddress = Some(Address(
              isNonUkAddress = false,
              addressLine1 = "Address Line 1"
            ))
          )
          when(mockRegisterTrustService.getNaturalPersons(any[String])(any[HeaderCarrier]))
            .thenReturn(Future.successful(new GetSuccessResponse[List[Individual]](List(individual))))

          val result = SUT.getNaturalPersons("sadfg").apply(FakeRequest("GET", ""))

          status(result) mustBe OK
          contentAsString(result) mustBe (
            """[{"title":"Mr","givenName":"John","familyName":"Doe","dateOfBirth":"1800-01-01",""" +
              """"passport":{"identifier":"IDENTIFIER","expiryDate":"2000-01-01","countryOfIssue":"UK"},""" +
              """"correspondenceAddress":{"isNonUkAddress":false,"addressLine1":"Address Line 1"}}]""")
        }
      }

      "return 404 not found" when {
        "the endpoint is called with an identifier that can't be found" in {
          when(mockRegisterTrustService.getNaturalPersons(any[String])(any[HeaderCarrier]))
            .thenReturn(Future.successful(NotFoundResponse))

          val result = SUT.getNaturalPersons("404NotFound").apply(FakeRequest("GET", ""))

          status(result) mustBe NOT_FOUND
        }
      }

      "return 400" when {
        "the  endpoint is called with an invalid identifier" in {
          when(mockRegisterTrustService.getNaturalPersons(any[String])(any[HeaderCarrier]))
            .thenReturn(Future.successful(BadRequestResponse))

          val result = SUT.getNaturalPersons("sadfg").apply(FakeRequest("GET", ""))

          status(result) mustBe BAD_REQUEST
        }
      }

      "return 500" when {
        "something is broken" in {
          when(mockRegisterTrustService.getNaturalPersons(any[String])(any[HeaderCarrier]))
            .thenReturn(Future.successful(InternalServerErrorResponse))

          val result = SUT.getNaturalPersons("sadfg").apply(FakeRequest("GET", ""))
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "return 401" when {
        "the endpoint is called and authentication credentials are missing or incorrect" in {
          when(mockHC.headers).thenReturn(List(AUTHORIZATION -> "NOT_AUTHORISED"))
          val result = SUT.getNaturalPersons("12345").apply(FakeRequest("GET", ""))

          status(result) mustBe UNAUTHORIZED
        }
      }

  }

  "Get Trust Contact Details endpoint" must {

    "return 200 ok" when {
      "the endpoint is called with a valid identifier" in {
        when(mockRegisterTrustService.getTrustContactDetails(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(new GetSuccessResponse[TrustContactDetails](TrustContactDetails(
            correspondenceAddress = validAddress,
            telephoneNumber = "0191 234 5678"
          ))))

        val result = SUT.getTrustContactDetails("sadfg").apply(FakeRequest("GET", ""))

        status(result) mustBe OK
      }
    }

    "return 200 ok with valid json" when {
      "the endpoint is called with a valid identifier" in {
        when(mockRegisterTrustService.getTrustContactDetails(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(new GetSuccessResponse[TrustContactDetails](TrustContactDetails(
            correspondenceAddress = validAddress,
            telephoneNumber = "0191 234 5678"
          ))))

        val result = SUT.getTrustContactDetails("sadfg").apply(FakeRequest("GET", ""))

        status(result) mustBe OK
        contentAsString(result) mustBe (
          """{"correspondenceAddress":""" +
            """{"isNonUkAddress":false,"addressLine1":"Line 1","addressLine2":"Line 2","addressLine3":"Line 3",""" +
            """"addressLine4":"Line 4","postcode":"NE1 2BR","country":"UK"},""" +
          """"telephoneNumber":"0191 234 5678"}""")
      }
    }

    "return 404 not found" when {
      "the endpoint is called with an identifier that can't be found" in {
        when(mockRegisterTrustService.getTrustContactDetails(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(NotFoundResponse))

        val result = SUT.getTrustContactDetails("404NotFound").apply(FakeRequest("GET", ""))

        status(result) mustBe NOT_FOUND
      }
    }

    "return 400" when {
      "the  endpoint is called with an invalid identifier" in {
        when(mockRegisterTrustService.getTrustContactDetails(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(BadRequestResponse))

        val result = SUT.getTrustContactDetails("sadfg").apply(FakeRequest("GET", ""))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return 500" when {
      "something is broken" in {
        when(mockRegisterTrustService.getTrustContactDetails(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(InternalServerErrorResponse))

        val result = SUT.getTrustContactDetails("sadfg").apply(FakeRequest("GET", ""))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 401" when {
      "the endpoint is called and authentication credentials are missing or incorrect" in {
        when(mockHC.headers).thenReturn(List(AUTHORIZATION -> "NOT_AUTHORISED"))
        val result = SUT.getTrustContactDetails("12345").apply(FakeRequest("GET", ""))

        status(result) mustBe UNAUTHORIZED
      }
    }

  }

  private val regDocPayload = Json.obj(
    "value" -> "Trust Name"
  )
  private val badRegDocPayload = Json.obj(
    "wrongIdentifier" -> "Trust Name"
  )

  private val mockRegisterTrustService = mock[RegisterTrustService]
  private val mockHC = mock[HeaderCarrier]
  private val mockMetrics = mock[TrustMetrics]
  private val mockContext = new com.codahale.metrics.Timer().time()

  when (mockMetrics.startDesConnectorTimer(any())).thenReturn(mockContext)

  object SUT extends RegisterTrustController {
    override implicit def hc(implicit rh: RequestHeader): HeaderCarrier = mockHC
    
    override val metrics: TrustMetrics = mockMetrics
    override val registerTrustService: RegisterTrustService = mockRegisterTrustService

  }


  private def withCallToPOST(payload: JsValue)(handler: Future[Result] => Any) = {
    handler(SUT.register.apply(registerRequestWithPayload(payload)))
  }

  private def registerRequestWithPayload(payload: JsValue): Request[JsValue] = FakeRequest(
    "POST",
    "",
    FakeHeaders(),
    payload
  ).withHeaders(CONTENT_TYPE -> "application/json")

  private def withCallToPOST()(handler: Future[Result] => Any) = {
    val fr = FakeRequest(
      "PUT",
      "",
      FakeHeaders(),
      ""
    ).withHeaders(CONTENT_TYPE -> "application/json")
    SUT.register.apply(fr)
  }

}
