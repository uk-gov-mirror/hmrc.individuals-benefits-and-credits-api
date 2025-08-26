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

package uk.gov.hmrc.individualsbenefitsandcreditsapi.connectors

import play.api.Logger
import play.api.mvc.RequestHeader
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, InternalServerException, JsValidationException, NotFoundException, StringContextOps, TooManyRequestException, UpstreamErrorResponse}
import uk.gov.hmrc.individualsbenefitsandcreditsapi.audit.AuditHelper
import uk.gov.hmrc.individualsbenefitsandcreditsapi.domains.integrationframework.{IfApplication, IfApplications}
import uk.gov.hmrc.individualsbenefitsandcreditsapi.play.RequestHeaderUtils
import uk.gov.hmrc.individualsbenefitsandcreditsapi.utils.Interval
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IfConnector @Inject() (servicesConfig: ServicesConfig, http: HttpClientV2, val auditHelper: AuditHelper) {

  private val logger = Logger(getClass)

  private val integrationFrameworkBearerToken =
    servicesConfig.getString("microservice.services.integration-framework.authorization-token")
  private val integrationFrameworkEnvironment =
    servicesConfig.getString("microservice.services.integration-framework.environment")

  val serviceUrl: String = servicesConfig.baseUrl("integration-framework")

  def fetchTaxCredits(nino: Nino, interval: Interval, filter: Option[String], matchId: String)(implicit
    hc: HeaderCarrier,
    request: RequestHeader,
    ec: ExecutionContext
  ): Future[Seq[IfApplication]] = {

    val startDate = interval.from.toLocalDate
    val endDate = interval.to.toLocalDate

    val url = s"$serviceUrl/individuals/tax-credits/nino/$nino?" +
      s"startDate=$startDate&endDate=$endDate${filter.map(f => s"&fields=$f").getOrElse("")}"

    call(url, matchId)

  }

  private def extractCorrelationId(requestHeader: RequestHeader) =
    RequestHeaderUtils.validateCorrelationId(requestHeader).toString

  def setHeaders(requestHeader: RequestHeader) = Seq(
    HeaderNames.authorisation -> s"Bearer $integrationFrameworkBearerToken",
    "Environment"             -> integrationFrameworkEnvironment,
    "CorrelationId"           -> extractCorrelationId(requestHeader)
  )

  private def call(url: String, matchId: String)(implicit
    hc: HeaderCarrier,
    request: RequestHeader,
    ec: ExecutionContext
  ) =
    recover[IfApplication](
      http.get(url"$url").transform(_.addHttpHeaders(setHeaders(request)*)).execute[IfApplications] map { response =>
        auditHelper.auditIfApiResponse(extractCorrelationId(request), matchId, request, url, response)
        response.applications
      },
      extractCorrelationId(request),
      matchId,
      request,
      url
    )

  private def recover[A](
    x: Future[Seq[A]],
    correlationId: String,
    matchId: String,
    request: RequestHeader,
    requestUrl: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[A]] = x.recoverWith {
    case validationError: JsValidationException =>
      logger.warn("Integration Framework JsValidationException encountered")
      auditHelper.auditIfApiFailure(
        correlationId,
        matchId,
        request,
        requestUrl,
        s"Error parsing IF response: ${validationError.errors}"
      )
      Future.failed(new InternalServerException("Something went wrong."))
    case UpstreamErrorResponse(msg, 404, _, _) =>
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, msg)

      msg.contains("NO_DATA_FOUND") match {
        case true => Future.successful(Seq.empty)
        case _ =>
          logger.warn("Integration Framework NotFoundException encountered")
          Future.failed(new NotFoundException(msg))
      }
    case UpstreamErrorResponse.Upstream5xxResponse(e) =>
      logger.warn(s"Integration Framework Upstream5xxResponse encountered: ${e.statusCode}")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, s"Internal Server error: ${e.message}")
      Future.failed(new InternalServerException("Something went wrong."))
    case UpstreamErrorResponse(msg, 429, _, _) =>
      logger.warn(s"Integration Framework Rate limited: $msg")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, s"IF Rate limited: $msg")
      Future.failed(new TooManyRequestException(msg))
    case UpstreamErrorResponse.Upstream4xxResponse(e) =>
      logger.warn(s"Integration Framework Upstream4xxResponse encountered: ${e.statusCode}")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, e.message)
      Future.failed(new InternalServerException("Something went wrong."))
    case e: Exception =>
      logger.error(s"Integration Framework Exception encountered", e)
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, e.getMessage)
      Future.failed(new InternalServerException("Something went wrong."))
  }
}
