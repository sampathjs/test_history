import { rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { Checkbox as CheckboxComponent } from 'components/Checkbox';
import { getColor } from 'styles';

export const Checkbox = styled(CheckboxComponent)`
  display: inline-flex;
  color: ${getColor('white.20')};

  ${(props) =>
    props.checked &&
    css`
      color: ${getColor('white.50')};
    `}
`;

export const Icon = styled.span`
  border: ${rem(1.5)} solid currentColor;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: ${rem(18)};
  width: ${rem(18)};
  border-radius: ${rem(2)};
`;

export const CheckedIcon = styled(Icon)`
  &:before {
    content: '';
    height: ${rem(10)};
    width: ${rem(10)};
    background-color: ${getColor('white.60')};
    border-radius: ${rem(1)};
  }
`;

export const Wrapper = styled.label`
  display: inline-flex;
  cursor: pointer;
`;
