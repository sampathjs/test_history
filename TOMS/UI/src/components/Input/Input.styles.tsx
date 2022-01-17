import { rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { getColor, getFont } from 'styles';

export const commonTextInputFocusStyles = css`
  border-color: ${getColor('primary.lilac100')};
`;

export const commonTextInputHoverStyles = css`
  border-color: ${getColor('filter.hover')};
`;

export const commonTextInputStyles = css`
  width: 100%;
  ${getFont('body.small')}
  color: ${getColor('white.75')};
  outline: none;
  padding: 0 ${rem(8)};
  border-radius: ${rem(4)};
  min-height: ${rem(28)};
  border: ${rem(1)} solid ${getColor('primary.lilac30')};
  background: ${getColor('orderEntry.mainInput')};
  line-height: ${rem(15)};

  &:active,
  &:focus,
  &--is-focused {
    ${commonTextInputFocusStyles}
  }

  &:hover {
    ${commonTextInputHoverStyles}
  }

  &::placeholder,
  textarea::placeholder {
    color: ${getColor('white.75')};
  }
`;

export const Input = styled.input`
  ${commonTextInputStyles}

  &[type='number']::-webkit-inner-spin-button,
  &[type='number']::-webkit-outer-spin-button {
    -webkit-appearance: none;
    margin: 0;
  }

  &[type='number'] {
    appearance: textfield;
  }
`;
