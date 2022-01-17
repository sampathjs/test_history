import { rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { FILTER_PANEL_WIDTH, getColor, getFont, getGradient } from 'styles';

export const Layout = styled.div<{
  $isExpanded: boolean;
  $isToggleHover: boolean;
}>`
  grid-area: filters;
  position: relative;
  width: ${rem(FILTER_PANEL_WIDTH)};
  padding: 0 ${rem(12)};
  border-right: ${rem(2)} solid ${getColor('primary.blue100')};
  color: ${getColor('white.60')};
  overflow-y: auto;
  overflow-x: hidden;
  transform: translateX(-${rem(FILTER_PANEL_WIDTH - 2)});

  &::after {
    content: '';
    position: sticky;
    left: 0;
    right: 0;
    bottom: 0;
    margin-left: -${rem(12)};
    display: block;
    height: ${rem(60)};
    width: calc(100% + ${rem(24)});
    pointer-events: none;
    ${getGradient('orderBlend')}
  }

  ${(props) =>
    props.$isExpanded &&
    css`
      transform: translateX(0);
      background-color: ${getColor('filter.filterPanelBg')};
      border-right: ${rem(2)} solid ${getColor('background.dividerLine')};
    `};

  ${(props) =>
    props.$isExpanded &&
    props.$isToggleHover &&
    css`
      border-right-color: ${getColor('primary.lilac100')};
    `}

  ${(props) =>
    !props.$isExpanded &&
    props.$isToggleHover &&
    css`
      background-color: ${getColor('white.100')};
      border-right-color: ${getColor('white.100')};
    `}
`;

export const Heading = styled.h1`
  ${getFont('heading.medium')}
  margin: 0 ${rem(8)};
  padding: ${rem(20)} 0 ${rem(12)};
  border-bottom: ${rem(1)} solid ${getColor('primary.lilac60')};
  color: ${getColor('white.50')};
`;

export const Filter = styled.div`
  position: relative;
  padding-bottom: ${rem(11)};

  &:not(:last-child)::after {
    content: '';
    position: absolute;
    height: 1px;
    left: ${rem(8)};
    right: ${rem(8)};
    bottom: 0;
    background-color: ${getColor('primary.lilac20')};
  }
`;

export const Subheading = styled.h2`
  ${getFont('heading.filterSubhead')}
  margin: 0 ${rem(8)} 0;
  padding: ${rem(13)} 0 0;
`;

export const CollapsableSubheading = styled(Subheading)`
  display: flex;
  justify-content: space-between;
  align-items: center;

  &:hover {
    color: ${getColor('primary.blue100')};
  }
`;
