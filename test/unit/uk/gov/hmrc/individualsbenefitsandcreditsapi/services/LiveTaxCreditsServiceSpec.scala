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

package unit.uk.gov.hmrc.individualsbenefitsandcreditsapi.services

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Format
import play.api.test.FakeRequest
import testUtils.TestHelpers
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsbenefitsandcreditsapi.audit.models.childtaxcredits.CtcApplicationModel
import uk.gov.hmrc.individualsbenefitsandcreditsapi.audit.models.workingtaxcredits.WtcApplicationModel
import uk.gov.hmrc.individualsbenefitsandcreditsapi.connectors.{IfConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsbenefitsandcreditsapi.domains.MatchedCitizen
import uk.gov.hmrc.individualsbenefitsandcreditsapi.services.cache.{CacheIdBase, CacheService}
import uk.gov.hmrc.individualsbenefitsandcreditsapi.services.{ScopesHelper, ScopesService, TaxCreditsService}
import uk.gov.hmrc.individualsbenefitsandcreditsapi.utils.Interval
import unit.uk.gov.hmrc.individualsbenefitsandcreditsapi.utils.UnitSpec

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class LiveTaxCreditsServiceSpec extends UnitSpec with MockitoSugar with TestHelpers with ScopesConfig {

  trait Setup {

    val cacheService: CacheService = new CacheService(null, null)(using null) {
      override def get[T: Format](cacheId: CacheIdBase, functionToCache: => Future[T]): Future[T] =
        functionToCache
    }

    val ifConnector: IfConnector = mock[IfConnector]
    val scopeService: ScopesService = mock[ScopesService]
    val scopesHelper: ScopesHelper = mock[ScopesHelper]
    val matchingConnector: IndividualsMatchingApiConnector =
      mock[IndividualsMatchingApiConnector]

    val taxCreditsService =
      new TaxCreditsService(
        cacheService,
        ifConnector,
        scopeService,
        scopesHelper,
        matchingConnector
      )

    val nino: Nino = Nino("AB123456C")
    private val fromDate = LocalDate.parse("2017-03-02").atStartOfDay()
    private val toDate = LocalDate.parse("2017-05-31").atStartOfDay()
    val testInterval = Interval(fromDate, toDate)
    val testMatchId: UUID =
      UUID.fromString("be2dbba5-f650-47cf-9753-91cdaeb16ebe")

    implicit val ec: ExecutionContextExecutor = ExecutionContext.global
    implicit val hc: HeaderCarrier = HeaderCarrier()

    when(scopeService.getValidFieldsForCacheKey(any(), any()))
      .thenReturn("test")
    when(scopesHelper.getQueryStringFor(any(), any())).thenReturn("(ABC)")
    when(matchingConnector.resolve(eqTo(testMatchId))(using any()))
      .thenReturn(Future.successful(MatchedCitizen(testMatchId, nino)))

  }

  "Live Tax Credits Service" should {

    "return empty list of working tax credits when no records exists for the given matchId" in new Setup {
      when(ifConnector.fetchTaxCredits(any(), any(), any(), any())(using any(), any(), any()))
        .thenReturn(Future.successful(createEmptyIfApplications.applications))
      val response: Seq[WtcApplicationModel] = await(
        taxCreditsService
          .getWorkingTaxCredits(testMatchId, testInterval, Seq("testScope"))(using hc, FakeRequest(), ec)
      )
      response.isEmpty shouldBe true
    }

    "return list of working tax credits when records exists for the given matchId" in new Setup {
      when(
        ifConnector
          .fetchTaxCredits(eqTo(nino), eqTo(testInterval), any(), eqTo(testMatchId.toString))(using any(), any(), any())
      )
        .thenReturn(Future.successful(createValidIfApplicationsMultiple.applications))
      val response: Seq[WtcApplicationModel] = await(
        taxCreditsService
          .getWorkingTaxCredits(testMatchId, testInterval, Seq("testScope"))(using hc, FakeRequest(), ec)
      )
      response.isEmpty shouldBe false
    }

    "return empty list of child tax credits when no records exists for the given matchId" in new Setup {
      when(ifConnector.fetchTaxCredits(any(), any(), any(), any())(using any(), any(), any()))
        .thenReturn(Future.successful(createEmptyIfApplications.applications))
      val response: Seq[CtcApplicationModel] = await(
        taxCreditsService
          .getChildTaxCredits(testMatchId, testInterval, Seq("testScope"))(using hc, FakeRequest(), ec)
      )
      response.isEmpty shouldBe true
    }

    "return list of child tax credits when records exists for the given matchId" in new Setup {
      when(
        ifConnector
          .fetchTaxCredits(eqTo(nino), eqTo(testInterval), any(), eqTo(testMatchId.toString))(using any(), any(), any())
      )
        .thenReturn(Future.successful(createValidIfApplicationsMultiple.applications))
      val response: Seq[CtcApplicationModel] = await(
        taxCreditsService
          .getChildTaxCredits(testMatchId, testInterval, Seq("testScope"))(using hc, FakeRequest(), ec)
      )
      response.isEmpty shouldBe false
    }
  }
}
