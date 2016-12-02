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
import com.github.fge.jsonschema.main.{JsonSchema, JsonSchemaFactory}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import uk.gov.hmrc.trustregistration.SchemaValidationExamples

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._


class JsonValidatorSpec extends PlaySpec  with SchemaValidationExamples{

   "JsonValidator" must {

     "read the schema and return a SuccessfulValidation" when {
       "when we have a non required field missing" in {
         val result = JsonSchemaValidator.validateAgainstSchema(multipleItemsSchema, validJson)

         result mustBe SuccessfulValidation
       }

       "a field matches a specified pattern" in {
         val result = JsonSchemaValidator.validateAgainstSchema(postcodeSchema, validPostcodeJson)

         result mustBe SuccessfulValidation
       }
     }

     "read the schema and return a FailedValidation" when {
       "we miss a required field" in {
         val result = JsonSchemaValidator.validateAgainstSchema(multipleItemsSchema, invalidJsonOneFieldMissing)

         result mustBe FailedValidation("Invalid Json",0,List(TrustsValidationError("""object has missing required properties (["message"])""", "/")))
       }
       "we miss 2 required fields" in {
         val result = JsonSchemaValidator.validateAgainstSchema(multipleItemsSchema, invalidJson)

         result mustBe FailedValidation("Invalid Json",0,List(TrustsValidationError("""object has missing required properties (["location","message"])""", "/")))
       }
       "a field has the wrong type" in {
         val result = JsonSchemaValidator.validateAgainstSchema(multipleItemsSchema, invalidTypeJson)

         result mustBe FailedValidation("Invalid Json",0,List(TrustsValidationError("""instance type (integer) does not match any allowed primitive type (allowed: ["string"])""", "/code")))
       }
       "a field exceeds the maximum length" in {
          val result = JsonSchemaValidator.validateAgainstSchema(maxLengthSchema, invalidLengthJson)

         result mustBe FailedValidation("Invalid Json",0,List(TrustsValidationError("""string "1234567890" is too long (length: 10, maximum allowed: 9)""", "/code")))
       }
       "a field doesn't match a specified pattern" in {
         val result = JsonSchemaValidator.validateAgainstSchema(postcodeSchema, invalidPostcodeJson)

         result mustBe FailedValidation("Invalid Json",0,List(TrustsValidationError("""ECMA 262 regex "^[A-Za-z0-9]{3,4} [A-Za-z0-9]{3}$" does not match input string "NOT A POSTCODE"""", "/postcode")))
       }
       "a required field is missing and one of the fields is the wrong type" in {
         val result = JsonSchemaValidator.validateAgainstSchema(multipleItemsSchema, invalidJsonMultipleErrors)

         result mustBe FailedValidation("Invalid Json",0,List(
           TrustsValidationError("""object has missing required properties (["code"])""", "/"),
           TrustsValidationError("""instance type (integer) does not match any allowed primitive type (allowed: ["string"])""", "/message")))
       }
       "a nested object has a missing field" in {
         val result = JsonSchemaValidator.validateAgainstSchema(nestedItemSchema, invalidNestedJsonOneFieldMissing)

         result mustBe FailedValidation("Invalid Json",0,List(TrustsValidationError("object has missing required properties ([\"message\"])", "/item")))
       }
       "we pass in some html rather than json" in {
         val result = JsonSchemaValidator.validateAgainstSchema(multipleItemsSchema, "<html></html>")

         result mustBe FailedValidation("Not JSON", 0, Nil)
       }
       "we pass duplicated elements" in {
         val result = JsonSchemaValidator.validateAgainstSchema(multipleItemsSchema, duplicatedElementsJson)

         result mustBe FailedValidation("Duplicated Elements", 0, Nil)
       }
     }
   }

  val duplicatedElementsJson: String =
    """
      {
        "message" : "test",
        "message" : "test"
      }
    """

  val invalidJsonMultipleErrors: String =
    """
      {
         "message" : 4444,
         "location" : "test"
      }
    """

  val validPostcodeJson: String =
    """
      {
         "postcode" : "NE98 1ZZ"
      }
    """

  val invalidPostcodeJson: String =
    """
      {
         "postcode" : "NOT A POSTCODE"
      }
    """

  val validJson: String =
    """
      |{
      |  "message" : "valid message",
      |  "code" : "valid code",
      |  "location" : "location"
      |}
    """.stripMargin

  val invalidJson: String =
    """
      |{
      |  "code" : "valid code"
      |}
    """.stripMargin

  val invalidJsonOneFieldMissing: String =
    """
      |{
      |  "location" : "test",
      |  "code" : "valid code"
      |}
    """.stripMargin

  val invalidNestedJsonOneFieldMissing: String =
    """
      {
        "item": {
          "code" : "12345"
        }
      }
    """

  val invalidTypeJson: String = """{"message" : "valid message", "code" : 5, "location":"this is a location"}"""

  val invalidLengthJson: String =
    """
      |{
      |  "code" : "1234567890"
      |}
    """.stripMargin

//  "JsonValidator" must {
//    //Happy Path
//    "read the schema and return a SuccessfulValidation" when {
//      "given a valid trust" in {
//
//        val result = schemaValidator.validateAgainstSchema(validTrust, "")
//        val res = result match {
//          case SuccessfulValidation => SuccessfulValidation
//          case f: FailedValidation => {
//            val messages: Seq[JsValue] = Json.parse(f.validationErrors.mkString) \\ "message"
//            println(s"validTrust =>${messages.map(_.as[String])}")
//          }
//        }
//        result mustBe SuccessfulValidation
//
//      }
//
//      "given a valid estate" in {
//
//        val result = schemaValidator.validateAgainstSchema(validEstate, "")
//        val res = result match {
//          case SuccessfulValidation => SuccessfulValidation
//          case f: FailedValidation => {
//            val messages: Seq[JsValue] = Json.parse(f.validationErrors.mkString) \\ "message"
//            println(s"validEstate =>${messages.map(_.as[String])}")
//          }
//        }
//        result mustBe SuccessfulValidation
//
//      }
//    }
//
//    //Sad Path
//    "read the schema and return an error message" when {
//      "given an invalid trust" in {
//        val result = schemaValidator.validateAgainstSchema(invalidTrustNoName, "")
//
//        val res = result match {
//          case SuccessfulValidation => SuccessfulValidation
//          case f: FailedValidation => {
//            f.validationErrors.map(_.message) must contain("Duplicate elements")
//            //f.validationErrors.map(_.message) must contain("object has missing required properties ([\"trustName\"])")
//          }
//        }
//      }
//
//      "given an invalid estate" in {
//        val result = schemaValidator.validateAgainstSchema(invalidEstateNoName, "")
//        val res = result match {
//          case SuccessfulValidation => SuccessfulValidation
//          case f: FailedValidation => {
//            val messages: Seq[JsValue] = Json.parse(f.validationErrors.mkString) \\ "message"
//            val location: Seq[JsValue] = Json.parse(f.validationErrors.mkString) \\ "location"
//            println(s"invalidEstateNoName =>${messages.map(_.as[String])}")
//            f.validationErrors.map(_.message) must contain("Duplicate elements")
//            //messages.map(_.as[String]) must contain("object has missing required properties ([\"estateName\"])")
//          }
//        }
//      }
//    }
//  }
//
//  val validTrust = """{
//      |	"@xmlns:xsi": "a",
//      |"trustEstate":{
//      |		"trust": {
//      |			"commencementDate": {
//      |				"$": "a"
//      |			},
//      |			"correspondenceAddress": {
//      |				"countryCode": {
//      |					"$": "AD"
//      |				},
//      |				"line1": {
//      |					"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |				},
//      |				"line2": {
//      |					"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |				},
//      |				"line3": {
//      |					"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |				},
//      |				"line4": {
//      |					"$": "aaaaaaaaaaaaaaaaa"
//      |				},
//      |				"postalCode": {
//      |					"$": "aaaaaaaaaa"
//      |				}
//      |			},
//      |			"currentYear": {
//      |				"$": "aaaa"
//      |			},
//      |			"declaration": {
//      |				"capacity": {
//      |					"$": "0001"
//      |				},
//      |				"confirmation": {
//      |					"$": true
//      |				},
//      |				"dateOfDeclaration": {
//      |					"$": "a"
//      |				},
//      |				"familyName": {
//      |					"$": "a"
//      |				},
//      |				"givenName": {
//      |					"$": "a"
//      |				},
//      |				"otherName": {
//      |					"$": "a"
//      |				},
//      |				"title": {
//      |					"$": "a"
//      |				}
//      |			},
//      |			"isNonResTypeIHTA84S218": {
//      |				"$": true
//      |			},
//      |			"isS218IHTA84": {
//      |				"$": true
//      |			},
//      |			"isTCGA925A": {
//      |				"$": true
//      |			},
//      |			"isTrustUkResident": {
//      |				"$": true
//      |			},
//      |			"leadTrustee": {
//      |				"individual": {
//      |					"correspondenceAddress": {
//      |						"countryCode": {
//      |							"$": "AD"
//      |						},
//      |						"line1": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line2": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line3": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line4": {
//      |							"$": "aaaaaaaaaaaaaaaaa"
//      |						},
//      |						"postalCode": {
//      |							"$": "aaaaaaaaaa"
//      |						}
//      |					},
//      |					"dateOfBirth": {
//      |						"$": "a"
//      |					},
//      |					"familyName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"givenName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"isUkNationalOrNonUkWithANino": {
//      |						"$": true
//      |					},
//      |					"nino": {
//      |						"$": "aaaaaaaaa"
//      |					},
//      |					"otherName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"passportOrIdCard": {
//      |						"countryOfIssue": {
//      |							"$": "AD"
//      |						},
//      |						"expiryDate": {
//      |							"$": "a"
//      |						},
//      |						"referenceNumber": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						}
//      |					},
//      |					"telephoneNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"title": {
//      |						"$": "a"
//      |					}
//      |				}
//      |			},
//      |			"legality": {
//      |				"administrationCountryCode": {
//      |					"$": "AD"
//      |				},
//      |				"governingCountryCode": {
//      |					"$": "AD"
//      |				},
//      |				"isEstablishedUnderScottishLaw": {
//      |					"$": true
//      |				},
//      |				"previousOffshoreCountryCode": {
//      |					"$": "AD"
//      |				}
//      |			},
//      |			"naturalPeople": {
//      |				"individuals": {
//      |					"correspondenceAddress": {
//      |						"countryCode": {
//      |							"$": "AD"
//      |						},
//      |						"line1": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line2": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line3": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line4": {
//      |							"$": "aaaaaaaaaaaaaaaaa"
//      |						},
//      |						"postalCode": {
//      |							"$": "aaaaaaaaaa"
//      |						}
//      |					},
//      |					"dateOfBirth": {
//      |						"$": "a"
//      |					},
//      |					"familyName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"givenName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"isUkNationalOrNonUkWithANino": {
//      |						"$": true
//      |					},
//      |					"nino": {
//      |						"$": "aaaaaaaaa"
//      |					},
//      |					"otherName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"passportOrIdCard": {
//      |						"countryOfIssue": {
//      |							"$": "AD"
//      |						},
//      |						"expiryDate": {
//      |							"$": "a"
//      |						},
//      |						"referenceNumber": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						}
//      |					},
//      |					"telephoneNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"title": {
//      |						"$": "a"
//      |					}
//      |				}
//      |			},
//      |			"nonResidentType": {
//      |				"$": "0001"
//      |			},
//      |			"protectors": {
//      |				"companies": {
//      |					"companyName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"correspondenceAddress": {
//      |						"countryCode": {
//      |							"$": "AD"
//      |						},
//      |						"line1": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line2": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line3": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line4": {
//      |							"$": "aaaaaaaaaaaaaaaaa"
//      |						},
//      |						"postalCode": {
//      |							"$": "aaaaaaaaaa"
//      |						}
//      |					},
//      |					"referenceNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"telephoneNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaa"
//      |					}
//      |				},
//      |				"individuals": {
//      |					"correspondenceAddress": {
//      |						"countryCode": {
//      |							"$": "AD"
//      |						},
//      |						"line1": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line2": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line3": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line4": {
//      |							"$": "aaaaaaaaaaaaaaaaa"
//      |						},
//      |						"postalCode": {
//      |							"$": "aaaaaaaaaa"
//      |						}
//      |					},
//      |					"dateOfBirth": {
//      |						"$": "a"
//      |					},
//      |					"familyName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"givenName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"isUkNationalOrNonUkWithANino": {
//      |						"$": true
//      |					},
//      |					"nino": {
//      |						"$": "aaaaaaaaa"
//      |					},
//      |					"otherName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"passportOrIdCard": {
//      |						"countryOfIssue": {
//      |							"$": "AD"
//      |						},
//      |						"expiryDate": {
//      |							"$": "a"
//      |						},
//      |						"referenceNumber": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						}
//      |					},
//      |					"telephoneNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"title": {
//      |						"$": "a"
//      |					}
//      |				}
//      |			},
//      |			"telephoneNumber": {
//      |				"$": "aaaaaaaaaaaaaaaaaaa"
//      |			},
//      |			"trustName": {
//      |				"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |			},
//      |			"trustTypeType": {
//      |				"employmentTrust": {
//      |					"assets": {
//      |						"businessAssets": {
//      |							"businessAsset": {
//      |								"businessDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"businessName": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"businessPayeRef": {
//      |									"$": "aaaaaaaaaaaaa"
//      |								},
//      |								"businessValue": {
//      |									"$": 0
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"lastValuationDate": {
//      |									"$": "a"
//      |								}
//      |							}
//      |						},
//      |						"monetaryAssets": {
//      |							"monetaryAsset": {
//      |								"value": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"otherAssets": {
//      |							"otherAsset": {
//      |								"OtherAssetDescription": {
//      |									"$": "a"
//      |								},
//      |								"lastValuationDate": {
//      |									"$": "a"
//      |								},
//      |								"value": {
//      |									"$": 1.1
//      |								}
//      |							}
//      |						},
//      |						"propertyAssets": {
//      |							"propertyAsset": {
//      |								"buildingLandName": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"propertyLandEvalDate": {
//      |									"$": "a"
//      |								},
//      |								"propertyLandValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"shareAssets": {
//      |							"shareAsset": {
//      |								"numberShares": {
//      |									"$": 1
//      |								},
//      |								"shareClass": {
//      |									"$": "a"
//      |								},
//      |								"shareCompanyRegistrationNumber": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"shareType": {
//      |									"$": "0001"
//      |								},
//      |								"shareValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"beneficiaries": {
//      |						"directorBeneficiaries": {
//      |							"directorBeneficiary": {
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								},
//      |								"individual": {
//      |									"correspondenceAddress": {
//      |										"countryCode": {
//      |											"$": "AD"
//      |										},
//      |										"line1": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line2": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line3": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line4": {
//      |											"$": "aaaaaaaaaaaaaaaaa"
//      |										},
//      |										"postalCode": {
//      |											"$": "aaaaaaaaaa"
//      |										}
//      |									},
//      |									"dateOfBirth": {
//      |										"$": "a"
//      |									},
//      |									"familyName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"givenName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"isUkNationalOrNonUkWithANino": {
//      |										"$": true
//      |									},
//      |									"nino": {
//      |										"$": "aaaaaaaaa"
//      |									},
//      |									"otherName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"passportOrIdCard": {
//      |										"countryOfIssue": {
//      |											"$": "AD"
//      |										},
//      |										"expiryDate": {
//      |											"$": "a"
//      |										},
//      |										"referenceNumber": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										}
//      |									},
//      |									"telephoneNumber": {
//      |										"$": "aaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"title": {
//      |										"$": "a"
//      |									}
//      |								},
//      |								"isVulnerable": {
//      |									"$": true
//      |								}
//      |							}
//      |						},
//      |						"employeeBeneficiaries": {
//      |							"employeeBeneficiary": {
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								},
//      |								"individual": {
//      |									"correspondenceAddress": {
//      |										"countryCode": {
//      |											"$": "AD"
//      |										},
//      |										"line1": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line2": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line3": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line4": {
//      |											"$": "aaaaaaaaaaaaaaaaa"
//      |										},
//      |										"postalCode": {
//      |											"$": "aaaaaaaaaa"
//      |										}
//      |									},
//      |									"dateOfBirth": {
//      |										"$": "a"
//      |									},
//      |									"familyName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"givenName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"isUkNationalOrNonUkWithANino": {
//      |										"$": true
//      |									},
//      |									"nino": {
//      |										"$": "aaaaaaaaa"
//      |									},
//      |									"otherName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"passportOrIdCard": {
//      |										"countryOfIssue": {
//      |											"$": "AD"
//      |										},
//      |										"expiryDate": {
//      |											"$": "a"
//      |										},
//      |										"referenceNumber": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										}
//      |									},
//      |									"telephoneNumber": {
//      |										"$": "aaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"title": {
//      |										"$": "a"
//      |									}
//      |								},
//      |								"isVulnerable": {
//      |									"$": true
//      |								}
//      |							}
//      |						},
//      |						"individualBeneficiaries": {
//      |							"individualBeneficiary": {
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								},
//      |								"individual": {
//      |									"correspondenceAddress": {
//      |										"countryCode": {
//      |											"$": "AD"
//      |										},
//      |										"line1": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line2": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line3": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line4": {
//      |											"$": "aaaaaaaaaaaaaaaaa"
//      |										},
//      |										"postalCode": {
//      |											"$": "aaaaaaaaaa"
//      |										}
//      |									},
//      |									"dateOfBirth": {
//      |										"$": "a"
//      |									},
//      |									"familyName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"givenName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"isUkNationalOrNonUkWithANino": {
//      |										"$": true
//      |									},
//      |									"nino": {
//      |										"$": "aaaaaaaaa"
//      |									},
//      |									"otherName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"passportOrIdCard": {
//      |										"countryOfIssue": {
//      |											"$": "AD"
//      |										},
//      |										"expiryDate": {
//      |											"$": "a"
//      |										},
//      |										"referenceNumber": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										}
//      |									},
//      |									"telephoneNumber": {
//      |										"$": "aaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"title": {
//      |										"$": "a"
//      |									}
//      |								},
//      |								"isVulnerable": {
//      |									"$": true
//      |								}
//      |							}
//      |						},
//      |						"otherBeneficiaries": {
//      |							"otherBeneficiary": {
//      |								"beneficiaryDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"employerFinancedRetirementBenefitSchemeStartDate": {
//      |						"$": "a"
//      |					},
//      |					"isEmployerFinancedRetirementBenefitScheme": {
//      |						"$": true
//      |					},
//      |					"settlors": {
//      |						"companies": {
//      |							"companyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"referenceNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							}
//      |						},
//      |						"individuals": {
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"dateOfBirth": {
//      |								"$": "a"
//      |							},
//      |							"familyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"givenName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"isUkNationalOrNonUkWithANino": {
//      |								"$": true
//      |							},
//      |							"nino": {
//      |								"$": "aaaaaaaaa"
//      |							},
//      |							"otherName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"passportOrIdCard": {
//      |								"countryOfIssue": {
//      |									"$": "AD"
//      |								},
//      |								"expiryDate": {
//      |									"$": "a"
//      |								},
//      |								"referenceNumber": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								}
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"title": {
//      |								"$": "a"
//      |							}
//      |						}
//      |					}
//      |				},
//      |				"flatManagementSinkingFundTrust": {
//      |					"assets": {
//      |						"monetaryAssets": {
//      |							"monetaryAsset": {
//      |								"value": {
//      |									"$": 0
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"beneficiaries": {
//      |						"buildingLandBeneficiary": {
//      |							"buildingBeneficiary": {
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						},
//      |						"otherBeneficiaries": {
//      |							"otherBeneficiary": {
//      |								"beneficiaryDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"settlors": {
//      |						"companies": {
//      |							"companyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"referenceNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							}
//      |						},
//      |						"individuals": {
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"dateOfBirth": {
//      |								"$": "a"
//      |							},
//      |							"familyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"givenName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"isUkNationalOrNonUkWithANino": {
//      |								"$": true
//      |							},
//      |							"nino": {
//      |								"$": "aaaaaaaaa"
//      |							},
//      |							"otherName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"passportOrIdCard": {
//      |								"countryOfIssue": {
//      |									"$": "AD"
//      |								},
//      |								"expiryDate": {
//      |									"$": "a"
//      |								},
//      |								"referenceNumber": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								}
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"title": {
//      |								"$": "a"
//      |							}
//      |						}
//      |					}
//      |				},
//      |				"heritageMaintenanceFundTrust": {
//      |					"assets": {
//      |						"monetaryAssets": {
//      |							"monetaryAsset": {
//      |								"value": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"otherAssets": {
//      |							"otherAsset": {
//      |								"OtherAssetDescription": {
//      |									"$": "a"
//      |								},
//      |								"lastValuationDate": {
//      |									"$": "a"
//      |								},
//      |								"value": {
//      |									"$": 1.1
//      |								}
//      |							}
//      |						},
//      |						"propertyAssets": {
//      |							"propertyAsset": {
//      |								"buildingLandName": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"propertyLandEvalDate": {
//      |									"$": "a"
//      |								},
//      |								"propertyLandValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"shareAssets": {
//      |							"shareAsset": {
//      |								"numberShares": {
//      |									"$": 1
//      |								},
//      |								"shareClass": {
//      |									"$": "a"
//      |								},
//      |								"shareCompanyRegistrationNumber": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"shareType": {
//      |									"$": "0001"
//      |								},
//      |								"shareValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"beneficiaries": {
//      |						"buildingLandBeneficiary": {
//      |							"buildingBeneficiary": {
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						},
//      |						"otherBeneficiaries": {
//      |							"otherBeneficiary": {
//      |								"beneficiaryDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"isMultiPurposeIncome": {
//      |						"$": true
//      |					},
//      |					"settlors": {
//      |						"companies": {
//      |							"companyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"referenceNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							}
//      |						},
//      |						"individuals": {
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"dateOfBirth": {
//      |								"$": "a"
//      |							},
//      |							"familyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"givenName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"isUkNationalOrNonUkWithANino": {
//      |								"$": true
//      |							},
//      |							"nino": {
//      |								"$": "aaaaaaaaa"
//      |							},
//      |							"otherName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"passportOrIdCard": {
//      |								"countryOfIssue": {
//      |									"$": "AD"
//      |								},
//      |								"expiryDate": {
//      |									"$": "a"
//      |								},
//      |								"referenceNumber": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								}
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"title": {
//      |								"$": "a"
//      |							}
//      |						}
//      |					}
//      |				},
//      |				"interVivoTrust": {
//      |					"assets": {
//      |						"businessAssets": {
//      |							"businessAsset": {
//      |								"businessDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"businessName": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"businessPayeRef": {
//      |									"$": "aaaaaaaaaaaaa"
//      |								},
//      |								"businessValue": {
//      |									"$": 0
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"lastValuationDate": {
//      |									"$": "a"
//      |								}
//      |							}
//      |						},
//      |						"monetaryAssets": {
//      |							"monetaryAsset": {
//      |								"value": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"otherAssets": {
//      |							"otherAsset": {
//      |								"OtherAssetDescription": {
//      |									"$": "a"
//      |								},
//      |								"lastValuationDate": {
//      |									"$": "a"
//      |								},
//      |								"value": {
//      |									"$": 1.1
//      |								}
//      |							}
//      |						},
//      |						"partnershipAssets": {
//      |							"partnershipAsset": {
//      |								"startOfPartnership": {
//      |									"$": "a"
//      |								},
//      |								"tradeOrProfession": {
//      |									"$": "a"
//      |								},
//      |								"utr": {
//      |									"$": "a"
//      |								},
//      |								"value": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"propertyAssets": {
//      |							"propertyAsset": {
//      |								"buildingLandName": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"propertyLandEvalDate": {
//      |									"$": "a"
//      |								},
//      |								"propertyLandValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"shareAssets": {
//      |							"shareAsset": {
//      |								"numberShares": {
//      |									"$": 1
//      |								},
//      |								"shareClass": {
//      |									"$": "a"
//      |								},
//      |								"shareCompanyRegistrationNumber": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"shareType": {
//      |									"$": "0001"
//      |								},
//      |								"shareValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"beneficiaries": {
//      |						"charityBeneficiaries": {
//      |							"charityBeneficiary": {
//      |								"charityName": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"charityNumber": {
//      |									"$": "aaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						},
//      |						"individualBeneficiaries": {
//      |							"individualBeneficiary": {
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								},
//      |								"individual": {
//      |									"correspondenceAddress": {
//      |										"countryCode": {
//      |											"$": "AD"
//      |										},
//      |										"line1": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line2": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line3": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line4": {
//      |											"$": "aaaaaaaaaaaaaaaaa"
//      |										},
//      |										"postalCode": {
//      |											"$": "aaaaaaaaaa"
//      |										}
//      |									},
//      |									"dateOfBirth": {
//      |										"$": "a"
//      |									},
//      |									"familyName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"givenName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"isUkNationalOrNonUkWithANino": {
//      |										"$": true
//      |									},
//      |									"nino": {
//      |										"$": "aaaaaaaaa"
//      |									},
//      |									"otherName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"passportOrIdCard": {
//      |										"countryOfIssue": {
//      |											"$": "AD"
//      |										},
//      |										"expiryDate": {
//      |											"$": "a"
//      |										},
//      |										"referenceNumber": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										}
//      |									},
//      |									"telephoneNumber": {
//      |										"$": "aaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"title": {
//      |										"$": "a"
//      |									}
//      |								},
//      |								"isVulnerable": {
//      |									"$": true
//      |								}
//      |							}
//      |						},
//      |						"otherBeneficiaries": {
//      |							"otherBeneficiary": {
//      |								"beneficiaryDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"isHoldOverClaimed": {
//      |						"$": true
//      |					},
//      |					"settlors": {
//      |						"companies": {
//      |							"companyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"referenceNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							}
//      |						},
//      |						"individuals": {
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"dateOfBirth": {
//      |								"$": "a"
//      |							},
//      |							"familyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"givenName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"isUkNationalOrNonUkWithANino": {
//      |								"$": true
//      |							},
//      |							"nino": {
//      |								"$": "aaaaaaaaa"
//      |							},
//      |							"otherName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"passportOrIdCard": {
//      |								"countryOfIssue": {
//      |									"$": "AD"
//      |								},
//      |								"expiryDate": {
//      |									"$": "a"
//      |								},
//      |								"referenceNumber": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								}
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"title": {
//      |								"$": "a"
//      |							}
//      |						}
//      |					}
//      |				},
//      |				"willIntestacyTrust": {
//      |					"assets": {
//      |						"businessAssets": {
//      |							"businessAsset": {
//      |								"businessDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"businessName": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"businessPayeRef": {
//      |									"$": "aaaaaaaaaaaaa"
//      |								},
//      |								"businessValue": {
//      |									"$": 0
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"lastValuationDate": {
//      |									"$": "a"
//      |								}
//      |							}
//      |						},
//      |						"monetaryAssets": {
//      |							"monetaryAsset": {
//      |								"value": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"otherAssets": {
//      |							"otherAsset": {
//      |								"OtherAssetDescription": {
//      |									"$": "a"
//      |								},
//      |								"lastValuationDate": {
//      |									"$": "a"
//      |								},
//      |								"value": {
//      |									"$": 1.1
//      |								}
//      |							}
//      |						},
//      |						"propertyAssets": {
//      |							"propertyAsset": {
//      |								"buildingLandName": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"propertyLandEvalDate": {
//      |									"$": "a"
//      |								},
//      |								"propertyLandValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"shareAssets": {
//      |							"shareAsset": {
//      |								"numberShares": {
//      |									"$": 1
//      |								},
//      |								"shareClass": {
//      |									"$": "a"
//      |								},
//      |								"shareCompanyRegistrationNumber": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"shareType": {
//      |									"$": "0001"
//      |								},
//      |								"shareValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"beneficiaries": {
//      |						"charityBeneficiaries": {
//      |							"charityBeneficiary": {
//      |								"charityName": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"charityNumber": {
//      |									"$": "aaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						},
//      |						"individualBeneficiaries": {
//      |							"individualBeneficiary": {
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								},
//      |								"individual": {
//      |									"correspondenceAddress": {
//      |										"countryCode": {
//      |											"$": "AD"
//      |										},
//      |										"line1": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line2": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line3": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line4": {
//      |											"$": "aaaaaaaaaaaaaaaaa"
//      |										},
//      |										"postalCode": {
//      |											"$": "aaaaaaaaaa"
//      |										}
//      |									},
//      |									"dateOfBirth": {
//      |										"$": "a"
//      |									},
//      |									"familyName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"givenName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"isUkNationalOrNonUkWithANino": {
//      |										"$": true
//      |									},
//      |									"nino": {
//      |										"$": "aaaaaaaaa"
//      |									},
//      |									"otherName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"passportOrIdCard": {
//      |										"countryOfIssue": {
//      |											"$": "AD"
//      |										},
//      |										"expiryDate": {
//      |											"$": "a"
//      |										},
//      |										"referenceNumber": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										}
//      |									},
//      |									"telephoneNumber": {
//      |										"$": "aaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"title": {
//      |										"$": "a"
//      |									}
//      |								},
//      |								"isVulnerable": {
//      |									"$": true
//      |								}
//      |							}
//      |						},
//      |						"otherBeneficiaries": {
//      |							"otherBeneficiary": {
//      |								"beneficiaryDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"deceased": {
//      |						"correspondenceAddress": {
//      |							"countryCode": {
//      |								"$": "AD"
//      |							},
//      |							"line1": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"line2": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"line3": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"line4": {
//      |								"$": "aaaaaaaaaaaaaaaaa"
//      |							},
//      |							"postalCode": {
//      |								"$": "aaaaaaaaaa"
//      |							}
//      |						},
//      |						"dateOfDeath": {
//      |							"$": "a"
//      |						},
//      |						"individual": {
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"dateOfBirth": {
//      |								"$": "a"
//      |							},
//      |							"familyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"givenName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"isUkNationalOrNonUkWithANino": {
//      |								"$": true
//      |							},
//      |							"nino": {
//      |								"$": "aaaaaaaaa"
//      |							},
//      |							"otherName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"passportOrIdCard": {
//      |								"countryOfIssue": {
//      |									"$": "AD"
//      |								},
//      |								"expiryDate": {
//      |									"$": "a"
//      |								},
//      |								"referenceNumber": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								}
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"title": {
//      |								"$": "a"
//      |							}
//      |						}
//      |					},
//      |					"settlors": {
//      |						"companies": {
//      |							"companyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"referenceNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							}
//      |						},
//      |						"individuals": {
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"dateOfBirth": {
//      |								"$": "a"
//      |							},
//      |							"familyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"givenName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"isUkNationalOrNonUkWithANino": {
//      |								"$": true
//      |							},
//      |							"nino": {
//      |								"$": "aaaaaaaaa"
//      |							},
//      |							"otherName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"passportOrIdCard": {
//      |								"countryOfIssue": {
//      |									"$": "AD"
//      |								},
//      |								"expiryDate": {
//      |									"$": "a"
//      |								},
//      |								"referenceNumber": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								}
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"title": {
//      |								"$": "a"
//      |							}
//      |						}
//      |					}
//      |				}
//      |			},
//      |			"trustees": {
//      |				"companies": {
//      |					"companyName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"correspondenceAddress": {
//      |						"countryCode": {
//      |							"$": "AD"
//      |						},
//      |						"line1": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line2": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line3": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line4": {
//      |							"$": "aaaaaaaaaaaaaaaaa"
//      |						},
//      |						"postalCode": {
//      |							"$": "aaaaaaaaaa"
//      |						}
//      |					},
//      |					"referenceNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"telephoneNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaa"
//      |					}
//      |				},
//      |				"individuals": {
//      |					"correspondenceAddress": {
//      |						"countryCode": {
//      |							"$": "AD"
//      |						},
//      |						"line1": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line2": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line3": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line4": {
//      |							"$": "aaaaaaaaaaaaaaaaa"
//      |						},
//      |						"postalCode": {
//      |							"$": "aaaaaaaaaa"
//      |						}
//      |					},
//      |					"dateOfBirth": {
//      |						"$": "a"
//      |					},
//      |					"familyName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"givenName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"isUkNationalOrNonUkWithANino": {
//      |						"$": true
//      |					},
//      |					"nino": {
//      |						"$": "aaaaaaaaa"
//      |					},
//      |					"otherName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"passportOrIdCard": {
//      |						"countryOfIssue": {
//      |							"$": "AD"
//      |						},
//      |						"expiryDate": {
//      |							"$": "a"
//      |						},
//      |						"referenceNumber": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						}
//      |					},
//      |					"telephoneNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"title": {
//      |						"$": "a"
//      |					}
//      |				}
//      |			}
//      |		}
//      |}
//      |}""".stripMargin
//
//  val invalidTrustNoName = """{
//      |	"@xmlns:xsi": "a",
//      |"trustEstate":{
//      |		"estate": {
//      |			"adminPeriodFinishedDate": {
//      |				"$": true
//      |			},
//      |			"deceased": {
//      |				"correspondenceAddress": {
//      |					"countryCode": {
//      |						"$": "AD"
//      |					},
//      |					"line1": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"line2": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"line3": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"line4": {
//      |						"$": "aaaaaaaaaaaaaaaaa"
//      |					},
//      |					"postalCode": {
//      |						"$": "aaaaaaaaaa"
//      |					}
//      |				},
//      |				"dateOfDeath": {
//      |					"$": "a"
//      |				},
//      |				"individual": {
//      |					"correspondenceAddress": {
//      |						"countryCode": {
//      |							"$": "AD"
//      |						},
//      |						"line1": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line2": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line3": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line4": {
//      |							"$": "aaaaaaaaaaaaaaaaa"
//      |						},
//      |						"postalCode": {
//      |							"$": "aaaaaaaaaa"
//      |						}
//      |					},
//      |					"dateOfBirth": {
//      |						"$": "a"
//      |					},
//      |					"familyName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"givenName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"isUkNationalOrNonUkWithANino": {
//      |						"$": true
//      |					},
//      |					"nino": {
//      |						"$": "aaaaaaaaa"
//      |					},
//      |					"otherName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"passportOrIdCard": {
//      |						"countryOfIssue": {
//      |							"$": "AD"
//      |						},
//      |						"expiryDate": {
//      |							"$": "a"
//      |						},
//      |						"referenceNumber": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						}
//      |					},
//      |					"telephoneNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"title": {
//      |						"$": "a"
//      |					}
//      |				}
//      |			},
//      |			"estateCriteriaMet": {
//      |				"$": true
//      |			},
//      |			"incomeTaxDueMoreThan10000": {
//      |				"$": true
//      |			},
//      |			"isCreatedByWill": {
//      |				"$": true
//      |			},
//      |			"personalRepresentative": {
//      |				"correspondenceAddress": {
//      |					"countryCode": {
//      |						"$": "AD"
//      |					},
//      |					"line1": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"line2": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"line3": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"line4": {
//      |						"$": "aaaaaaaaaaaaaaaaa"
//      |					},
//      |					"postalCode": {
//      |						"$": "aaaaaaaaaa"
//      |					}
//      |				},
//      |				"individual": {
//      |					"correspondenceAddress": {
//      |						"countryCode": {
//      |							"$": "AD"
//      |						},
//      |						"line1": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line2": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line3": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line4": {
//      |							"$": "aaaaaaaaaaaaaaaaa"
//      |						},
//      |						"postalCode": {
//      |							"$": "aaaaaaaaaa"
//      |						}
//      |					},
//      |					"dateOfBirth": {
//      |						"$": "a"
//      |					},
//      |					"familyName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"givenName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"isUkNationalOrNonUkWithANino": {
//      |						"$": true
//      |					},
//      |					"nino": {
//      |						"$": "aaaaaaaaa"
//      |					},
//      |					"otherName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"passportOrIdCard": {
//      |						"countryOfIssue": {
//      |							"$": "AD"
//      |						},
//      |						"expiryDate": {
//      |							"$": "a"
//      |						},
//      |						"referenceNumber": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						}
//      |					},
//      |					"telephoneNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"title": {
//      |						"$": "a"
//      |					}
//      |				},
//      |				"isExecutor": {
//      |					"$": true
//      |				},
//      |				"telephoneNumber": {
//      |					"$": "aaaaaaaaaaaaaaaaaaa"
//      |				}
//      |			},
//      |			"saleOfEstateAssetsMoreThan250000": {
//      |				"$": true
//      |			},
//      |			"saleOfEstateAssetsMoreThan500000": {
//      |				"$": true
//      |			},
//      |			"worthMoreThanTwoAndHalfMillionAtTimeOfDeath": {
//      |				"$": true
//      |			}
//      |		},
//      |		"trust": {
//      |			"commencementDate": {
//      |				"$": "a"
//      |			},
//      |			"correspondenceAddress": {
//      |				"countryCode": {
//      |					"$": "AD"
//      |				},
//      |				"line1": {
//      |					"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |				},
//      |				"line2": {
//      |					"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |				},
//      |				"line3": {
//      |					"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |				},
//      |				"line4": {
//      |					"$": "aaaaaaaaaaaaaaaaa"
//      |				},
//      |				"postalCode": {
//      |					"$": "aaaaaaaaaa"
//      |				}
//      |			},
//      |			"currentYear": {
//      |				"$": "aaaa"
//      |			},
//      |			"declaration": {
//      |				"capacity": {
//      |					"$": "0001"
//      |				},
//      |				"confirmation": {
//      |					"$": true
//      |				},
//      |				"dateOfDeclaration": {
//      |					"$": "a"
//      |				},
//      |				"familyName": {
//      |					"$": "a"
//      |				},
//      |				"givenName": {
//      |					"$": "a"
//      |				},
//      |				"otherName": {
//      |					"$": "a"
//      |				},
//      |				"title": {
//      |					"$": "a"
//      |				}
//      |			},
//      |			"isNonResTypeIHTA84S218": {
//      |				"$": true
//      |			},
//      |			"isS218IHTA84": {
//      |				"$": true
//      |			},
//      |			"isTCGA925A": {
//      |				"$": true
//      |			},
//      |			"isTrustUkResident": {
//      |				"$": true
//      |			},
//      |			"leadTrustee": {
//      |				"company": {
//      |					"companyName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"correspondenceAddress": {
//      |						"countryCode": {
//      |							"$": "AD"
//      |						},
//      |						"line1": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line2": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line3": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line4": {
//      |							"$": "aaaaaaaaaaaaaaaaa"
//      |						},
//      |						"postalCode": {
//      |							"$": "aaaaaaaaaa"
//      |						}
//      |					},
//      |					"referenceNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"telephoneNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaa"
//      |					}
//      |				},
//      |				"individual": {
//      |					"correspondenceAddress": {
//      |						"countryCode": {
//      |							"$": "AD"
//      |						},
//      |						"line1": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line2": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line3": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line4": {
//      |							"$": "aaaaaaaaaaaaaaaaa"
//      |						},
//      |						"postalCode": {
//      |							"$": "aaaaaaaaaa"
//      |						}
//      |					},
//      |					"dateOfBirth": {
//      |						"$": "a"
//      |					},
//      |					"familyName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"givenName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"isUkNationalOrNonUkWithANino": {
//      |						"$": true
//      |					},
//      |					"nino": {
//      |						"$": "aaaaaaaaa"
//      |					},
//      |					"otherName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"passportOrIdCard": {
//      |						"countryOfIssue": {
//      |							"$": "AD"
//      |						},
//      |						"expiryDate": {
//      |							"$": "a"
//      |						},
//      |						"referenceNumber": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						}
//      |					},
//      |					"telephoneNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"title": {
//      |						"$": "a"
//      |					}
//      |				}
//      |			},
//      |			"legality": {
//      |				"administrationCountryCode": {
//      |					"$": "AD"
//      |				},
//      |				"governingCountryCode": {
//      |					"$": "AD"
//      |				},
//      |				"isEstablishedUnderScottishLaw": {
//      |					"$": true
//      |				},
//      |				"previousOffshoreCountryCode": {
//      |					"$": "AD"
//      |				}
//      |			},
//      |			"naturalPeople": {
//      |				"individuals": {
//      |					"correspondenceAddress": {
//      |						"countryCode": {
//      |							"$": "AD"
//      |						},
//      |						"line1": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line2": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line3": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line4": {
//      |							"$": "aaaaaaaaaaaaaaaaa"
//      |						},
//      |						"postalCode": {
//      |							"$": "aaaaaaaaaa"
//      |						}
//      |					},
//      |					"dateOfBirth": {
//      |						"$": "a"
//      |					},
//      |					"familyName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"givenName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"isUkNationalOrNonUkWithANino": {
//      |						"$": true
//      |					},
//      |					"nino": {
//      |						"$": "aaaaaaaaa"
//      |					},
//      |					"otherName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"passportOrIdCard": {
//      |						"countryOfIssue": {
//      |							"$": "AD"
//      |						},
//      |						"expiryDate": {
//      |							"$": "a"
//      |						},
//      |						"referenceNumber": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						}
//      |					},
//      |					"telephoneNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"title": {
//      |						"$": "a"
//      |					}
//      |				}
//      |			},
//      |			"nonResidentType": {
//      |				"$": "0001"
//      |			},
//      |			"protectors": {
//      |				"companies": {
//      |					"companyName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"correspondenceAddress": {
//      |						"countryCode": {
//      |							"$": "AD"
//      |						},
//      |						"line1": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line2": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line3": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line4": {
//      |							"$": "aaaaaaaaaaaaaaaaa"
//      |						},
//      |						"postalCode": {
//      |							"$": "aaaaaaaaaa"
//      |						}
//      |					},
//      |					"referenceNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"telephoneNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaa"
//      |					}
//      |				},
//      |				"individuals": {
//      |					"correspondenceAddress": {
//      |						"countryCode": {
//      |							"$": "AD"
//      |						},
//      |						"line1": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line2": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line3": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line4": {
//      |							"$": "aaaaaaaaaaaaaaaaa"
//      |						},
//      |						"postalCode": {
//      |							"$": "aaaaaaaaaa"
//      |						}
//      |					},
//      |					"dateOfBirth": {
//      |						"$": "a"
//      |					},
//      |					"familyName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"givenName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"isUkNationalOrNonUkWithANino": {
//      |						"$": true
//      |					},
//      |					"nino": {
//      |						"$": "aaaaaaaaa"
//      |					},
//      |					"otherName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"passportOrIdCard": {
//      |						"countryOfIssue": {
//      |							"$": "AD"
//      |						},
//      |						"expiryDate": {
//      |							"$": "a"
//      |						},
//      |						"referenceNumber": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						}
//      |					},
//      |					"telephoneNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"title": {
//      |						"$": "a"
//      |					}
//      |				}
//      |			},
//      |			"telephoneNumber": {
//      |				"$": "aaaaaaaaaaaaaaaaaaa"
//      |			},
//      |			"trustTypeType": {
//      |				"employmentTrust": {
//      |					"assets": {
//      |						"businessAssets": {
//      |							"businessAsset": {
//      |								"businessDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"businessName": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"businessPayeRef": {
//      |									"$": "aaaaaaaaaaaaa"
//      |								},
//      |								"businessValue": {
//      |									"$": 0
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"lastValuationDate": {
//      |									"$": "a"
//      |								}
//      |							}
//      |						},
//      |						"monetaryAssets": {
//      |							"monetaryAsset": {
//      |								"value": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"otherAssets": {
//      |							"otherAsset": {
//      |								"OtherAssetDescription": {
//      |									"$": "a"
//      |								},
//      |								"lastValuationDate": {
//      |									"$": "a"
//      |								},
//      |								"value": {
//      |									"$": 1.1
//      |								}
//      |							}
//      |						},
//      |						"propertyAssets": {
//      |							"propertyAsset": {
//      |								"buildingLandName": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"propertyLandEvalDate": {
//      |									"$": "a"
//      |								},
//      |								"propertyLandValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"shareAssets": {
//      |							"shareAsset": {
//      |								"numberShares": {
//      |									"$": 1
//      |								},
//      |								"shareClass": {
//      |									"$": "a"
//      |								},
//      |								"shareCompanyRegistrationNumber": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"shareType": {
//      |									"$": "0001"
//      |								},
//      |								"shareValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"beneficiaries": {
//      |						"directorBeneficiaries": {
//      |							"directorBeneficiary": {
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								},
//      |								"individual": {
//      |									"correspondenceAddress": {
//      |										"countryCode": {
//      |											"$": "AD"
//      |										},
//      |										"line1": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line2": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line3": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line4": {
//      |											"$": "aaaaaaaaaaaaaaaaa"
//      |										},
//      |										"postalCode": {
//      |											"$": "aaaaaaaaaa"
//      |										}
//      |									},
//      |									"dateOfBirth": {
//      |										"$": "a"
//      |									},
//      |									"familyName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"givenName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"isUkNationalOrNonUkWithANino": {
//      |										"$": true
//      |									},
//      |									"nino": {
//      |										"$": "aaaaaaaaa"
//      |									},
//      |									"otherName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"passportOrIdCard": {
//      |										"countryOfIssue": {
//      |											"$": "AD"
//      |										},
//      |										"expiryDate": {
//      |											"$": "a"
//      |										},
//      |										"referenceNumber": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										}
//      |									},
//      |									"telephoneNumber": {
//      |										"$": "aaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"title": {
//      |										"$": "a"
//      |									}
//      |								},
//      |								"isVulnerable": {
//      |									"$": true
//      |								}
//      |							}
//      |						},
//      |						"employeeBeneficiaries": {
//      |							"employeeBeneficiary": {
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								},
//      |								"individual": {
//      |									"correspondenceAddress": {
//      |										"countryCode": {
//      |											"$": "AD"
//      |										},
//      |										"line1": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line2": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line3": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line4": {
//      |											"$": "aaaaaaaaaaaaaaaaa"
//      |										},
//      |										"postalCode": {
//      |											"$": "aaaaaaaaaa"
//      |										}
//      |									},
//      |									"dateOfBirth": {
//      |										"$": "a"
//      |									},
//      |									"familyName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"givenName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"isUkNationalOrNonUkWithANino": {
//      |										"$": true
//      |									},
//      |									"nino": {
//      |										"$": "aaaaaaaaa"
//      |									},
//      |									"otherName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"passportOrIdCard": {
//      |										"countryOfIssue": {
//      |											"$": "AD"
//      |										},
//      |										"expiryDate": {
//      |											"$": "a"
//      |										},
//      |										"referenceNumber": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										}
//      |									},
//      |									"telephoneNumber": {
//      |										"$": "aaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"title": {
//      |										"$": "a"
//      |									}
//      |								},
//      |								"isVulnerable": {
//      |									"$": true
//      |								}
//      |							}
//      |						},
//      |						"individualBeneficiaries": {
//      |							"individualBeneficiary": {
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								},
//      |								"individual": {
//      |									"correspondenceAddress": {
//      |										"countryCode": {
//      |											"$": "AD"
//      |										},
//      |										"line1": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line2": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line3": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line4": {
//      |											"$": "aaaaaaaaaaaaaaaaa"
//      |										},
//      |										"postalCode": {
//      |											"$": "aaaaaaaaaa"
//      |										}
//      |									},
//      |									"dateOfBirth": {
//      |										"$": "a"
//      |									},
//      |									"familyName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"givenName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"isUkNationalOrNonUkWithANino": {
//      |										"$": true
//      |									},
//      |									"nino": {
//      |										"$": "aaaaaaaaa"
//      |									},
//      |									"otherName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"passportOrIdCard": {
//      |										"countryOfIssue": {
//      |											"$": "AD"
//      |										},
//      |										"expiryDate": {
//      |											"$": "a"
//      |										},
//      |										"referenceNumber": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										}
//      |									},
//      |									"telephoneNumber": {
//      |										"$": "aaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"title": {
//      |										"$": "a"
//      |									}
//      |								},
//      |								"isVulnerable": {
//      |									"$": true
//      |								}
//      |							}
//      |						},
//      |						"otherBeneficiaries": {
//      |							"otherBeneficiary": {
//      |								"beneficiaryDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"employerFinancedRetirementBenefitSchemeStartDate": {
//      |						"$": "a"
//      |					},
//      |					"isEmployerFinancedRetirementBenefitScheme": {
//      |						"$": true
//      |					},
//      |					"settlors": {
//      |						"companies": {
//      |							"companyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"referenceNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							}
//      |						},
//      |						"individuals": {
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"dateOfBirth": {
//      |								"$": "a"
//      |							},
//      |							"familyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"givenName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"isUkNationalOrNonUkWithANino": {
//      |								"$": true
//      |							},
//      |							"nino": {
//      |								"$": "aaaaaaaaa"
//      |							},
//      |							"otherName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"passportOrIdCard": {
//      |								"countryOfIssue": {
//      |									"$": "AD"
//      |								},
//      |								"expiryDate": {
//      |									"$": "a"
//      |								},
//      |								"referenceNumber": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								}
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"title": {
//      |								"$": "a"
//      |							}
//      |						}
//      |					}
//      |				},
//      |				"flatManagementSinkingFundTrust": {
//      |					"assets": {
//      |						"monetaryAssets": {
//      |							"monetaryAsset": {
//      |								"value": {
//      |									"$": 0
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"beneficiaries": {
//      |						"buildingLandBeneficiary": {
//      |							"buildingBeneficiary": {
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						},
//      |						"otherBeneficiaries": {
//      |							"otherBeneficiary": {
//      |								"beneficiaryDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"settlors": {
//      |						"companies": {
//      |							"companyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"referenceNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							}
//      |						},
//      |						"individuals": {
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"dateOfBirth": {
//      |								"$": "a"
//      |							},
//      |							"familyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"givenName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"isUkNationalOrNonUkWithANino": {
//      |								"$": true
//      |							},
//      |							"nino": {
//      |								"$": "aaaaaaaaa"
//      |							},
//      |							"otherName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"passportOrIdCard": {
//      |								"countryOfIssue": {
//      |									"$": "AD"
//      |								},
//      |								"expiryDate": {
//      |									"$": "a"
//      |								},
//      |								"referenceNumber": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								}
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"title": {
//      |								"$": "a"
//      |							}
//      |						}
//      |					}
//      |				},
//      |				"heritageMaintenanceFundTrust": {
//      |					"assets": {
//      |						"monetaryAssets": {
//      |							"monetaryAsset": {
//      |								"value": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"otherAssets": {
//      |							"otherAsset": {
//      |								"OtherAssetDescription": {
//      |									"$": "a"
//      |								},
//      |								"lastValuationDate": {
//      |									"$": "a"
//      |								},
//      |								"value": {
//      |									"$": 1.1
//      |								}
//      |							}
//      |						},
//      |						"propertyAssets": {
//      |							"propertyAsset": {
//      |								"buildingLandName": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"propertyLandEvalDate": {
//      |									"$": "a"
//      |								},
//      |								"propertyLandValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"shareAssets": {
//      |							"shareAsset": {
//      |								"numberShares": {
//      |									"$": 1
//      |								},
//      |								"shareClass": {
//      |									"$": "a"
//      |								},
//      |								"shareCompanyRegistrationNumber": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"shareType": {
//      |									"$": "0001"
//      |								},
//      |								"shareValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"beneficiaries": {
//      |						"buildingLandBeneficiary": {
//      |							"buildingBeneficiary": {
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						},
//      |						"otherBeneficiaries": {
//      |							"otherBeneficiary": {
//      |								"beneficiaryDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"isMultiPurposeIncome": {
//      |						"$": true
//      |					},
//      |					"settlors": {
//      |						"companies": {
//      |							"companyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"referenceNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							}
//      |						},
//      |						"individuals": {
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"dateOfBirth": {
//      |								"$": "a"
//      |							},
//      |							"familyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"givenName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"isUkNationalOrNonUkWithANino": {
//      |								"$": true
//      |							},
//      |							"nino": {
//      |								"$": "aaaaaaaaa"
//      |							},
//      |							"otherName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"passportOrIdCard": {
//      |								"countryOfIssue": {
//      |									"$": "AD"
//      |								},
//      |								"expiryDate": {
//      |									"$": "a"
//      |								},
//      |								"referenceNumber": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								}
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"title": {
//      |								"$": "a"
//      |							}
//      |						}
//      |					}
//      |				},
//      |				"interVivoTrust": {
//      |					"assets": {
//      |						"businessAssets": {
//      |							"businessAsset": {
//      |								"businessDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"businessName": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"businessPayeRef": {
//      |									"$": "aaaaaaaaaaaaa"
//      |								},
//      |								"businessValue": {
//      |									"$": 0
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"lastValuationDate": {
//      |									"$": "a"
//      |								}
//      |							}
//      |						},
//      |						"monetaryAssets": {
//      |							"monetaryAsset": {
//      |								"value": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"otherAssets": {
//      |							"otherAsset": {
//      |								"OtherAssetDescription": {
//      |									"$": "a"
//      |								},
//      |								"lastValuationDate": {
//      |									"$": "a"
//      |								},
//      |								"value": {
//      |									"$": 1.1
//      |								}
//      |							}
//      |						},
//      |						"partnershipAssets": {
//      |							"partnershipAsset": {
//      |								"startOfPartnership": {
//      |									"$": "a"
//      |								},
//      |								"tradeOrProfession": {
//      |									"$": "a"
//      |								},
//      |								"utr": {
//      |									"$": "a"
//      |								},
//      |								"value": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"propertyAssets": {
//      |							"propertyAsset": {
//      |								"buildingLandName": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"propertyLandEvalDate": {
//      |									"$": "a"
//      |								},
//      |								"propertyLandValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"shareAssets": {
//      |							"shareAsset": {
//      |								"numberShares": {
//      |									"$": 1
//      |								},
//      |								"shareClass": {
//      |									"$": "a"
//      |								},
//      |								"shareCompanyRegistrationNumber": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"shareType": {
//      |									"$": "0001"
//      |								},
//      |								"shareValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"beneficiaries": {
//      |						"charityBeneficiaries": {
//      |							"charityBeneficiary": {
//      |								"charityName": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"charityNumber": {
//      |									"$": "aaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						},
//      |						"individualBeneficiaries": {
//      |							"individualBeneficiary": {
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								},
//      |								"individual": {
//      |									"correspondenceAddress": {
//      |										"countryCode": {
//      |											"$": "AD"
//      |										},
//      |										"line1": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line2": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line3": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line4": {
//      |											"$": "aaaaaaaaaaaaaaaaa"
//      |										},
//      |										"postalCode": {
//      |											"$": "aaaaaaaaaa"
//      |										}
//      |									},
//      |									"dateOfBirth": {
//      |										"$": "a"
//      |									},
//      |									"familyName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"givenName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"isUkNationalOrNonUkWithANino": {
//      |										"$": true
//      |									},
//      |									"nino": {
//      |										"$": "aaaaaaaaa"
//      |									},
//      |									"otherName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"passportOrIdCard": {
//      |										"countryOfIssue": {
//      |											"$": "AD"
//      |										},
//      |										"expiryDate": {
//      |											"$": "a"
//      |										},
//      |										"referenceNumber": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										}
//      |									},
//      |									"telephoneNumber": {
//      |										"$": "aaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"title": {
//      |										"$": "a"
//      |									}
//      |								},
//      |								"isVulnerable": {
//      |									"$": true
//      |								}
//      |							}
//      |						},
//      |						"otherBeneficiaries": {
//      |							"otherBeneficiary": {
//      |								"beneficiaryDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"isHoldOverClaimed": {
//      |						"$": true
//      |					},
//      |					"settlors": {
//      |						"companies": {
//      |							"companyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"referenceNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							}
//      |						},
//      |						"individuals": {
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"dateOfBirth": {
//      |								"$": "a"
//      |							},
//      |							"familyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"givenName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"isUkNationalOrNonUkWithANino": {
//      |								"$": true
//      |							},
//      |							"nino": {
//      |								"$": "aaaaaaaaa"
//      |							},
//      |							"otherName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"passportOrIdCard": {
//      |								"countryOfIssue": {
//      |									"$": "AD"
//      |								},
//      |								"expiryDate": {
//      |									"$": "a"
//      |								},
//      |								"referenceNumber": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								}
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"title": {
//      |								"$": "a"
//      |							}
//      |						}
//      |					}
//      |				},
//      |				"willIntestacyTrust": {
//      |					"assets": {
//      |						"businessAssets": {
//      |							"businessAsset": {
//      |								"businessDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"businessName": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"businessPayeRef": {
//      |									"$": "aaaaaaaaaaaaa"
//      |								},
//      |								"businessValue": {
//      |									"$": 0
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"lastValuationDate": {
//      |									"$": "a"
//      |								}
//      |							}
//      |						},
//      |						"monetaryAssets": {
//      |							"monetaryAsset": {
//      |								"value": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"otherAssets": {
//      |							"otherAsset": {
//      |								"OtherAssetDescription": {
//      |									"$": "a"
//      |								},
//      |								"lastValuationDate": {
//      |									"$": "a"
//      |								},
//      |								"value": {
//      |									"$": 1.1
//      |								}
//      |							}
//      |						},
//      |						"propertyAssets": {
//      |							"propertyAsset": {
//      |								"buildingLandName": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"propertyLandEvalDate": {
//      |									"$": "a"
//      |								},
//      |								"propertyLandValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						},
//      |						"shareAssets": {
//      |							"shareAsset": {
//      |								"numberShares": {
//      |									"$": 1
//      |								},
//      |								"shareClass": {
//      |									"$": "a"
//      |								},
//      |								"shareCompanyRegistrationNumber": {
//      |									"$": "aaaaaaaaaa"
//      |								},
//      |								"shareType": {
//      |									"$": "0001"
//      |								},
//      |								"shareValue": {
//      |									"$": 0
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"beneficiaries": {
//      |						"charityBeneficiaries": {
//      |							"charityBeneficiary": {
//      |								"charityName": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"charityNumber": {
//      |									"$": "aaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						},
//      |						"individualBeneficiaries": {
//      |							"individualBeneficiary": {
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								},
//      |								"individual": {
//      |									"correspondenceAddress": {
//      |										"countryCode": {
//      |											"$": "AD"
//      |										},
//      |										"line1": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line2": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line3": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										},
//      |										"line4": {
//      |											"$": "aaaaaaaaaaaaaaaaa"
//      |										},
//      |										"postalCode": {
//      |											"$": "aaaaaaaaaa"
//      |										}
//      |									},
//      |									"dateOfBirth": {
//      |										"$": "a"
//      |									},
//      |									"familyName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"givenName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"isUkNationalOrNonUkWithANino": {
//      |										"$": true
//      |									},
//      |									"nino": {
//      |										"$": "aaaaaaaaa"
//      |									},
//      |									"otherName": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"passportOrIdCard": {
//      |										"countryOfIssue": {
//      |											"$": "AD"
//      |										},
//      |										"expiryDate": {
//      |											"$": "a"
//      |										},
//      |										"referenceNumber": {
//      |											"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |										}
//      |									},
//      |									"telephoneNumber": {
//      |										"$": "aaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"title": {
//      |										"$": "a"
//      |									}
//      |								},
//      |								"isVulnerable": {
//      |									"$": true
//      |								}
//      |							}
//      |						},
//      |						"otherBeneficiaries": {
//      |							"otherBeneficiary": {
//      |								"beneficiaryDescription": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"correspondenceAddress": {
//      |									"countryCode": {
//      |										"$": "AD"
//      |									},
//      |									"line1": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line2": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line3": {
//      |										"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |									},
//      |									"line4": {
//      |										"$": "aaaaaaaaaaaaaaaaa"
//      |									},
//      |									"postalCode": {
//      |										"$": "aaaaaaaaaa"
//      |									}
//      |								},
//      |								"income": {
//      |									"isIncomeAtTrusteeDiscretion": {
//      |										"$": true
//      |									},
//      |									"shareOfIncome": {
//      |										"$": 0
//      |									}
//      |								}
//      |							}
//      |						}
//      |					},
//      |					"deceased": {
//      |						"correspondenceAddress": {
//      |							"countryCode": {
//      |								"$": "AD"
//      |							},
//      |							"line1": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"line2": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"line3": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"line4": {
//      |								"$": "aaaaaaaaaaaaaaaaa"
//      |							},
//      |							"postalCode": {
//      |								"$": "aaaaaaaaaa"
//      |							}
//      |						},
//      |						"dateOfDeath": {
//      |							"$": "a"
//      |						},
//      |						"individual": {
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"dateOfBirth": {
//      |								"$": "a"
//      |							},
//      |							"familyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"givenName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"isUkNationalOrNonUkWithANino": {
//      |								"$": true
//      |							},
//      |							"nino": {
//      |								"$": "aaaaaaaaa"
//      |							},
//      |							"otherName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"passportOrIdCard": {
//      |								"countryOfIssue": {
//      |									"$": "AD"
//      |								},
//      |								"expiryDate": {
//      |									"$": "a"
//      |								},
//      |								"referenceNumber": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								}
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"title": {
//      |								"$": "a"
//      |							}
//      |						}
//      |					},
//      |					"settlors": {
//      |						"companies": {
//      |							"companyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"referenceNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							}
//      |						},
//      |						"individuals": {
//      |							"correspondenceAddress": {
//      |								"countryCode": {
//      |									"$": "AD"
//      |								},
//      |								"line1": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line2": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line3": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								},
//      |								"line4": {
//      |									"$": "aaaaaaaaaaaaaaaaa"
//      |								},
//      |								"postalCode": {
//      |									"$": "aaaaaaaaaa"
//      |								}
//      |							},
//      |							"dateOfBirth": {
//      |								"$": "a"
//      |							},
//      |							"familyName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"givenName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"isUkNationalOrNonUkWithANino": {
//      |								"$": true
//      |							},
//      |							"nino": {
//      |								"$": "aaaaaaaaa"
//      |							},
//      |							"otherName": {
//      |								"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"passportOrIdCard": {
//      |								"countryOfIssue": {
//      |									"$": "AD"
//      |								},
//      |								"expiryDate": {
//      |									"$": "a"
//      |								},
//      |								"referenceNumber": {
//      |									"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |								}
//      |							},
//      |							"telephoneNumber": {
//      |								"$": "aaaaaaaaaaaaaaaaaaa"
//      |							},
//      |							"title": {
//      |								"$": "a"
//      |							}
//      |						}
//      |					}
//      |				}
//      |			},
//      |			"trustees": {
//      |				"companies": {
//      |					"companyName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"correspondenceAddress": {
//      |						"countryCode": {
//      |							"$": "AD"
//      |						},
//      |						"line1": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line2": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line3": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line4": {
//      |							"$": "aaaaaaaaaaaaaaaaa"
//      |						},
//      |						"postalCode": {
//      |							"$": "aaaaaaaaaa"
//      |						}
//      |					},
//      |					"referenceNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"telephoneNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaa"
//      |					}
//      |				},
//      |				"individuals": {
//      |					"correspondenceAddress": {
//      |						"countryCode": {
//      |							"$": "AD"
//      |						},
//      |						"line1": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line2": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line3": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						},
//      |						"line4": {
//      |							"$": "aaaaaaaaaaaaaaaaa"
//      |						},
//      |						"postalCode": {
//      |							"$": "aaaaaaaaaa"
//      |						}
//      |					},
//      |					"dateOfBirth": {
//      |						"$": "a"
//      |					},
//      |					"familyName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"givenName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"isUkNationalOrNonUkWithANino": {
//      |						"$": true
//      |					},
//      |					"nino": {
//      |						"$": "aaaaaaaaa"
//      |					},
//      |					"otherName": {
//      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"passportOrIdCard": {
//      |						"countryOfIssue": {
//      |							"$": "AD"
//      |						},
//      |						"expiryDate": {
//      |							"$": "a"
//      |						},
//      |						"referenceNumber": {
//      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//      |						}
//      |					},
//      |					"telephoneNumber": {
//      |						"$": "aaaaaaaaaaaaaaaaaaa"
//      |					},
//      |					"title": {
//      |						"$": "a"
//      |					}
//      |				}
//      |			}
//      |		}
//      |}
//      |}""".stripMargin
//
//  val validEstate = """{
//                      |	"@xmlns:xsi": "a",
//                      |	"trustEstate": {
//                      |		"estate": {
//                      |			"estateName": {
//                      |				"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |			},
//                      |			"adminPeriodFinishedDate": {
//                      |				"$": true
//                      |			},
//                      |			"deceased": {
//                      |				"correspondenceAddress": {
//                      |					"countryCode": {
//                      |						"$": "AD"
//                      |					},
//                      |					"line1": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"line2": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"line3": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"line4": {
//                      |						"$": "aaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"postalCode": {
//                      |						"$": "aaaaaaaaaa"
//                      |					}
//                      |				},
//                      |				"dateOfDeath": {
//                      |					"$": "a"
//                      |				},
//                      |				"individual": {
//                      |					"correspondenceAddress": {
//                      |						"countryCode": {
//                      |							"$": "AD"
//                      |						},
//                      |						"line1": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"line2": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"line3": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"line4": {
//                      |							"$": "aaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"postalCode": {
//                      |							"$": "aaaaaaaaaa"
//                      |						}
//                      |					},
//                      |					"dateOfBirth": {
//                      |						"$": "a"
//                      |					},
//                      |					"familyName": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"givenName": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"isUkNationalOrNonUkWithANino": {
//                      |						"$": true
//                      |					},
//                      |					"nino": {
//                      |						"$": "aaaaaaaaa"
//                      |					},
//                      |					"otherName": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"passportOrIdCard": {
//                      |						"countryOfIssue": {
//                      |							"$": "AD"
//                      |						},
//                      |						"expiryDate": {
//                      |							"$": "a"
//                      |						},
//                      |						"referenceNumber": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						}
//                      |					},
//                      |					"telephoneNumber": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"title": {
//                      |						"$": "a"
//                      |					}
//                      |				}
//                      |			},
//                      |			"estateCriteriaMet": {
//                      |				"$": true
//                      |			},
//                      |			"incomeTaxDueMoreThan10000": {
//                      |				"$": true
//                      |			},
//                      |			"isCreatedByWill": {
//                      |				"$": true
//                      |			},
//                      |			"personalRepresentative": {
//                      |				"correspondenceAddress": {
//                      |					"countryCode": {
//                      |						"$": "AD"
//                      |					},
//                      |					"line1": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"line2": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"line3": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"line4": {
//                      |						"$": "aaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"postalCode": {
//                      |						"$": "aaaaaaaaaa"
//                      |					}
//                      |				},
//                      |				"individual": {
//                      |					"correspondenceAddress": {
//                      |						"countryCode": {
//                      |							"$": "AD"
//                      |						},
//                      |						"line1": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"line2": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"line3": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"line4": {
//                      |							"$": "aaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"postalCode": {
//                      |							"$": "aaaaaaaaaa"
//                      |						}
//                      |					},
//                      |					"dateOfBirth": {
//                      |						"$": "a"
//                      |					},
//                      |					"familyName": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"givenName": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"isUkNationalOrNonUkWithANino": {
//                      |						"$": true
//                      |					},
//                      |					"nino": {
//                      |						"$": "aaaaaaaaa"
//                      |					},
//                      |					"otherName": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"passportOrIdCard": {
//                      |						"countryOfIssue": {
//                      |							"$": "AD"
//                      |						},
//                      |						"expiryDate": {
//                      |							"$": "a"
//                      |						},
//                      |						"referenceNumber": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						}
//                      |					},
//                      |					"telephoneNumber": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"title": {
//                      |						"$": "a"
//                      |					}
//                      |				},
//                      |				"isExecutor": {
//                      |					"$": true
//                      |				},
//                      |				"telephoneNumber": {
//                      |					"$": "aaaaaaaaaaaaaaaaaaa"
//                      |				}
//                      |			},
//                      |			"saleOfEstateAssetsMoreThan250000": {
//                      |				"$": true
//                      |			},
//                      |			"saleOfEstateAssetsMoreThan500000": {
//                      |				"$": true
//                      |			},
//                      |			"worthMoreThanTwoAndHalfMillionAtTimeOfDeath": {
//                      |				"$": true
//                      |			}
//                      |		}
//                      |	}
//                      |}""".stripMargin
//
//  val invalidEstateNoName = """{
//                      |	"@xmlns:xsi": "a",
//                      |	"trustEstate": {
//                      |		"estate": {
//                      |			"adminPeriodFinishedDate": {
//                      |				"$": true
//                      |			},
//                      |			"deceased": {
//                      |				"correspondenceAddress": {
//                      |					"countryCode": {
//                      |						"$": "AD"
//                      |					},
//                      |					"line1": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"line2": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"line3": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"line4": {
//                      |						"$": "aaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"postalCode": {
//                      |						"$": "aaaaaaaaaa"
//                      |					}
//                      |				},
//                      |				"dateOfDeath": {
//                      |					"$": "a"
//                      |				},
//                      |				"individual": {
//                      |					"correspondenceAddress": {
//                      |						"countryCode": {
//                      |							"$": "AD"
//                      |						},
//                      |						"line1": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"line2": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"line3": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"line4": {
//                      |							"$": "aaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"postalCode": {
//                      |							"$": "aaaaaaaaaa"
//                      |						}
//                      |					},
//                      |					"dateOfBirth": {
//                      |						"$": "a"
//                      |					},
//                      |					"familyName": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"givenName": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"isUkNationalOrNonUkWithANino": {
//                      |						"$": true
//                      |					},
//                      |					"nino": {
//                      |						"$": "aaaaaaaaa"
//                      |					},
//                      |					"otherName": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"passportOrIdCard": {
//                      |						"countryOfIssue": {
//                      |							"$": "AD"
//                      |						},
//                      |						"expiryDate": {
//                      |							"$": "a"
//                      |						},
//                      |						"referenceNumber": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						}
//                      |					},
//                      |					"telephoneNumber": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"title": {
//                      |						"$": "a"
//                      |					}
//                      |				}
//                      |			},
//                      |			"estateCriteriaMet": {
//                      |				"$": true
//                      |			},
//                      |			"incomeTaxDueMoreThan10000": {
//                      |				"$": true
//                      |			},
//                      |			"isCreatedByWill": {
//                      |				"$": true
//                      |			},
//                      |			"personalRepresentative": {
//                      |				"correspondenceAddress": {
//                      |					"countryCode": {
//                      |						"$": "AD"
//                      |					},
//                      |					"line1": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"line2": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"line3": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"line4": {
//                      |						"$": "aaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"postalCode": {
//                      |						"$": "aaaaaaaaaa"
//                      |					}
//                      |				},
//                      |				"individual": {
//                      |					"correspondenceAddress": {
//                      |						"countryCode": {
//                      |							"$": "AD"
//                      |						},
//                      |						"line1": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"line2": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"line3": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"line4": {
//                      |							"$": "aaaaaaaaaaaaaaaaa"
//                      |						},
//                      |						"postalCode": {
//                      |							"$": "aaaaaaaaaa"
//                      |						}
//                      |					},
//                      |					"dateOfBirth": {
//                      |						"$": "a"
//                      |					},
//                      |					"familyName": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"givenName": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"isUkNationalOrNonUkWithANino": {
//                      |						"$": true
//                      |					},
//                      |					"nino": {
//                      |						"$": "aaaaaaaaa"
//                      |					},
//                      |					"otherName": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"passportOrIdCard": {
//                      |						"countryOfIssue": {
//                      |							"$": "AD"
//                      |						},
//                      |						"expiryDate": {
//                      |							"$": "a"
//                      |						},
//                      |						"referenceNumber": {
//                      |							"$": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
//                      |						}
//                      |					},
//                      |					"telephoneNumber": {
//                      |						"$": "aaaaaaaaaaaaaaaaaaa"
//                      |					},
//                      |					"title": {
//                      |						"$": "a"
//                      |					}
//                      |				},
//                      |				"isExecutor": {
//                      |					"$": true
//                      |				},
//                      |				"telephoneNumber": {
//                      |					"$": "aaaaaaaaaaaaaaaaaaa"
//                      |				}
//                      |			},
//                      |			"saleOfEstateAssetsMoreThan250000": {
//                      |				"$": true
//                      |			},
//                      |			"saleOfEstateAssetsMoreThan500000": {
//                      |				"$": true
//                      |			},
//                      |			"worthMoreThanTwoAndHalfMillionAtTimeOfDeath": {
//                      |				"$": true
//                      |			}
//                      |		}
//                      |	}
//                      |}""".stripMargin

}
