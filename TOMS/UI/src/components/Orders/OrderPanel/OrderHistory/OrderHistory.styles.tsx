import { ellipsis, rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableHeaderCell,
  TableRow,
} from 'components/Table';
import { getColor, getFont } from 'styles/theme/helpers';

const commonCellCss = css`
  ${ellipsis()}

  &:first-child {
    padding-left: ${rem(20)};
    max-width: ${rem(80)};
  }

  &:nth-child(2) {
    max-width: ${rem(60)};
  }

  &:nth-child(3) {
    max-width: ${rem(100)};
  }

  &:last-child {
    max-width: ${rem(90)};
    padding-right: ${rem(20)};
  }
`;

export const HistoryTable = styled(Table)`
  ${getFont('table.cell')}
  background: ${getColor('background.tertiaryNav')};

  ${TableHeader} {
    border-top: 0;
  }

  ${TableBody} {
    max-height: ${rem(100)};
    overflow-y: scroll;
  }

  ${TableHeaderCell} {
    color: ${getColor('white.50')};
    ${commonCellCss}
  }

  ${TableRow} {
    border-bottom: 1px solid ${getColor('primary.lilac10')};
    color: ${getColor('white.65')};
    box-shadow: none;

    &:hover {
      box-shadow: 0 -1px 0 0 ${getColor('primary.lilac10')};
      background: ${getColor('background.primaryNav')};
      color: ${getColor('orderBlue')};
      cursor: pointer;
    }
  }

  ${TableCell} {
    ${commonCellCss}
  }
`;
