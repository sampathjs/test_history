import { rem } from 'polished';
import styled from 'styled-components/macro';

import { getColor } from 'styles';

export const Columns = styled.ul`
  display: flex;
  justify-content: center;
  padding: ${rem(16)} 0;
  border-top: 1px solid ${getColor('primary.lilac40')};
  border-bottom: 1px solid ${getColor('primary.lilac20')};
  column-gap: ${rem(4)};
  min-height: ${rem(64)};
`;
