import { rem, rgba } from 'polished';
import styled, { css } from 'styled-components/macro';

import { getColor, getFont } from 'styles';

const ButtonIcon = styled.span`
  display: inline-flex;
`;

type ButtonProps = {
  variant: 'contained' | 'outlined';
  color: 'primary' | 'secondary' | 'error';
  size: 'medium' | 'small';
};

export const Button = styled.button<ButtonProps>`
  border-radius: ${rem(3)};
  padding: ${rem(6)} ${rem(8)};
  text-transform: uppercase;
  display: inline-flex;
  align-items: center;

  ${(props) =>
    props.size === 'medium' &&
    css`
      ${getFont('button.smallBold')}
    `}

  ${(props) =>
    props.size === 'small' &&
    css`
      ${getFont('button.xSmallBold')}
    `}

  ${(props) =>
    props.variant === 'contained' &&
    css`
      ${props.color === 'primary' &&
      css`
        background-color: ${getColor('primary.blue100')};
        color: ${getColor('background.bg')};

        ${ButtonIcon} {
          color: ${getColor('background.primaryNav')};
        }

        &:hover {
          background-color: ${getColor('white.100')};
        }
      `}

      ${props.color === 'secondary' &&
      css`
        color: ${getColor('white.80')};
        background-color: ${getColor('primary.lilac20')};

        ${ButtonIcon} {
          color: ${getColor('white.70')};
        }

        &:hover {
          color: ${getColor('background.tertiaryNav')};
          background-color: ${getColor('primary.lilac100')};

          ${ButtonIcon} {
            color: ${getColor('background.dividerLine')};
          }
        }
      `}

      ${props.color === 'error' &&
      css`
        background-color: ${getColor('primary.red100')};
        color: ${getColor('background.bg')};

        &:hover {
          background-color: ${getColor('white.100')};
        }
      `}
    `}

  ${(props) =>
    props.variant === 'outlined' &&
    css`
      outline-width: ${rem(1.25)};
      outline-style: solid;
      background-color: transparent;

      &:hover {
        background-color: ${getColor('white.5')};
        color: ${getColor('white.100')};
      }

      ${props.color === 'primary' &&
      css`
        color: ${getColor('primary.blue100')};
        outline-color: ${getColor('primary.blue100')};

        &:hover {
          outline-color: ${getColor('primary.blue100')};
        }
      `}

      ${props.color === 'secondary' &&
      css`
        color: ${getColor('primary.lilac80')};
        outline-color: ${getColor('primary.lilac40')};

        &:hover {
          outline-color: ${getColor('primary.blue100')};
        }
      `}


      ${props.color === 'error' &&
      css`
        color: ${getColor('primary.red100')};
        outline-color: ${rgba(getColor('primary.red100')(props), 0.6)};

        &:hover {
          outline-color: ${getColor('primary.red100')};
        }
      `}
    `}


    ${(props) =>
    props.disabled &&
    css`
      &,
      &:hover {
        cursor: inherit;
        color: ${getColor('background.ordersGrid')};
        background-color: ${getColor('background.tertiaryNav')};
      }
      ${props.variant === 'contained' &&
      css`
        &,
        &:hover {
          color: ${getColor('orderEntry.bg')};
          background-color: ${getColor('orderEntry.selectedLegPanel')};
        }
      `}
    `}
`;

export const StartIcon = styled(ButtonIcon)`
  padding-right: ${rem(6)};
`;

export const EndIcon = styled(ButtonIcon)`
  padding-left: ${rem(6)};
`;
