import { rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { getColor } from 'styles';

export const Nav = styled.nav<{ hideDivider?: boolean }>`
  grid-area: nav;
  width: ${rem(56)};
  padding: ${rem(6)} 0;
  background-color: ${getColor('background.primaryNav')};
  border-right-width: ${rem(1)};
  border-right-style: solid;
  border-right-color: ${getColor('background.dividerLine')};
  transition: border-right 0s ease 0.1s;

  ${(props) =>
    props?.hideDivider &&
    css`
      border-right-color: ${getColor('background.primaryNav')};
      transition: border-right 0.1s ease 0;
    `}
`;

export const NavItems = styled.ul``;
