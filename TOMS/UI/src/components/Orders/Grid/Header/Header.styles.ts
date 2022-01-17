import { rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { getColor, getFont } from 'styles';

export const Header = styled.div`
  padding: ${rem(11)} 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
`;

export const Title = styled.h1`
  margin: 0;
  color: ${getColor('white.80')};

  ${getFont('heading.large')};
`;

export const Actions = styled.div`
  display: flex;
  align-items: center;
`;

export const Action = styled.div`
  display: flex;
  padding: 0 ${rem(16)};
  border-right: 1px solid ${getColor('primary.lilac15')};

  &:last-child {
    padding-right: 0;
    border-right: none;
  }
`;

export const TimeZoneTitle = styled.h6`
  margin: 0;
  color: ${getColor('white.50')};

  ${getFont('table.header')}
`;

export const TimeZoneSelector = styled.div`
  display: flex;
  column-gap: ${rem(2)};
`;

type TimeZoneSelectButtonProps = {
  active: boolean;
};

export const TimeZoneSelectButton = styled.button<TimeZoneSelectButtonProps>`
  appearance: none;
  border: 0;
  background-color: transparent;
  color: ${getColor('primary.lilac30')};
  padding: ${rem(4)} ${rem(6)};
  border-radius: ${rem(3)};

  ${getFont('body.smallBold')}

  ${(props) =>
    props.active &&
    css`
      background-color: ${getColor('primary.lilac20')};
      color: ${getColor('white.80')};
    `}

  &:hover {
    background-color: ${getColor('primary.blue100')};
    color: ${getColor('background.tertiaryNav')};
  }
`;
