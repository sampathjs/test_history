# TotalLiquidityMonitoring

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 11.2.7. It also uses **Storybook** for the component design & testing.

For testing, it also uses [json-server](https://github.com/typicode/json-server) as mock data server.

All the relevant tasks are configured via the standard npm approach:

* To start Storybook server: `npm run storybook` - all the components can be checked in isolation at localhost:6006

* To start the whole UI with the mock server: `npm run start-with-mock` - it will start the server at localhost:3000

* To build for production: `npm run build --prod --output-hashing=none`

## Component Overview

The workflow of the UI is straightforward: it will show the user login (component **"user-login"**) firstly if the user is not logged in, 
then it will show both common info (component **"common-info"**) and balance form (component **"balance-form"**) for user input.

Component **busy-indicator** will be displayed whenever the website is waiting for the server's responses.

Component **error-indicator** will be displayed as an overlay dialog whenever errors occur.

The only service in this project is **data-service** - it takes care of all the interactions with REST API.

## Storybook

This project follows [guide 1](https://storybook.js.org/docs/angular/get-started/introduction) and [guide 2](https://storybook.js.org/tutorials/intro-to-storybook/angular/en/get-started/).

All the inputs of the components can be adjusted in Storybook, so different user scenarios can be verified. 
All the output of the components are added as **actions** in Storybook, so the event triggering can be observed.

Furthermore, by design, all the REST API calling only happen in the top-level component (i.e. **app.component**) so other components can be designed and tested without the need of mock server.

## Testing with Mock Server

**json-server** is used as mock server in this project. It is easy to use: all the data are in the file "mock/data-service.json" - this file can be updated for different test scenarios.

The script **"start-with-mock"** will start a server at localhost:3000 with the REST API specified in "mock/data-service.json", and the static content of this website.

Obviously, the static content needs to be built first. Using `npm build` (with any parameters needed) to generate the static content under "dist/total-liquidity-monitoring".
