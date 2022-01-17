# Trading Order Management System (TOMS) UI

This project was bootstrapped with [Create React App](https://github.com/facebook/create-react-app).

## Editing Environment variables

An `env.example` file is provied to convigure variables for production delpoyments.

The following variables will likely need to be confugired:

- `REACT_APP_API_URL` - The API URL which the UI points at
- `REACT_APP_BASE_URL` - The base address of the UI
- `REACT_APP_MOCK_API` - Whether the app should use a proxied mock. Should be set to `false` for production.

The example file should be saved as `env.local` or `env.production` for local and production testing respectively.

## Building the code for production

### `npm run setup`

Installs all dependencies and builds the app for production to the `build` folder.\

The entry point of the application is `index.html` within the build folder

## Running locally

In the project directory, you can run:

### `npm start`

Runs the app in the development mode.\
Open [http://localhost:3000](http://localhost:3000) to view it in the browser.

The page will reload if you make edits.\
You will also see any lint errors in the console.
