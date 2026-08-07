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

package uk.gov.hmrc.individualsbenefitsandcreditsapi.controllers

import play.api.{Environment, Mode}
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsbenefitsandcreditsapi.audit.AuditHelper
import uk.gov.hmrc.individualsbenefitsandcreditsapi.config.AppConfig

import scala.concurrent.{ExecutionContext, Future}

trait PrivilegedAuthentication extends AuthorisedFunctions {

  def authPredicate(scopes: Iterable[String]): Predicate =
    scopes.map(Enrolment(_): Predicate).reduce(_ or _)

  def authenticate(endpointScopes: Iterable[String], matchId: String)(f: Iterable[String] => Future[Result])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    request: RequestHeader,
    auditHelper: AuditHelper,
    appConfig: AppConfig,
    environment: Environment
  ): Future[Result] =
    if (endpointScopes.isEmpty) throw new Exception("No scopes defined")
    if (appConfig.localEnv && environment.mode == Mode.Dev) {
      f(endpointScopes.toList)
    } else {
      authorised(authPredicate(endpointScopes))
        .retrieve(Retrievals.allEnrolments) { case scopes =>
          auditHelper.auditAuthScopes(matchId, scopes.enrolments.map(e => e.key).mkString(","), request)

          f(scopes.enrolments.map(e => e.key))
        }
    }
}
