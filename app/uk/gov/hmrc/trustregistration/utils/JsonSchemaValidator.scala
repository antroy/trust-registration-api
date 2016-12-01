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

package uk.gov.hmrc.trustregistration.utils

import com.fasterxml.jackson.core.{JsonFactory, JsonParser}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.LogLevel.ERROR
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.JsonSchemaFactory
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

trait ValidationResult


case class TrustsValidationError(message: String, location: String)

object TrustsValidationError {
  implicit val formats = Json.format[TrustsValidationError]
}

case class FailedValidation(message: String, code: Int, validationErrors: Seq[TrustsValidationError]) extends ValidationResult

object FailedValidation {
  implicit val formats = Json.format[FailedValidation]
}

case class SuccessfulValidation() extends ValidationResult

object SuccessfulValidation extends ValidationResult

trait SchemaValidator{

  def validateAgainstSchema(schema: String, input: String): ValidationResult = {
    try {
      val jsonToValidate = doNotAllowDuplicatedProperties(input)

      jsonToValidate match {
        case Success(json) => {
          val validator = JsonSchemaFactory.byDefault.getJsonSchema(JsonLoader.fromString(schema))
          val validationOutput = validator.validate(json, true)

          if (validationOutput.isSuccess) {
            SuccessfulValidation
          } else {
            FailedValidation("Invalid Json",0, getValidationErrors(validationOutput))
          }
        }
      }
    }
    catch {
      case ex: Exception => {
        if (ex.getMessage.contains("Duplicate")) {
          FailedValidation("Duplicated Elements", 0, Nil)
        } else {
          FailedValidation("Not JSON",0,Nil)
        }
      }
    }
  }

  private def getValidationErrors(validationOutput: ProcessingReport): Seq[TrustsValidationError] = {
    val validationErrors: Seq[TrustsValidationError] = validationOutput.iterator.asScala.toList.filter(m => m.getLogLevel == ERROR).map(m => {
      val error = m.asJson()
      val message = error.findValue("message").asText("")
      val location = error.findValue("instance").at("/pointer").asText()

      TrustsValidationError(message, if (location == "") "/" else location)
    })
    validationErrors
  }

  private def doNotAllowDuplicatedProperties(jsonNodeAsString: String): Try[JsonNode] = {
    val objectMapper: ObjectMapper = new ObjectMapper()
    objectMapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)

    val jsonFactory: JsonFactory = objectMapper.getFactory()
    val jsonParser: JsonParser = jsonFactory.createParser(jsonNodeAsString)

    objectMapper.readTree(jsonParser)

    val jsonAsNode: Try[JsonNode] = Try(JsonLoader.fromString(jsonNodeAsString))
    jsonAsNode
  }
}

object SchemaValidator extends SchemaValidator
