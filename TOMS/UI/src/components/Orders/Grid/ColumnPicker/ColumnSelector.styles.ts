import { rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { Checkbox } from 'components/Checkbox';
import { FormControlLabel } from 'components/FormControlLabel';
import { CheckIcon } from 'components/Icon';
import { getColor, getFont } from 'styles';

export const Wrapper = styled.div`
  padding: 0 ${rem(12)};
`;

export const Header = styled.div`
  padding-bottom: ${rem(10)};
  margin-bottom: ${rem(8)};
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid ${getColor('primary.lilac40')};
`;

export const Title = styled.h3`
  margin: 0;
  color: ${getColor('white.90')};

  ${getFont('heading.medium')}
`;

export const Columns = styled.ul`
  list-style: none;
  margin: 0;
  padding: 0;
`;

export const Column = styled.li`
  margin-bottom: ${rem(1)};
`;

type ColumnSelectButtonProps = {
  selected: boolean;
};

export const ColumnSelectButton = styled.button<ColumnSelectButtonProps>`
  display: flex;
  align-items: center;
  justify-content: space-between;
  appearance: none;
  cursor: pointer;
  background-color: transparent;
  border: none;
  width: 100%;
  text-align: left;
  padding: ${rem(6)} ${rem(10)};
  color: ${getColor('white.80')};

  ${getFont('body.medium')}

  ${(props) =>
    props.selected &&
    css`
      background-color: ${getColor('orderEntry.selectedLegPanelInput')};
    `}

  &:hover {
    background-color: ${getColor('primary.blue100')};
    color: ${getColor('background.tertiaryNav')};
  }
`;

export const SelectedIcon = styled(CheckIcon)`
  margin-right: ${rem(4)};
  font-size: ${rem(11)};
`;

export const SelectAllLabel = styled(FormControlLabel)`
  color: ${getColor('white.50')};
  margin: 0;
  padding: 0;
`;

export const SelectAllCheckbox = styled(Checkbox)`
  color: ${getColor('white.60')};
  display: inline-flex;
  font-size: ${rem(14)};
`;
