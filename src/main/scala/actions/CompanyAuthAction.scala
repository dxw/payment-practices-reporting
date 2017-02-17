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

package actions

import javax.inject.Inject

import models.CompaniesHouseId
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class CompanyAuthRequest[A](companiesHouseId: CompaniesHouseId, companyName: String, request: Request[A]) extends WrappedRequest[A](request)

object CompanyAuthAction {
  val companyIdHeader = "company_id"
  val companyNameHeader = "company_name"
}

class CompanyAuthAction @Inject()(implicit ec: ExecutionContext) {

  import CompanyAuthAction._

  implicit class SessionSyntax(result: Result)(implicit request: Request[_]) {
    def clearingSession = result.removingFromSession(companyIdHeader, companyNameHeader)
  }

  def apply(expectedId: CompaniesHouseId): ActionBuilder[CompanyAuthRequest] =
    new ActionBuilder[CompanyAuthRequest] {
      override def invokeBlock[A](request: Request[A], next: (CompanyAuthRequest[A]) => Future[Result]): Future[Result] = {
        implicit val r = request
        val companyRequest = for {
          coHoId <- request.session.get(companyIdHeader).map(CompaniesHouseId)
          name <- request.session.get(companyNameHeader)
        } yield CompanyAuthRequest(coHoId, name, request)

        companyRequest match {
          case Some(cr) if cr.companiesHouseId == expectedId => next(cr)
          case Some(cr) => Future.successful(Unauthorized("company id on session does not match id in url").clearingSession)
          case None => Future.successful(Unauthorized("no company details found on request").clearingSession)
        }
      }
    }
}
