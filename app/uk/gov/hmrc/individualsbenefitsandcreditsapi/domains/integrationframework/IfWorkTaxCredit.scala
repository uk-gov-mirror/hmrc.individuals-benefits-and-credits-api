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
import play.api.libs.json.{Format, JsPath, Json}

case class IfWorkTaxCredit(amount: Option[Double], entitlementYTD: Option[Double], paidYTD: Option[Double])

object IfWorkTaxCredit extends PatternsAndValidators {

  implicit val workTaxCreditFormat: Format[IfWorkTaxCredit] = Format(
    (
      (JsPath \ "amount").readNullable[Double](using paymentAmountValidator) and
        (JsPath \ "entitlementYTD")
          .readNullable[Double](using paymentAmountValidator) and
        (JsPath \ "paidYTD").readNullable[Double](using paymentAmountValidator)
    )(IfWorkTaxCredit.apply),
    Json.writes[IfWorkTaxCredit]
  )
}
