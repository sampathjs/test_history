import { rem } from 'polished';
import { Link } from 'react-router-dom';
import styled from 'styled-components/macro';

export const NavItem = styled.li`
  line-height: 1;
`;

export const NavLink = styled(Link)<{ $isActive?: boolean }>`
  display: block;
  padding: ${rem(12)} ${rem(16)};
  font-size: ${rem(24)};
  color: ${(props) =>
    props.$isActive
      ? props.theme.colors.primary.lilac60
      : props.theme.colors.primary.lilac15};

  &:hover {
    color: ${(props) => props.theme.colors.primary.blue100};
  }
`;
