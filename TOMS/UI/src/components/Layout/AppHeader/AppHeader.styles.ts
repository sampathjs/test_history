import { rem } from 'polished';
import { Link } from 'react-router-dom';
import styled from 'styled-components/macro';

export const Header = styled.div`
  grid-area: header;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: ${(props) => props.theme.colors.background.tertiaryNav};
  padding: ${rem(10)} ${rem(16)};
  border-bottom: 1px solid
    ${(props) => props.theme.colors.background.secondaryNav};

  /* App version */
  span {
    color: ${(props) => props.theme.colors.primary.lilac60};
    font-size: ${rem(11)};
    opacity: 0.3;
  }
`;

export const HomeLink = styled(Link)`
  display: flex;
  font-size: ${rem(24)};
  color: ${(props) => props.theme.colors.primary.lilac60};
`;
