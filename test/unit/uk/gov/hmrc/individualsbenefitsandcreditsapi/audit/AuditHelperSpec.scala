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

package unit.uk.gov.hmrc.individualsbenefitsandcreditsapi.audit

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsbenefitsandcreditsapi.audit.AuditHelper
import uk.gov.hmrc.individualsbenefitsandcreditsapi.audit.models.*
import uk.gov.hmrc.individualsbenefitsandcreditsapi.audit.models.childtaxcredits.{ChildTaxApiResponseEventModel, CtcApplicationModel, CtcAwardModel}
import uk.gov.hmrc.individualsbenefitsandcreditsapi.audit.models.workingtaxcredits.{WorkingTaxApiResponseEventModel, WtcApplicationModel, WtcAwardModel}
import uk.gov.hmrc.individualsbenefitsandcreditsapi.domains.integrationframework.{IfApplication, IfApplications}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import unit.uk.gov.hmrc.individualsbenefitsandcreditsapi.utils.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class AuditHelperSpec extends UnitSpec with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val nino = "CS700100A"
  val correlationId = "test"
  val scopes = "test"
  val matchId = "80a6bb14-d888-436e-a541-4000674c60aa"
  val applicationId = "80a6bb14-d888-436e-a541-4000674c60bb"
  val request: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withHeaders("X-Application-Id" -> applicationId)
  val response: JsValue = Json.toJson("some" -> "json")
  val ifUrl =
    s"host/individuals/benefits-and-credits/child-tax-credit/nino/$nino?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)"
  val endpoint = "/test"

  val auditConnector: AuditConnector = mock[AuditConnector]

  val workingTaxCreditResponse = Seq(WtcApplicationModel(None, Seq(WtcAwardModel(None, None, None, None, None))))
  val childTaxCreditResponse = Seq(CtcApplicationModel(None, Seq(CtcAwardModel(None, None, None, None))))
  val ifResponse: IfApplications = IfApplications(Seq(IfApplication(None, None, None, None, None)))

  val auditHelper = new AuditHelper(auditConnector)

  "Auth helper" should {

    "auditAuthScopes" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ScopesAuditEventModel])

      auditHelper.auditAuthScopes(matchId, scopes, request)

      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("AuthScopesAuditEvent"), captor.capture())(using any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[ScopesAuditEventModel]
      capturedEvent.apiVersion shouldEqual "1.0"
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.scopes shouldBe scopes
      capturedEvent
        .asInstanceOf[ScopesAuditEventModel]
        .applicationId shouldBe applicationId

    }

    "childTaxApiResponseEvent" in {

      Mockito.reset(auditConnector)

      val captor =
        ArgumentCaptor.forClass(classOf[ChildTaxApiResponseEventModel])

      auditHelper
        .childTaxCreditAuditApiResponse(correlationId, matchId, scopes, request, endpoint, childTaxCreditResponse)

      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("ApiResponseEvent"), captor.capture())(using any(), any(), any())

      val capturedEvent =
        captor.getValue.asInstanceOf[ChildTaxApiResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.scopes shouldBe scopes
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.returnLinks shouldBe endpoint
      capturedEvent.childTaxCredit shouldBe childTaxCreditResponse
      capturedEvent.apiVersion shouldBe "1.0"

    }

    "workingTaxApiResponseEvent" in {

      Mockito.reset(auditConnector)

      val captor =
        ArgumentCaptor.forClass(classOf[ChildTaxApiResponseEventModel])

      auditHelper
        .workingTaxCreditAuditApiResponse(correlationId, matchId, scopes, request, endpoint, workingTaxCreditResponse)

      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("ApiResponseEvent"), captor.capture())(using any(), any(), any())

      val capturedEvent =
        captor.getValue.asInstanceOf[WorkingTaxApiResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.scopes shouldBe scopes
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.returnLinks shouldBe endpoint
      capturedEvent.workingTaxCredit shouldBe workingTaxCreditResponse
      capturedEvent.apiVersion shouldBe "1.0"

    }

    "auditApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor =
        ArgumentCaptor.forClass(classOf[ApiFailureResponseEventModel])

      auditHelper.auditApiFailure(Some(correlationId), matchId, request, "/test", msg)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("ApiFailureEvent"), captor.capture())(using
        any(),
        any(),
        any()
      )

      val capturedEvent =
        captor.getValue.asInstanceOf[ApiFailureResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.requestUrl shouldEqual endpoint
      capturedEvent.response shouldEqual msg

    }

    "auditIfApiResponse" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[IfApiResponseEventModel])

      auditHelper.auditIfApiResponse(correlationId, matchId, request, ifUrl, ifResponse)

      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("IntegrationFrameworkApiResponseEvent"), captor.capture())(using any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[IfApiResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual correlationId
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.requestUrl shouldBe ifUrl
      capturedEvent.integrationFrameworkApplications shouldBe ifResponse

    }

    "auditIfApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor =
        ArgumentCaptor.forClass(classOf[ApiFailureResponseEventModel])

      auditHelper.auditIfApiFailure(correlationId, matchId, request, ifUrl, msg)

      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("IntegrationFrameworkApiFailureEvent"), captor.capture())(using any(), any(), any())

      val capturedEvent =
        captor.getValue.asInstanceOf[ApiFailureResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.requestUrl shouldEqual ifUrl
      capturedEvent.response shouldEqual msg

    }
  }
}
