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


class SettlorsSpec extends PlaySpec {

  "Settlors" must {
    "throw an exception" when {
      "there are no individuals or companies" in {
        val ex = the [IllegalArgumentException] thrownBy Settlors(None, None)
        ex.getMessage must include("object has missing required properties ([\\\"settlors\\\"])")
      }
    }
  }

}
