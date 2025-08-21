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

package testUtils

import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait AuthHelper {

  protected val enrolments = Enrolments(
    Set(
      Enrolment("read:hello-scopes-1", Seq(EnrolmentIdentifier("FOO", "BAR")), "Activated"),
      Enrolment("read:hello-scopes-2", Seq(EnrolmentIdentifier("FOO2", "BAR2")), "Activated"),
      Enrolment("read:hello-scopes-3", Seq(EnrolmentIdentifier("FOO3", "BAR3")), "Activated")
    )
  )

  protected def fakeAuthConnector(stubbedRetrievalResult: Future[?]): AuthConnector = new AuthConnector {
    def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
    ): Future[A] =
      stubbedRetrievalResult.map(_.asInstanceOf[A])
  }

}
