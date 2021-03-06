# payment-practices-reporting
[![CircleCI](https://circleci.com/gh/UKGovernmentBEIS/payment-practices-reporting.svg?style=svg)](https://circleci.com/gh/UKGovernmentBEIS/payment-practices-reporting)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/6640412ef5a54d149d5a7ed87e583521)](https://www.codacy.com/app/UKGovernmentBEIS/payment-practices-reporting?utm_source=github.com&utm_medium=referral&utm_content=UKGovernmentBEIS/payment-practices-reporting&utm_campaign=badger)


## Configuration

The application is designed to start up successfully even if no configuration has been provided. In this case
the configuration system will wire in mock versions of the three external services, namely the Companies House
Search API, the Companies House OAuth 2 api and the GOV Notify service.

### Companies House Search API
You'll need to obtain an API key for making calls to the Companies House Search API. Go to the
Companies House [developer hub](https://developer.companieshouse.gov.uk/api/docs/),
register an account and create a new application. One of the pieces of information
provided is an API key. You can provide this key to the application by setting the
`COMPANIES_HOUSE_API_KEY` environment variable prior to starting `sbt`.

Similarly, for production, inject the api key value into the environment with that
env variable.

### Companies House OAuth2 API

The application uses OAuth2 integration with Companies House to authorise the user for filing. The user
must be able to log in to Companies House and provide an authorisation code that grants them permissions
to file on behalf of the company. To configure the oAuth2 integration set the following environment vars:

* `OAUTH2_CLIENT_ID` and `OAUTH2_CLIENT_SECRET` are the credentials associated with the application registered
with Companies House
* `OAUTH2_CALLBACK_URL` is the absolute url of the endpoint that the oAuth2 flow will redirect back to
after the user has logged in.

_NOTE:_ the Companies House oAuth2 integration is currently still in closed alpha and you must contact
their developer support team directly if you want to get an application registered with them.

### GDS Notify API
PPR uses the GDS Notify service to send confirmation emails when reports are filed. To
do this it needs an API key. You can generate one by registering an account at
https://www.notifications.service.gov.uk.

Use the environment variable `GOVNOTIFY_API_KEY` to pass the key into the application and `GOVNOTIFY_TEMPLATE_ID` to
specify the email template to use.

### Database
In production the database configuration can be injected using the environment variables
`JDBC_DATABASE_URL`, `JDBC_DATABASE_USER` and `JDBC_DATABASE_PASSWORD`. On Heroku, using
the PostgreSQL add-on, these variables are set for you.
 
In development the database details are defaulted to use a local PostgreSQL server
running on the default port (5432) with a database name of `ppr`, a user `ppruser` and
a password of `password`.

### Google Analytics

Set an environment variable `GA_CODE` to the tracking code for your google analytics
account in order to enable tracking.
 
## Design notes

### Confirmation Emails

When the user files their report the system sends a confirmation email to the email
address associated with the Companies House account that was used for filing. PPR
relies on the GDS Notify service to send the emails. Rather than trying to send the
email directly from the form handler for the report, PPR manages a queue of confirmations
to send.

The queue is implemented as a database table, `confirmation_pending`. A row is created
in that table as part of the same transaction that creates the `filing` record.

There is an actor, `ConfirmationActor`, that manages sending of the email and updating
 the confirmation status based on the response from the `Notify` service. If the notification
 is successfully handed off to `Notify` then the `confirmation_pending` row is deleted and
 a row created in `confirmation_sent`. If the call to `Notify` fails then the system attempts
 to decide if it was a transient failure (say because the `Notify` service is down) or a
 permanent failure (e.g. the email address is malformed). If transient then the failure 
 details are just recorded in the `confirmation_pending` row. If permanent then the 
 `confirmation_pending` row is deleted and a row is created in `confirmation_failed` with
 the details of the failure.
 
 When the actor is created it schedules a recurring event that sends a `'poll` message to
 the actor every 10 seconds. The handler for that event finds a single pending confirmation
 and attempts to deliver it. In the process, the selected row is locked by writing a timestamp
  to the `locked_at` column. Part of the query to find a pending confirmation filters out
  rows with a lock time less than 30 seconds ago. This ensures that multiple instances of the
  application do not attempt to send the same confirmation, and also that the system won't 
  attempt to deliver a transiently-failed confirmation for at least 30 seconds.
  
  If a pending confirmation was found and processed then the last action of the message handler
  is to send another `'poll` message to the actor so that any further pending confirmations will
  be processed immediately.
  
  If no unlocked pending confirmation is found then the actor will wait for the scheduled 
  event to send it the next `'poll` message.
  
# Acceptance tests

See https://github.com/UKGovernmentBEIS/ppr-acceptance-tests for the acceptance test suite.