import './styles/fonts.css';

import { QueryClient, QueryClientProvider } from 'react-query';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { ThemeProvider } from 'styled-components/macro';

import { Layout } from 'components/Layout';
import { Orders } from 'components/Orders';
import { AuthProvider } from 'contexts';

import { GlobalStyles, theme } from './styles';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
    },
  },
});

const App = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter basename={process.env.REACT_APP_BASE_URL}>
        <AuthProvider>
          <ThemeProvider theme={theme}>
            <GlobalStyles />
            <Routes>
              <Route path="/" element={<Layout />}>
                <Route index element={<Orders />} />
                <Route
                  path="admin"
                  element={<div style={{ color: 'white' }}>Admin</div>}
                />
              </Route>
            </Routes>
          </ThemeProvider>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
};

export default App;
