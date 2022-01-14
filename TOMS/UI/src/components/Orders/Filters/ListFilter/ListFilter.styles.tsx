import { rem } from 'polished';
import styled from 'styled-components/macro';

import { getColor, getFont } from 'styles/theme/helpers';

export const List = styled.ul`
  ${getFont('body.small')}
  display: flex;
  flex-direction: column;
  row-gap: ${rem(1)};
  margin-top: ${rem(3)};
  color: ${getColor('white.70')};
`;

export const ShowMore = styled.button`
  ${getFont('button.xSmallBold')}
  appearance: none;
  margin: ${rem(1)} 0 -${rem(5)};
  padding: ${rem(10)} ${rem(8)};
  background-color: transparent;
  border: none;
  color: ${getColor('white.30')};
  text-transform: uppercase;

  &:hover {
    color: ${getColor('primary.blue100')};
  }
`;

export const GroupSubheading = styled.h3`
  ${getFont('body.small')}
  color: ${getColor('white.70')};
  padding: ${rem(4.5)} ${rem(8)};
  margin: 0;
`;
