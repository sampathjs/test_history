import { LinkProps, useMatch, useResolvedPath } from 'react-router-dom';

import * as Styles from './NavItem.styles';

export const NavItem = ({ children, to }: LinkProps) => {
  const resolved = useResolvedPath(to);
  const isActive = useMatch({ path: resolved.pathname, end: true });

  return (
    <Styles.NavItem>
      <Styles.NavLink to={to} $isActive={Boolean(isActive)}>
        {children}
      </Styles.NavLink>
    </Styles.NavItem>
  );
};
