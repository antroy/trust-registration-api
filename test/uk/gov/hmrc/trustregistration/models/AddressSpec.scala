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

package uk.gov.hmrc.trustregistration.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.trustregistration.{JsonExamples, ScalaDataExamples}

class AddressSpec extends PlaySpec with JsonExamples with ScalaDataExamples {

  val addressGb = """{"addressGB": {"line1": "123 Easy Street", "postalCode": "AB1 1AB"}}"""
  val addressNonGb = """{"addressNonGB": {"line1": "123 Easy Street", "postalCode": "AB1 1AB"}}"""
  val addressLegacy = """{"line1": "123 Easy Street", "postalCode": "AB1 1AB"}""" // TODO: Take this out once we've ported over to new address style

  "Address" must {
    "serialize from Json" when {
      "a GB address is specified" in {
        val address: Address = Json.parse(addressGb).as[Address]

        address.line1 mustBe "123 Easy Street"
        address.postalCode mustBe Some("AB1 1AB")
      }
      "a non GB address is specified" in {
        val address: Address = Json.parse(addressNonGb).as[Address]

        address.line1 mustBe "123 Easy Street"
        address.postalCode mustBe Some("AB1 1AB")
      }
      "a old-style address is specified (i.e. backwards compatibility!)" in {
        val address: Address = Json.parse(addressLegacy).as[Address]

        address.line1 mustBe "123 Easy Street"
        address.postalCode mustBe Some("AB1 1AB")
      }
    }
  }

}