import { rem } from 'polished';
import styled from 'styled-components/macro';

import { getColor, getFont } from 'styles';

export const Shortcut = styled.button`
  padding: ${rem(4)} ${rem(8)};
  color: ${getColor('white.80')};
  border: ${rem(1)} solid ${getColor('primary.lilac40')};
  border-radius: ${rem(3)};
  ${getFont('body.small')}

  &:nth-child(odd):not(:last-of-type) {
    border-top-right-radius: 0;
    border-bottom-right-radius: 0;
  }

  &:nth-child(even) {
    border-left: none;
    border-top-left-radius: 0;
    border-bottom-left-radius: 0;
  }

  &:hover {
    background-color: ${getColor('primary.blue100')};
    border-color: ${getColor('primary.blue100')};
    color: ${getColor('background.primaryNav')};
  }
`;
