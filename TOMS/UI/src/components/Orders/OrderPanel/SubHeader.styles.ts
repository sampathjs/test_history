import { rem } from 'polished';
import styled from 'styled-components/macro';

import { getColor, getFont } from 'styles';

export const SubHeader = styled.div`
  display: flex;
  align-items: center;
  padding: ${rem(8)} ${rem(20)};
  background-color: ${getColor('primary.lilac10')};
  color: ${getColor('white.85')};

  ${getFont('heading.xSmall')}
`;

export const SubHeaderText = styled.span`
  display: inline-block;
  padding: 0 ${rem(8)};
  border-left: 1px solid ${getColor('primary.lilac20')};

  &:first-child {
    padding-left: 0;
    border-left: none;
  }
`;

export const Status = styled.span`
  display: inline-block;
  margin-left: auto;
  text-transform: uppercase;
`;
