import { rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { getColor, getFont } from 'styles/theme/helpers';

import { LabelPlacement } from '.';

interface LabelProps {
  placement: LabelPlacement;
  isOptional?: boolean;
}

export const Label = styled.label<LabelProps>`
  display: flex;
  cursor: pointer;

  > span {
    text-transform: uppercase;
  }

  // TODO AJ - I'm thinking that we might need two distinct label
  // components here - since the majority of the styles are now different

  ${({ placement }) =>
    placement === 'end' &&
    css`
      ${getFont('button.xSmall')}
      align-items: center;
      column-gap: ${rem(6)};
      margin: ${rem(6)} 0;
      padding: ${rem(6)} ${rem(8)};
    `}

  ${({ placement }) =>
    placement === 'start' &&
    css`
      ${getFont('heading.entryTitle')}
      color: ${getColor('white.70')};
      display: flex;
      flex-direction: column-reverse;
      align-items: flex-start;
      row-gap: ${rem(5)};

      span {
        display: block;
      }
    `}


  ${({ isOptional }) =>
    isOptional &&
    css`
      color: ${getColor('orderEntry.selectedLozenge')};
    `}
`;
