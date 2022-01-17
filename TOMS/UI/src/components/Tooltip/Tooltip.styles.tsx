import { rem } from 'polished';
import styled from 'styled-components/macro';

import { getColor, getFont } from 'styles';

export const Trigger = styled.div`
  display: inline-block;
  padding-right: ${rem(3)};
`;

export const Wrapper = styled.div`
  .tooltip {
    ${getFont('body.small')}
    padding: ${rem(8)} ${rem(10)};
    background: ${getColor('background.tertiaryNav')};
    color: ${getColor('white.80')};
    border-radius: ${rem(3)};
    &.show {
      margin-left: ${rem(3)};
      margin-top: ${rem(10)};
    }
    &.place-right:after {
      border: none;
    }
  }

  /* Currently applies to the 'i' icon only */
  svg {
    margin-left: ${rem(5)};
    font-size: ${rem(12)};
    border-radius: 50%;
    border: ${rem(1)} solid ${getColor('white.40')};

    /* outline circle */
    path:first-child {
      fill: none;
    }

    /* 'i' */
    path:last-child {
      fill: ${getColor('white.40')};
    }
  }

  &:hover {
    svg {
      border: none;
      /* outline circle */
      path:first-child {
        fill: ${getColor('primary.blue100')};
      }

      /* 'i' */
      path:last-child {
        fill: ${getColor('orderEntry.mainInput')};
      }
    }
  }
`;
