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

package uk.gov.hmrc.trustregistration

import org.joda.time.DateTime
import uk.gov.hmrc.trustregistration.models._

import scala.io.Source

trait JsonExamples {
  val validPassportJson = Source.fromFile(getClass.getResource("/ValidPassport.json").getPath).mkString
  val validAddressJson = Source.fromFile(getClass.getResource("/ValidAddress.json").getPath).mkString
  val validIndividualJson = Source
    .fromFile(getClass.getResource("/ValidIndividual.json").getPath)
    .mkString
    .replace("\"{PASSPORT}\"", validPassportJson)
    .replace("\"{ADDRESS}\"", validAddressJson)
  val invalidIndividualJson = Source.fromFile(getClass.getResource("/InvalidIndividual.json").getPath).mkString
  val validCompanyJson = Source
    .fromFile(getClass.getResource("/ValidCompany.json").getPath)
    .mkString
    .replace("\"{ADDRESS}\"", validAddressJson)
  val invalidCompanyJson = Source
    .fromFile(getClass.getResource("/InvalidCompany.json").getPath)
    .mkString
    .replace("\"{ADDRESS}\"", validAddressJson)
  val validLeadTrusteeIndividualJson = s"""{"individual":$validIndividualJson,"company":null}"""
  val validLeadTrusteeCompanyJson = s"""{"individual":null,"company":$validCompanyJson}"""
  val invalidLeadTrusteeJson = s"""{"individual":$validIndividualJson,"company":$validCompanyJson}"""
}

trait ScalaDataExamples {
  val address = Address(
    isNonUkAddress = false,
    addressLine1 = "Line 1",
    addressLine2 = Some("Line 2"),
    addressLine3 = Some("Line 3"),
    addressLine4 = Some("Line 4"),
    postcode = Some("NE1 2BR"),
    country = Some("UK")
  )

  val passport = Passport(
    identifier = "IDENTIFIER",
    expiryDate = new DateTime("2020-01-01"),
    countryOfIssue = "UK"
  )

  val individual = Individual(
    title = "Dr",
    givenName = "Leo",
    otherName = None,
    familyName = "Spaceman",
    dateOfBirth = new DateTime("1800-01-01"),
    dateOfDeath = None,
    nino = None,
    passport = Some(passport),
    correspondenceAddress = Some(address),
    telephoneNumber = None
  )

  val company = Company(
    name = "Company",
    referenceNumber = Some("AAA5221"),
    correspondenceAddress = address,
    telephoneNumber = "12345"
  )

  val leadTrusteeIndividual = LeadTrustee(
    individual = Some(individual),
    company = None
  )

  val leadTrusteeCompany = LeadTrustee(
    individual = None,
    company = Some(company)
  )
}