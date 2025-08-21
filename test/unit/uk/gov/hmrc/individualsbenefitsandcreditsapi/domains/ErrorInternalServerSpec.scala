/*
 * Copyright 2025 HM Revenue & Customs
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

package unit.uk.gov.hmrc.individualsbenefitsandcreditsapi.domains

import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.individualsbenefitsandcreditsapi.domains.ErrorInternalServer

implicit val errorInternalServerWrites: Writes[ErrorInternalServer] = Json.writes[ErrorInternalServer]

class ErrorInternalServerSpec extends PlaySpec {

  "ErrorInternalServer" should {

    "have the correct HTTP status, error code, and message" in {
      val error = ErrorInternalServer()

      error.httpStatusCode mustBe INTERNAL_SERVER_ERROR
      error.errorCode mustBe "INTERNAL_SERVER_ERROR"
      error.message mustBe "Failed to process request"
    }

    "be serializable to JSON" in {
      val error = ErrorInternalServer("Custom error message")
      error.errorMessage mustBe "Custom error message"
      error.errorCode mustBe "INTERNAL_SERVER_ERROR"
      error.httpStatusCode mustBe 500
    }

    "return the correct HTTP response" in {
      val error = ErrorInternalServer()

      val httpResponse = error.toHttpResponse
      httpResponse.header.status mustBe INTERNAL_SERVER_ERROR
    }
  }
}
