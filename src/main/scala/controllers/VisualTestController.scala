/*
 * Copyright (C) 2017  Department for Business, Energy and Industrial Strategy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package controllers

import javax.inject.Inject

import calculator.Calculator
import config.{PageConfig, ServiceConfig}
import forms.report._
import forms.{DateRange, Validations}
import models.{CompaniesHouseId, DecisionState, ReportId}
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.data.Forms.single
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Call, Controller}
import play.twirl.api.Html
import questionnaire._
import services._
import utils.YesNo.{No, Yes}
import utils.{SystemTimeSource, YesNo}
import views.html.helpers.ReviewPageData

class VisualTestController @Inject()(
                                      summarizer: Summarizer,
                                      val pageConfig: PageConfig,
                                      val serviceConfig: ServiceConfig
                                    )(implicit messages: MessagesApi) extends Controller with PageHelper {

  import Questions._

  val startDate = new LocalDate(2017, 1, 1)
  val endDate = new LocalDate(2017, 12, 31)

  def show = Action { implicit request =>
    val df = CalculatorController.df
    val index = views.html.index()
    val qStart = views.html.questionnaire.start()
    val reasons = Seq("reason.firstyear", "reason.company.notlargeenough", "reason.group.notlargeenough")
    val exempts = views.html.questionnaire.notACompany("reason.notacompany") +: reasons.map(views.html.questionnaire.exempt(_))

    val secondYear: DecisionState = models.DecisionState(Some(Yes), Some(FinancialYear.Second), Thresholds(Some(Yes), Some(Yes), Some(Yes)), Some(Yes), Thresholds(Some(Yes), Some(Yes), Some(Yes)))
    val thirdYear: DecisionState = models.DecisionState(Some(Yes), Some(FinancialYear.ThirdOrLater), Thresholds(Some(Yes), Some(Yes), Some(Yes)), Some(Yes), Thresholds(Some(Yes), Some(Yes), Some(Yes)))

    val requireds =
      Seq(views.html.questionnaire.required(summarizer.summarize(secondYear)),
        views.html.questionnaire.required(summarizer.summarize(thirdYear)))
    val calcs = Seq(
      views.html.calculator.calculator(CalculatorController.emptyForm),
      views.html.calculator.calculator(CalculatorController.emptyForm.bind(Map[String, String]())))


    val calc = Calculator(calculator.FinancialYear(DateRange(startDate, endDate)))
    val answers = Seq(views.html.calculator.answer(isGroup = false, calc, df))
    val reports = Seq(views.html.search.report(healthyReport, df))
    val id = CompaniesHouseId("1234567890")
    val companyName = "ABC Limited"
    val summary = CompanySearchResult(id, companyName, Some("123 Abc Road"))
    val results = PagedResults(Seq(summary, summary, summary), 25, 1, 100)
    val searches = Seq(
      html(h1("Publish a report"), views.html.search.search("cod", Some(results), Map(id -> 3), "", _ => "", _ => "")),
      html(h1("Search for a report"), views.html.search.search("cod", Some(results), Map(id -> 3), "", _ => "", _ => ""))
    )
    val companies = Seq(views.html.search.company(CompanyDetail(id, companyName), PagedResults(Seq(healthyReport, healthyReport, healthyReport), 25, 1, 100), _ => "", df))
    val start = Seq(views.html.report.start(companyName, id))
    val signIn = Seq(views.html.report.preLogin(id, Form(single("account" -> Validations.yesNo))))
    val options = Seq(
      views.html.report.companiesHouseOptions(companyName, id),
      views.html.report.askColleague(companyName, id),
      views.html.report.requestAccessCode(companyName, id)
    )

    val reportValidations = new Validations(new SystemTimeSource, ServiceConfig.empty)
    import reportValidations._

    val dummyReportingPeriodModel = ReportingPeriodFormModel(DateRange(LocalDate.now(), LocalDate.now()), No)
    val dummyReportingPeriodForm = emptyReportingPeriod.fill(dummyReportingPeriodModel)
    val header = h1(s"Publish a report for:<br>$companyName")
    val serviceStartDate = serviceConfig.startDate.getOrElse(ServiceConfig.defaultServiceStartDate)
    val healthyLongFormModel = LongFormModel(paymentCodes, healthyLongForm)

    val reportingPeriods = Seq(
      views.html.report.reportingPeriod(header, dummyReportingPeriodForm, Map(), id, df, serviceStartDate)
    )

    val longForms = Seq(
      views.html.report.longForm(header, emptyLongForm, dummyReportingPeriodForm.data, id, df, serviceStartDate),
      views.html.report.longForm(header, emptyLongForm.fill(healthyLongFormModel), dummyReportingPeriodForm.data, id, df, serviceStartDate),
      views.html.report.longForm(header, emptyLongForm.fillAndValidate(LongFormModel(paymentCodes, unhealthyLongForm)), dummyReportingPeriodForm.data, id, df, serviceStartDate)
    )

    val shortForms = Seq(
      views.html.report.shortForm(header, emptyShortForm, dummyReportingPeriodForm.data, id, df, serviceStartDate)
    )

    val formGroups = ReviewPageData.formGroups(companyName, dummyReportingPeriodModel, healthyLongFormModel)
    val review = Seq(views.html.report.review(emptyReview, emptyLongForm.fill(healthyLongFormModel).data ++ dummyReportingPeriodForm.data, formGroups, Call("", "")))
    val published = Seq(views.html.report.filingSuccess(reportId, "foobar@example.com", pageConfig.surveyMonkeyConfig))
    val errors = Seq(
      views.html.errors.sessionTimeout(),
      views.html.errors.error404(),
      views.html.errors.error500(),
      views.html.errors.forbidden403("")
    )

    val content: Seq[Html] = (
      Seq(index, qStart)
        ++ questionPages
        ++ exempts
        ++ requireds
        ++ calcs
        ++ answers
        ++ reports
        ++ searches
        ++ companies
        ++ start
        ++ signIn
        ++ options
        ++ reportingPeriods
        ++ shortForms
        ++ longForms
        ++ review
        ++ published
        ++ errors
      ).zipWithIndex.flatMap { case (x, i) => Seq(Html(s"<hr id='${i + 1}' />screen ${i + 1}"), x) }
    Ok(page("Visual test of all pages")(content: _*))
  }

  val questionPages = Seq(
    isCompanyOrLLPQuestion,
    financialYearQuestion,
    hasSubsidiariesQuestion,
    companyTurnoverQuestionY2,
    companyBalanceSheetQuestionY2,
    companyEmployeesQuestionY2,
    companyTurnoverQuestionY3,
    companyBalanceSheetQuestionY3,
    companyEmployeesQuestionY3,
    subsidiaryTurnoverQuestionY2,
    subsidiaryBalanceSheetQuestionY2,
    subsidiaryEmployeesQuestionY2,
    subsidiaryTurnoverQuestionY3,
    subsidiaryBalanceSheetQuestionY3,
    subsidiaryEmployeesQuestionY3
  ).map(views.html.questionnaire.question(_, Form(QuestionnaireValidations.decisionStateMapping).data, None))

  val states = Seq(
    StateSummary(None, ThresholdSummary(None, None, None), ThresholdSummary(None, None, None))
  )

  import YesNo._

  val reportId = ReportId(0)

  private val paymentCodes = ConditionalText("Payment Practice Code")

  lazy val healthyReport = Report(
    reportId, "ABC Limited", CompaniesHouseId("1234567890"), LocalDate.now,
    "The big boss", "bigboss@thebigcompany.com", DateRange(startDate, endDate), paymentCodes,
    Some(healthyLongForm))

  val healthyLongForm = ContractDetails(
    PaymentTerms(
      30,
      None,
      "payment terms",
      30,
      Some("Maximum period is very fair"),
      PaymentTermsChanged(ConditionalText("Payment terms have changed"), Some(ConditionalText("We told everyone"))),
      Some("Other comments"),
      "Dispute resolution process is the best"),
    PaymentHistory(30, 10, PercentageSplit(33, 33, 33)),
    No, Yes, No, Yes)


  lazy val unhealthyReport = Report(
    reportId, "ABC Limited", CompaniesHouseId("1234567890"), LocalDate.now,
    "The big boss", "bigboss@thebigcompany.com", DateRange(startDate.plusYears(1), endDate.plusYears(1)), paymentCodes,
    Some(unhealthyLongForm))

  val unhealthyLongForm = ContractDetails(
    PaymentTerms(-1, None, "payment terms", 200, Some("Maximum period is very fair"),
      PaymentTermsChanged(ConditionalText("Payment terms have changed"), Some(ConditionalText("We told everyone"))),
      Some("Other comments"),
      "Dispute resolution process is the best"
    ),
    PaymentHistory(-1, 200, PercentageSplit(20, 33, 33)),
    No, Yes, No, Yes
  )

}
