import { useLayoutContext } from 'contexts';

import { OrdersIcon, UserIcon } from '../../Icon';
import * as Styles from './Nav.styles';
import { NavItem } from './NavItem';

export const Nav = () => {
  const { filter } = useLayoutContext();

  return (
    <Styles.Nav hideDivider={!filter.isPanelExpanded}>
      <Styles.NavItems>
        <NavItem to="/">
          <OrdersIcon />
        </NavItem>
        <NavItem to="/admin">
          <UserIcon />
        </NavItem>
      </Styles.NavItems>
    </Styles.Nav>
  );
};
