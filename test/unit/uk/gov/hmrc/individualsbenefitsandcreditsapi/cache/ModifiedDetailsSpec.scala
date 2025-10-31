/*
 * Copyright 2024 HM Revenue & Customs
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

package unit.uk.gov.hmrc.individualsbenefitsandcreditsapi.cache

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.individualsbenefitsandcreditsapi.cache.ModifiedDetails
import uk.gov.hmrc.individualsbenefitsandcreditsapi.utils.MongoDateFormats

import java.time.{LocalDateTime, ZoneOffset}

class ModifiedDetailsSpec extends AnyWordSpec with Matchers with MongoDateFormats {

  val createdAt: LocalDateTime = LocalDateTime.of(2023, 5, 10, 12, 30)
  val lastUpdated: LocalDateTime = LocalDateTime.of(2023, 6, 15, 14, 45)
  private val testModifiedDetails = ModifiedDetails(
    createdAt = createdAt,
    lastUpdated = lastUpdated
  )

  private val testJson: JsValue = Json.obj(
    "createdAt" -> Json.obj(
      "$date" -> Json.obj(
        "$numberLong" -> createdAt.toInstant(ZoneOffset.UTC).toEpochMilli.toString
      )
    ),
    "lastUpdated" -> Json.obj(
      "$date" -> Json.obj(
        "$numberLong" -> lastUpdated.toInstant(ZoneOffset.UTC).toEpochMilli.toString
      )
    )
  )

  "ModifiedDetails" should {
    "correctly serialize to JSON" in {
      val result = Json.toJson(testModifiedDetails)
      result shouldBe testJson
    }

    "correctly deserialize from JSON" in {
      val result = Json.fromJson[ModifiedDetails](testJson)
      result.get shouldBe testModifiedDetails
    }
  }
}
