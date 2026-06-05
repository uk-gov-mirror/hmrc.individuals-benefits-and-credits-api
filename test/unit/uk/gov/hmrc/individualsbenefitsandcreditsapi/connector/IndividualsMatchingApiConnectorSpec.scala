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

package unit.uk.gov.hmrc.individualsbenefitsandcreditsapi.connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.individualsbenefitsandcreditsapi.connectors.IndividualsMatchingApiConnector
import uk.gov.hmrc.individualsbenefitsandcreditsapi.domains.{MatchNotFoundException, MatchedCitizen}
import unit.uk.gov.hmrc.individualsbenefitsandcreditsapi.utils.SpecBase

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class IndividualsMatchingApiConnectorSpec extends SpecBase with BeforeAndAfterEach {

  val stubPort: Int = sys.env.getOrElse("WIREMOCK", "11121").toInt
  val stubHost = "localhost"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  trait Fixture {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val rd: RequestHeader = FakeRequest()

    val individualsMatchingApiConnector: IndividualsMatchingApiConnector =
      new IndividualsMatchingApiConnector(servicesConfig, fakeApplication().injector.instanceOf[HttpClientV2]) {
        override val serviceUrl = "http://127.0.0.1:11121"
      }
  }

  def externalServices: Seq[String] = Seq("Stub")

  override def beforeEach(): Unit = {
    wireMockServer.start()
    configureFor(stubHost, stubPort)
  }

  "Matching API connector resolve function should" should {

    val matchId = UUID.randomUUID()

    def stubWithResponseStatus(responseStatus: Int, body: String = ""): Unit =
      stubFor(
        get(urlPathMatching(s"/match-record/$matchId"))
          .willReturn(aResponse().withStatus(responseStatus).withBody(body))
      )

    "fail when upstream service fails" in new Fixture {
      stubWithResponseStatus(INTERNAL_SERVER_ERROR)
      a[UpstreamErrorResponse] should be thrownBy {
        await(individualsMatchingApiConnector.resolve(matchId))
      }
    }

    "rethrow a not found exception as a match not found exception" in new Fixture {
      stubWithResponseStatus(NOT_FOUND)
      a[MatchNotFoundException] should be thrownBy {
        await(individualsMatchingApiConnector.resolve(matchId))
      }
    }

    "return a nino match when upstream service call succeeds" in new Fixture {
      stubWithResponseStatus(
        OK,
        s"""
          {
            "matchId":"${matchId.toString}",
            "nino":"AB123456C"
          }
        """
      )
      await(individualsMatchingApiConnector.resolve(matchId)) shouldBe MatchedCitizen(matchId, Nino("AB123456C"))
    }

    "setHeaders return header when CorrelationId is present" in new Fixture {

      val request = FakeRequest().withHeaders("CorrelationId" -> "188e9400-b636-4a3b-80ba-230a8c72b92a")

      val result: Seq[(String, String)] = individualsMatchingApiConnector.setHeaders(request)

      result mustBe Seq("CorrelationId" -> "188e9400-b636-4a3b-80ba-230a8c72b92a")
    }

    "setHeaders return empty Seq when CorrelationId is missing" in new Fixture {
      val request = FakeRequest()

      val result: Seq[(String, String)] = individualsMatchingApiConnector.setHeaders(request)

      result mustBe Seq.empty
    }
  }

  override def afterEach(): Unit =
    wireMockServer.stop()

}
