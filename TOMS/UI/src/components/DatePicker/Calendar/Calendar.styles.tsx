import { rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { getColor, getFont, getShadow } from 'styles';

export const Calendars = styled.div`
  box-shadow: ${getShadow('datePicker')};
  cursor: default;
`;

export const Calendar = styled.div`
  width: ${rem(196)};
  padding: ${rem(10)} ${rem(8)};
  background-color: ${getColor('background.dropdownTooltip')};
  border-radius: ${rem(3)};
  color: ${getColor('white.100')};
  text-align: center;
`;

export const Navigator = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: stretch;
  width: 100%;
  height: ${rem(24)};
  margin-bottom: ${rem(8)};

  button {
    display: flex;
    align-items: center;
    justify-content: center;
    width: ${rem(26)};
    font-size: ${rem(12)};
    color: ${getColor('white.100')};
    border: ${rem(1)} solid ${getColor('primary.lilac60')};

    &:first-of-type {
      border-top-left-radius: ${rem(3)};
      border-bottom-left-radius: ${rem(3)};
    }

    &:last-of-type {
      border-top-right-radius: ${rem(3)};
      border-bottom-right-radius: ${rem(3)};
    }

    &:hover {
      /** TODO: Consider adding a general 'hover' style */
      background-color: ${getColor('primary.blue100')};
      border-color: ${getColor('primary.blue100')};
      color: ${getColor('background.primaryNav')};
    }
  }

  span {
    flex: 1;
    padding: ${rem(4)} 0 ${rem(3)};
    border-color: ${getColor('primary.lilac60')};
    border-style: solid;
    border-width: ${rem(1)} 0;
    ${getFont('body.smallBold')}
  }
`;

export const Month = styled.div`
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: ${rem(4)};
  justify-content: center;
  padding: ${rem(6)} 0 ${rem(8)};
  margin-bottom: ${rem(8)};
  border-width: ${rem(1)} 0;
  border-style: solid;
  border-color: ${getColor('primary.lilac40')};
`;

export const Weekday = styled.div`
  padding: ${rem(3)} ${rem(2)};
  color: ${getColor('white.50')};
  ${getFont('body.small')};
`;

type DayProps = {
  $isSelectable?: boolean;
  $isSelected?: boolean;
  $isToday?: boolean;
  $isInRange?: boolean;
  $isInHoverRange?: boolean;
  $isNotCurrentMonth?: boolean;
};

export const Day = styled.button<DayProps>`
  padding: ${rem(3)} ${rem(2)};
  border: none;
  border-radius: ${rem(3)};
  color: ${getColor('white.80')};
  cursor: default;
  ${getFont('body.smallBold')}

  ${(props) =>
    props.$isSelectable &&
    css`
      background-color: ${getColor('background.dropdownTooltipActive')};
      cursor: pointer;
    `}

  ${(props) =>
    props.$isNotCurrentMonth &&
    css`
      background-color: transparent;
      color: ${getColor('white.30')};
    `}

  ${(props) =>
    !props.$isSelectable &&
    !props.$isNotCurrentMonth &&
    css`
      color: ${getColor('white.50')};
    `}

  ${(props) =>
    props.$isSelected &&
    css`
      background-color: ${getColor('primary.lilac100')};
      color: ${getColor('background.primaryNav')};
    `}

  ${(props) =>
    props.$isInHoverRange &&
    css`
      background-color: ${getColor('primary.blue100')};
      color: ${getColor('background.primaryNav')};
    `}


  ${(props) =>
    props.$isInRange &&
    css`
      background-color: ${getColor('primary.lilac100')};
      color: ${getColor('background.primaryNav')};
    `}

  &:hover {
    background-color: ${getColor('primary.blue100')};
    color: ${getColor('background.primaryNav')};

    ${(props) =>
      !props.$isSelectable &&
      css`
        background-color: transparent;
        color: ${props.$isNotCurrentMonth
          ? getColor('white.30')
          : getColor('white.50')};
      `}
  }
`;

export const Shortcuts = styled.div`
  display: flex;
  width: 100%;
  justify-content: center;
  flex-wrap: wrap;
  row-gap: ${rem(4)};
`;
