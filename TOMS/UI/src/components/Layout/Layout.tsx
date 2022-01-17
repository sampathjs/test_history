import { Outlet } from 'react-router-dom';

import { LayoutProvider } from 'contexts';

import { AppHeader } from './AppHeader';
import * as Styles from './Layout.styles';
import { Nav } from './Nav';

export const Layout = () => (
  <LayoutProvider>
    <Styles.Layout>
      <AppHeader />
      <Nav />
      <Styles.Content>
        <Outlet />
      </Styles.Content>
    </Styles.Layout>
  </LayoutProvider>
);
