import { ellipsis, rem } from 'polished';
import styled from 'styled-components/macro';

import { commonTextInputStyles } from 'components/Input';
import { getColor, getFont } from 'styles';

export const Wrapper = styled.div`
  width: 100%;
  .react-select__control {
    ${commonTextInputStyles}
    appearance: none;
    box-shadow: none;

    &--is-focused {
      border-color: ${getColor('primary.lilac100')};
    }
  }

  .react-select__indicator {
    color: ${getColor('orderEntry.selectedLozenge')};
    padding: 0;

    &:hover {
      color: ${getColor('orderEntry.selectedLozenge')};
    }

    svg {
      width: ${rem(18)};
    }

    &-separator {
      display: none;
    }
  }

  .react-select__value-container {
    color: ${getColor('white.75')};
    padding: 0;
    & > div {
      color: ${getColor('white.75')};
      padding: 0;
      margin: 0;
      line-height: 26px;
    }
  }

  .react-select__placeholder {
    color: ${getColor('white.75')};
    ${getFont('body.small')}
  }

  .react-select__menu {
    ${getFont('button.xSmall')}
    margin: 0;
    padding: 0;
    position: absolute;
    top: 28px;
    left: 4px;
    width: calc(100% - 8px);
    border-radius: 0;
    color: ${getColor('white.80')};
    line-height: ${rem(8)} !important; /* override default xSmall line-height */

    &-list {
      padding: 0;
      background-color: ${getColor('background.dropdownTooltip')};
    }
  }

  .react-select__group {
    padding: 0;
    &-heading {
      margin: 0;
      color: inherit;
      font-size: inherit;
      font-weight: inherit;
      padding: ${rem(3)} ${rem(6)} 0 ${rem(6)};
      line-height: ${rem(
        22
      )} !important; /* override default xSmall line-height */
      border-bottom: ${rem(1)} solid ${getColor('primary.lilac30')};
    }
  }

  .react-select__option {
    ${ellipsis()}
    padding: 0 ${rem(6)};
    line-height: ${rem(
      22
    )} !important; /* override default xSmall line-height */
    background-color: inherit;
    &:hover {
      ${getFont('button.xSmallBold')}
      letter-spacing: ${rem(0.15)};
      background: ${getColor('background.dropdownTooltipActive')};
    }
  }
`;
