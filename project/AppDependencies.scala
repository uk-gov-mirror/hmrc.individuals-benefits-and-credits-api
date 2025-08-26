import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {
  val hmrc = "uk.gov.hmrc"
  val hmrcMongo = s"$hmrc.mongo"
  val playVersion = "play-30"
  val hmrcBootstrapVersion = "10.1.0"
  val hmrcMongoVersion = "2.7.0"

  val compile: Seq[ModuleID] = Seq(
    hmrc                %% s"bootstrap-backend-$playVersion"      % hmrcBootstrapVersion,
    hmrc                %% s"domain-$playVersion"                 % "13.0.0",
    hmrc                %% s"play-hal-$playVersion"               % "4.1.0",
    hmrc                %% s"crypto-json-$playVersion"            % "8.3.0",
    hmrcMongo           %% s"hmrc-mongo-$playVersion"             % hmrcMongoVersion
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    "org.scalatestplus"      %% "mockito-3-4"                     % "3.2.10.0"            % scope,
    "org.scalatestplus"      %% "scalacheck-1-17"                 % "3.2.18.0"            % scope,
    "com.codacy"             %% "scalaj-http"                     % "2.5.0"               % scope,
    hmrc                     %% s"bootstrap-test-$playVersion"    % hmrcBootstrapVersion  % scope
  )
}
