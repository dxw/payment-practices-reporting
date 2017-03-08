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
import config.AppConfig
import play.api.mvc.{Action, Controller}

class ErrorController @Inject()(val appConfig: AppConfig) extends Controller with PageHelper {

  def sessionTimeout = Action { implicit request =>
    Unauthorized(page("Your session timed out")(home, views.html.errors.sessionTimeout())).removingFromSession(SessionAction.sessionIdKey)
  }

  def notFound = Action { implicit request =>
    NotFound(page("Page not found")(home, views.html.errors.error404()))
  }

}