import { ellipsis, rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import * as StandardTable from 'components/Table';
import { getColor, getFont } from 'styles/theme/helpers';

import * as OrderPanelStyles from '../OrderPanel.styles';

export const Table = styled(StandardTable.Table)`
  grid-template-columns: auto;
  width: 100%;
  margin-bottom: ${rem(4)};
  ${getFont('table.cell')}
`;

export const TableRow = styled(StandardTable.TableRow)`
  display: grid;
  grid-template-columns: 50px 46px 95px 70px 77px 92px;
  box-shadow: inset 0 -1px 0 0px ${getColor('primary.lilac20')};
  color: inherit;
  align-items: center;

  &:hover {
    background: transparent;
    box-shadow: inset 0 -1px 0 0px ${getColor('primary.lilac20')};
    color: inherit;
  }
`;

export const TableHeader = styled(StandardTable.TableHeader)`
  box-shadow: inset 0 -1px 0 0px ${getColor('primary.lilac30')};
  border-top: 0;
  color: ${getColor('white.70')};
  ${getFont('heading.entryTitle')}

  ${TableRow} {
    border: 0;
    box-shadow: none;
  }
`;

export const TableBody = styled(StandardTable.TableBody)`
  color: ${getColor('white.85')};
`;

export const TableHeaderCell = styled(StandardTable.TableHeaderCell)`
  text-transform: uppercase;
  color: inherit;
  padding-left: ${rem(5)};
  padding-right: 0;
  ${ellipsis()}

  &:first-child {
    padding-left: 0;
  }

  &:hover {
    color: inherit;
    background-color: transparent;
  }
`;

export const TableCell = styled(StandardTable.TableCell)`
  color: inherit;
  padding-left: ${rem(5)};
  padding-right: 0;
  ${ellipsis()}

  &:first-child {
    padding-left: 0;
  }
`;

export const TableInputCell = styled(StandardTable.TableCell)`
  padding-left: 0;
  padding-right: 0;
`;

export const Header = styled.header`
  display: flex;
  align-items: center;
  margin-bottom: ${rem(12)};
`;

export const HeaderTitle = styled(OrderPanelStyles.SectionTitle)`
  margin-bottom: 0;
  min-height: ${rem(23)};
`;

export const AddButtonIcon = styled.span`
  display: inline-flex;
  font-size: ${rem(7)};
  color: ${getColor('white.50')};
  margin-right: ${rem(4)};
  padding: ${rem(4)};
  border: 1px solid currentColor;
  border-radius: 50%;
`;

export const AddButton = styled.button`
  font-family: Kanit;
  font-size: ${rem(11)};
  font-weight: 500;
  letter-spacing: 0.02em;
  color: ${getColor('white.65')};
  padding: ${rem(3)};
  padding-right: ${rem(8)};
  margin-left: ${rem(8)};
  display: inline-flex;
  align-items: center;
  border-radius: 2em;

  &:hover {
    color: ${getColor('white.85')};
    background-color: ${getColor('primary.lilac20')};

    ${AddButtonIcon} {
      color: ${getColor('white.80')};
    }
  }
`;

type QuantityProps = {
  overfilled: boolean;
};

export const Quantity = styled.div<QuantityProps>`
  margin-left: auto;
  color: ${getColor('white.70')};
  text-transform: uppercase;

  ${getFont('heading.entryTitle')};

  ${(props) =>
    props.overfilled &&
    css`
      color: ${getColor('primary.red100')};
    `}
`;

export const QuantityFilledIcon = styled.span`
  display: inline-flex;
  margin-right: ${rem(4)};
  padding: ${rem(3)};
  border: 1px solid ${getColor('white.50')};
  color: ${getColor('white.90')};
  border-radius: 50%;
  font-size: ${rem(8)};
`;
