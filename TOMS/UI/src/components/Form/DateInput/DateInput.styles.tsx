import { rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import {
  commonTextInputHoverStyles,
  commonTextInputStyles,
} from 'components/Input/Input.styles';
import { getColor } from 'styles';

export const Button = styled.button`
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  padding: ${rem(4)} ${rem(8)} ${rem(3)};
  border-top-right-radius: ${rem(4)};
  border-bottom-right-radius: ${rem(4)};
  color: ${getColor('primary.lilac40')};
  font-size: ${rem(15)};
`;

export const TextInput = styled.input`
  ${commonTextInputStyles};

  cursor: default;
  padding-right: ${rem(40)};

  &:active + ${Button}, &:focus + ${Button} {
    color: ${getColor('orderEntry.selectedLozenge')};
  }
`;

export const DateInput = styled.div<{ $isValid?: boolean }>`
  position: relative;

  &:hover {
    ${TextInput} {
      ${commonTextInputHoverStyles}
    }
      ${(props) =>
        !props.$isValid &&
        css`
          ${TextInput} {
            border-color: ${getColor('primary.red100')};
            color: ${getColor('primary.red100')};
          }

          ${TextInput} + ${Button} {
            color: ${getColor('primary.red100')};
          }
        `}
    }

    ${TextInput} + ${Button} {
      color: ${getColor('orderEntry.selectedLozenge')};
    }

    ${(props) =>
      !props.$isValid &&
      css`
        ${TextInput} {
          border-color: ${getColor('primary.red100')};
          color: ${getColor('primary.red100')};
        }

        ${TextInput} + ${Button} {
          color: ${getColor('primary.red100')};
        }
      `}

  }
`;

export const DatePicker = styled.div<{ $isHidden: boolean }>`
  position: absolute;
  top: calc(100% + 1px);
  right: 0;
  z-index: 1;

  ${(props) =>
    props.$isHidden &&
    css`
      display: none;
    `}
`;
