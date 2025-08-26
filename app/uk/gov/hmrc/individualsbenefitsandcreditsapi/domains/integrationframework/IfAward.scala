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

case class IfAward(
  payProfCalcDate: Option[String],
  startDate: Option[String],
  endDate: Option[String],
  totalEntitlement: Option[Double],
  workingTaxCredit: Option[IfWorkTaxCredit],
  childTaxCredit: Option[IfChildTaxCredit],
  grossTaxYearAmount: Option[Double],
  payments: Option[Seq[IfPayment]]
)

object IfAward extends PatternsAndValidators {

  implicit val awardsFormat: Format[IfAward] = Format(
    (
      (JsPath \ "payProfCalcDate")
        .readNullable[String](using pattern(datePattern, "invalid date")) and
        (JsPath \ "startDate")
          .readNullable[String](using pattern(datePattern, "invalid date")) and
        (JsPath \ "endDate")
          .readNullable[String](using pattern(datePattern, "invalid date")) and
        (JsPath \ "totalEntitlement")
          .readNullable[Double](using paymentAmountValidator) and
        (JsPath \ "workingTaxCredit").readNullable[IfWorkTaxCredit] and
        (JsPath \ "childTaxCredit").readNullable[IfChildTaxCredit] and
        (JsPath \ "grossTaxYearAmount")
          .readNullable[Double](using paymentAmountValidator) and
        (JsPath \ "payments").readNullable[Seq[IfPayment]]
    )(IfAward.apply),
    Json.writes[IfAward]
  )
}
