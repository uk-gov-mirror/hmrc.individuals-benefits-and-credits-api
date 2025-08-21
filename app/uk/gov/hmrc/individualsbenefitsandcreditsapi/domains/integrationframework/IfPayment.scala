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
import play.api.libs.json.*
import play.api.libs.json.Reads.*

case class IfPayment(
  periodStartDate: Option[String],
  periodEndDate: Option[String],
  startDate: Option[String],
  endDate: Option[String],
  status: Option[String],
  postedDate: Option[String],
  nextDueDate: Option[String],
  frequency: Option[Int],
  tcType: Option[String],
  amount: Option[Double],
  method: Option[String]
)

object IfPayment extends PatternsAndValidators {
  implicit val paymentsFormat: Format[IfPayment] = Format(
    (
      (JsPath \ "periodStartDate").readNullable[String](using pattern(datePattern, "invalid date")) and
        (JsPath \ "periodEndDate").readNullable[String](using pattern(datePattern, "invalid date")) and
        (JsPath \ "startDate").readNullable[String](using pattern(datePattern, "invalid date")) and
        (JsPath \ "endDate").readNullable[String](using pattern(datePattern, "invalid date")) and
        (JsPath \ "status").readNullable[String](using pattern(statusPattern, "invalid status")) and
        (JsPath \ "postedDate").readNullable[String](using pattern(datePattern, "invalid date")) and
        (JsPath \ "nextDueDate").readNullable[String](using pattern(datePattern, "invalid date")) and
        (JsPath \ "frequency").readNullable[Int](using min[Int](1).keepAnd(max[Int](999))) and
        (JsPath \ "tcType").readNullable[String](using pattern(tcTypePattern, "invalid tc type")) and
        (JsPath \ "amount").readNullable[Double](using paymentAmountValidator) and
        (JsPath \ "method").readNullable[String](using pattern(methodPattern, "invalid method"))
    )(IfPayment.apply),
    Json.writes[IfPayment]
  )
}
