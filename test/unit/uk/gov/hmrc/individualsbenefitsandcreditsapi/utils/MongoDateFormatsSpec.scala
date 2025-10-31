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

package unit.uk.gov.hmrc.individualsbenefitsandcreditsapi.utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.individualsbenefitsandcreditsapi.utils.MongoDateFormats

import java.time.{LocalDateTime, ZoneOffset}

class MongoDateFormatsSpec extends AnyWordSpec with Matchers with MongoDateFormats {

  "MongoDateFormats" should {
    val testDateTime = LocalDateTime.of(2023, 1, 1, 12, 30, 45)
    val testEpochMillis = testDateTime.toInstant(ZoneOffset.UTC).toEpochMilli

    val testJson = Json.obj(
      "$date" -> Json.obj(
        "$numberLong" -> testEpochMillis.toString
      )
    )

    "correctly serialize LocalDateTime to MongoDB date format" in {
      val result = localDateTimeFormat.writes(testDateTime)
      result shouldBe testJson
    }

    "correctly deserialize MongoDB date format to LocalDateTime" in {
      val result = localDateTimeFormat.reads(testJson)
      result.get shouldBe testDateTime
    }

    "round-trip LocalDateTime through JSON serialization" in {
      val json = localDateTimeFormat.writes(testDateTime)
      val result = localDateTimeFormat.reads(json)
      result.get shouldBe testDateTime
    }

    "handle the format correctly in the Format composition" in {
      val format = localDateTimeFormat
      val json = format.writes(testDateTime)
      val result = format.reads(json)
      result.get shouldBe testDateTime
    }

    "fail to deserialize when JSON is in wrong format" in {
      val invalidJson = Json.obj("invalid" -> "format")
      val result = localDateTimeFormat.reads(invalidJson)
      result.isError shouldBe true
    }
  }
}
