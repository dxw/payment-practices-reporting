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

import actions.SessionAction
import config.{PageConfig, ServiceConfig}
import models.CompaniesHouseId
import play.api.mvc.{Action, Controller}
import services.{CompanyAuthService, CompanySearchService, SessionService}

import scala.concurrent.ExecutionContext

class CoHoOAuthMockController @Inject()(
                                         companySearch: CompanySearchService,
                                         companyAuth: CompanyAuthService,
                                         sessionService: SessionService,
                                         SessionAction: SessionAction,
                                         val pageConfig: PageConfig,
                                         val serviceConfig: ServiceConfig)
                                       (implicit ec: ExecutionContext) extends Controller with PageHelper {

  def login(companiesHouseId: CompaniesHouseId) = Action {
    Ok(views.html.oauthMock.mockCohoLogin(companiesHouseId))
  }

  def postLogin(companiesHouseId: CompaniesHouseId) = Action { request => Redirect(controllers.routes.CoHoOAuthMockController.authCode(companiesHouseId)) }

  def authCode(companiesHouseId: CompaniesHouseId) = Action.async { implicit request =>

    companySearch.find(companiesHouseId).map {
      case Some(co) =>
        Ok(views.html.oauthMock.mockCohoAuthCode(companiesHouseId, co.companyName))
      case None => BadRequest(s"Unknown company id ${companiesHouseId.id}")
    }
  }
}
