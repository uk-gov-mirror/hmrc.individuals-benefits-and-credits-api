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

package unit.uk.gov.hmrc.individualsbenefitsandcreditsapi.controllers

import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.{any, eq => eqTo, refEq}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments, InsufficientEnrolments}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsbenefitsandcreditsapi.audit.AuditHelper
import uk.gov.hmrc.individualsbenefitsandcreditsapi.controllers.RootController
import uk.gov.hmrc.individualsbenefitsandcreditsapi.domains.{MatchNotFoundException, MatchedCitizen}
import uk.gov.hmrc.individualsbenefitsandcreditsapi.services.{ScopesHelper, ScopesService, TaxCreditsService}
import unit.uk.gov.hmrc.individualsbenefitsandcreditsapi.config.ScopesConfigHelper
import unit.uk.gov.hmrc.individualsbenefitsandcreditsapi.utils.SpecBase

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class RootControllerSpec extends SpecBase with MockitoSugar {

  val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
  val correlationIdHeader: (String, String) = "CorrelationId" -> sampleCorrelationId

  implicit lazy val materializer: Materializer = fakeApplication().materializer

  private val testMatchId =
    UUID.fromString("be2dbba5-f650-47cf-9753-91cdaeb16ebe")

  trait Fixture extends ScopesConfigHelper {

    implicit val ec: ExecutionContext =
      fakeApplication().injector.instanceOf[ExecutionContext]
    lazy val scopeService: ScopesService = new ScopesService(mockScopesConfig)
    lazy val scopesHelper: ScopesHelper = new ScopesHelper(scopeService)

    val taxCreditsService: TaxCreditsService = mock[TaxCreditsService]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val auditHelper: AuditHelper = mock[AuditHelper]

    val testNino: Nino = Nino("AB123456C")

    when(
      mockAuthConnector.authorise(eqTo(Enrolment("test-scope")), refEq(Retrievals.allEnrolments))(using any(), any())
    )
      .thenReturn(Future.successful(Enrolments(Set(Enrolment("test-scope")))))

    val rootController =
      new RootController(
        mockAuthConnector,
        cc,
        scopeService,
        scopesHelper,
        auditHelper,
        taxCreditsService
      )

  }

  "Root" should {
    "return a 404 (not found) when a match id does not match tdata" in new Fixture {

      Mockito.reset(rootController.auditHelper)

      when(taxCreditsService.resolve(eqTo(testMatchId))(using any[HeaderCarrier]))
        .thenReturn(Future.failed(new MatchNotFoundException))

      val eventualResult: Future[Result] =
        rootController.root(testMatchId)(FakeRequest().withHeaders(correlationIdHeader))

      status(eventualResult) shouldBe NOT_FOUND
      contentAsJson(eventualResult) shouldBe Json.obj(
        "code"    -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )

      verify(rootController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(using any())
    }

    "return a 200 (ok) when a match id matches tdata" in new Fixture {

      when(taxCreditsService.resolve(eqTo(testMatchId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(MatchedCitizen(testMatchId, testNino)))

      val eventualResult: Future[Result] =
        rootController.root(testMatchId)(FakeRequest().withHeaders(correlationIdHeader))

      status(eventualResult) shouldBe OK
      contentAsJson(eventualResult) shouldBe Json.obj(
        "_links" -> Json.obj(
          "child-tax-credit" -> Json.obj(
            "href"  -> s"/individuals/benefits-and-credits/child-tax-credit?matchId=$testMatchId{&fromDate,toDate}",
            "title" -> "Get Child Tax Credit details"
          ),
          "working-tax-credit" -> Json.obj(
            "href"  -> s"/individuals/benefits-and-credits/working-tax-credit?matchId=$testMatchId{&fromDate,toDate}",
            "title" -> "Get Working Tax Credit details"
          ),
          "self" -> Json.obj(
            "href" -> s"/individuals/benefits-and-credits/?matchId=$testMatchId"
          )
        )
      )
    }

    "fail with status 401 when the bearer token does not have enrolment test-scope" in new Fixture {

      when(mockAuthConnector.authorise(any(), any())(using any(), any()))
        .thenReturn(Future.failed(InsufficientEnrolments()))

      val result: Future[Result] = rootController.root(testMatchId)(FakeRequest().withHeaders(correlationIdHeader))

      status(result) shouldBe UNAUTHORIZED
      verifyNoInteractions(taxCreditsService)
    }
  }
}
