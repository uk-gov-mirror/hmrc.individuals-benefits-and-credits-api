/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.individualsbenefitsandcreditsapi.domains.integrationframework

import play.api.libs.functional.syntax.*
import play.api.libs.json.Reads.pattern
import play.api.libs.json.{Format, JsPath, Json}

case class IfApplication(
  id: Option[Double],
  ceasedDate: Option[String],
  entStartDate: Option[String],
  entEndDate: Option[String],
  awards: Option[Seq[IfAward]]
)

object IfApplication extends PatternsAndValidators {

  implicit val applicationFormat: Format[IfApplication] = Format(
    (
      (JsPath \ "id").readNullable[Double](using applicationIdValidator) and
        (JsPath \ "ceasedDate")
          .readNullable[String](using pattern(datePattern, "invalid date")) and
        (JsPath \ "entStartDate")
          .readNullable[String](using pattern(datePattern, "invalid date")) and
        (JsPath \ "entEndDate")
          .readNullable[String](using pattern(datePattern, "invalid date")) and
        (JsPath \ "awards").readNullable[Seq[IfAward]]
    )(IfApplication.apply),
    Json.writes[IfApplication]
  )
}
