import { rem } from 'polished';
import styled from 'styled-components/macro';

import { getColor, getFont } from 'styles';

export const Wrapper = styled.div`
  background-color: ${getColor('background.tertiaryNav')};
  padding: ${rem(16)} ${rem(28)};
`;

export const Header = styled.div`
  padding: ${rem(8)} 0 ${rem(16)};
`;

export const Title = styled.h2`
  color: ${getColor('white.90')};
  margin: 0;

  ${getFont('heading.large')}
`;

export const Actions = styled.div`
  padding: ${rem(16)} 0;
  border-top: 1px solid ${getColor('primary.lilac40')};
  column-gap: ${rem(12)};
  display: flex;
  align-items: center;
  justify-content: flex-end;
`;

export const ColumnSelectors = styled.div`
  display: flex;
  justify-content: center;
  padding: ${rem(16)} 0;
`;

export const ColumnSelector = styled.div`
  border-right: 1px solid ${getColor('primary.lilac20')};
  min-width: ${rem(383)};

  &:last-child {
    border-right: none;
  }
`;
