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

package uk.gov.hmrc.individualsbenefitsandcreditsapi.services

import uk.gov.hmrc.individualsbenefitsandcreditsapi.utils.Interval
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsbenefitsandcreditsapi.audit.models.childtaxcredits.CtcApplicationModel
import uk.gov.hmrc.individualsbenefitsandcreditsapi.audit.models.workingtaxcredits.WtcApplicationModel
import uk.gov.hmrc.individualsbenefitsandcreditsapi.connectors.{IfConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsbenefitsandcreditsapi.domains.MatchedCitizen
import uk.gov.hmrc.individualsbenefitsandcreditsapi.domains.childtaxcredits.CtcApplication
import uk.gov.hmrc.individualsbenefitsandcreditsapi.domains.workingtaxcredits.WtcApplication
import uk.gov.hmrc.individualsbenefitsandcreditsapi.services.cache.{CacheId, CacheService}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxCreditsService @Inject() (
  cacheService: CacheService,
  ifConnector: IfConnector,
  scopesService: ScopesService,
  scopesHelper: ScopesHelper,
  individualsMatchingApiConnector: IndividualsMatchingApiConnector
) {
  def getWorkingTaxCredits(matchId: UUID, interval: Interval, scopes: Iterable[String])(implicit
    hc: HeaderCarrier,
    request: RequestHeader,
    ec: ExecutionContext
  ): Future[Seq[WtcApplicationModel]] = {

    val endpoint: String = "working-tax-credit"

    val cacheid = CacheId(matchId, interval, scopesService.getValidFieldsForCacheKey(scopes.toList, List(endpoint)))

    cacheService
      .get(
        cacheid,
        resolve(matchId)
          .flatMap { ninoMatch =>
            val fieldsQuery =
              scopesHelper.getQueryStringFor(scopes.toList, Seq(endpoint).toList)
            ifConnector
              .fetchTaxCredits(ninoMatch.nino, interval, Option(fieldsQuery).filter(_.nonEmpty), matchId.toString)
          }
      )
      .map(applications => applications.map(WtcApplication.create))
  }

  def resolve(matchId: UUID)(implicit hc: HeaderCarrier, request: RequestHeader): Future[MatchedCitizen] =
    individualsMatchingApiConnector.resolve(matchId)

  def getChildTaxCredits(matchId: UUID, interval: Interval, scopes: Iterable[String])(implicit
    hc: HeaderCarrier,
    request: RequestHeader,
    ec: ExecutionContext
  ): Future[Seq[CtcApplicationModel]] = {

    val endpoint: String = "child-tax-credit"

    val cacheid = CacheId(matchId, interval, scopesService.getValidFieldsForCacheKey(scopes.toList, List(endpoint)))

    cacheService
      .get(
        cacheid,
        resolve(matchId)
          .flatMap { ninoMatch =>
            val fieldsQuery =
              scopesHelper.getQueryStringFor(scopes.toList, List(endpoint))
            ifConnector
              .fetchTaxCredits(ninoMatch.nino, interval, Option(fieldsQuery).filter(_.nonEmpty), matchId.toString)
          }
      )
      .map(applications => applications.map(CtcApplication.create))
  }
}
