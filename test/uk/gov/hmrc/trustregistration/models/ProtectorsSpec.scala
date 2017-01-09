/*
 * Copyright 2017 HM Revenue & Customs
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

class ProtectorsSpec extends PlaySpec with JsonExamples with ScalaDataExamples {

  "Protectors" must {
    "serialize to json" in {
      val details = Json.toJson[Protectors](protectors).toString()
      details must include ("Spaceman")
    }
    "exclude none values from the serialized response" in {
      val emptyProtectors = new Protectors(None, None)
      val details = Json.toJson[Protectors](emptyProtectors).toString()
      details must be ("{}")
    }
    "throw an exception" when {
      "there are more than two individual protectors" in {
        val ex = the [IllegalArgumentException] thrownBy Protectors(Some(List(individual, individual, individual)), None)
        ex.getMessage must include("must have no more than two protectors")
      }
      "there are more than two company protectors" in {
        val ex = the [IllegalArgumentException] thrownBy Protectors(None, Some(List(company, company, company)))
        ex.getMessage must include("must have no more than two protectors")
      }
      "there are more than two protectors (in any combination)" in {
        val ex = the [IllegalArgumentException] thrownBy Protectors(Some(List(individual)), Some(List(company, company)))
        ex.getMessage must include("must have no more than two protectors")
      }
    }
  }

}
