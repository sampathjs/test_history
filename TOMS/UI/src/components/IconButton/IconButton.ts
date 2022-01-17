import { rem } from 'polished';
import styled from 'styled-components/macro';

import { getColor } from 'styles';

export const IconButton = styled.button`
  display: inline-flex;
  appearance: none;
  padding: 0;
  background-color: transparent;
  border: none;
  font-size: ${rem(22)};
  color: ${getColor('primary.lilac30')};

  &:hover {
    color: ${getColor('white.100')};
  }
`;
