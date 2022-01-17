import { rem } from 'polished';
import styled from 'styled-components/macro';

import { SearchIcon as SearchIconComponent } from 'components/Icon';

export const Wrapper = styled.div`
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  padding: ${rem(11)} ${rem(12)};
  background-color: ${(props) => props.theme.colors.background.tertiaryNav};
`;

export const Search = styled.div`
  margin: 0 ${rem(12)};
  position: relative;
  display: flex;
  align-items: center;
`;

export const SearchIcon = styled(SearchIconComponent)`
  position: absolute;
  left: ${rem(12)};
  color: ${(props) => props.theme.colors.primary.lilac40};
  font-size: ${rem(13)};
`;

export const SearchInput = styled.input`
  width: 100%;
  border: none;
  background-color: ${(props) => props.theme.colors.background.bg};
  border-radius: 2em;
  padding: ${rem(7)} ${rem(7)} ${rem(7)} ${rem(32)};
  color: ${(props) => props.theme.colors.white[80]};
  min-height: ${rem(30)};

  &:focus {
    outline: 1px solid ${(props) => props.theme.colors.primary.lilac100};

    & + ${SearchIcon} {
      color: ${(props) => props.theme.colors.primary.lilac100};
    }
  }

  &::placeholder {
    color: ${(props) => props.theme.colors.primary.lilac40};
  }

  ${(props) => props.theme.typography.body.medium};
`;
