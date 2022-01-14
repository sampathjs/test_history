const { REACT_APP_API_URL: API_URL, REACT_APP_MOCK_API: MOCK_API } =
  process.env;

export const REACT_APP_API_URL = API_URL ?? 'http://localhost:8080';
export const REACT_APP_MOCK_API = MOCK_API === 'true';
