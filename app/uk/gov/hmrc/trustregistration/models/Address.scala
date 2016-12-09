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

import play.api.libs.json.{JsPath, Json, Reads}
import play.api.libs.functional.syntax._

case class Address (
                     line1: String,
                     line2: Option[String] = None,
                     line3: Option[String] = None,
                     line4: Option[String] = None,
                     postalCode: Option[String] = None,
                     countryCode: Option[String] = None)

object Address {
  implicit val addressReads: Reads[Address] = (
    (JsPath \\ "line1").read[String] and
    (JsPath \\ "line2").readNullable[String] and
    (JsPath \\ "line3").readNullable[String] and
    (JsPath \\ "line4").readNullable[String] and
    (JsPath \\ "postalCode").readNullable[String] and
    (JsPath \\ "countryCode").readNullable[String]
  )(Address.apply _)

  implicit val addressWrites = Json.writes[Address]
}
