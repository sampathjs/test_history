import isEmpty from 'lodash/isEmpty';
import { useFieldArray } from 'react-hook-form';

import { CheckIcon, PlusIcon } from 'components/Icon';
import { OrderFill } from 'types';

import { FormFill, useOrderFillsContext } from '../OrderFillsContext';
import * as OrderPanelStyles from '../OrderPanel.styles';
import { Fill } from './Fill';
import { NewFill } from './NewFill';
import * as Styles from './OrderFills.styles';

const EMPTY_FILL: FormFill = {
  volume: '',
  price: '',
};

export const OrderFills = () => {
  const {
    fillsQuery,
    form,
    isEditingFills,
    orderQuantity,
    totalFilledQuantity,
  } = useOrderFillsContext();
  const { data, isLoading } = fillsQuery;
  const { control } = form;
  const { append, fields } = useFieldArray({
    control,
    name: 'fills',
  });
  const outstandingQuantity = orderQuantity - totalFilledQuantity;
  const isOrderFilled = outstandingQuantity === 0;
  const isQuantityOverfilled = outstandingQuantity < 0;

  if (isLoading) {
    return null;
  }

  return (
    <OrderPanelStyles.SecondarySection>
      <Styles.Header>
        <Styles.HeaderTitle>Fills</Styles.HeaderTitle>

        {isEditingFills && (
          <Styles.AddButton type="button" onClick={() => append(EMPTY_FILL)}>
            <Styles.AddButtonIcon>
              <PlusIcon />
            </Styles.AddButtonIcon>
            Add fill
          </Styles.AddButton>
        )}

        <Styles.Quantity overfilled={isQuantityOverfilled}>
          {isOrderFilled && (
            <Styles.QuantityFilledIcon>
              <CheckIcon />
            </Styles.QuantityFilledIcon>
          )}
          Outstanding quantity {outstandingQuantity} of {orderQuantity}
        </Styles.Quantity>
      </Styles.Header>
      <OrderPanelStyles.SectionRow>
        <Styles.Table>
          <Styles.TableHeader>
            <Styles.TableRow>
              <Styles.TableHeaderCell>Volume</Styles.TableHeaderCell>
              <Styles.TableHeaderCell>Price</Styles.TableHeaderCell>
              <Styles.TableHeaderCell>Date / Time</Styles.TableHeaderCell>
              <Styles.TableHeaderCell>Owner</Styles.TableHeaderCell>
              <Styles.TableHeaderCell>Endur Deal #</Styles.TableHeaderCell>
              <Styles.TableHeaderCell>Endur Response</Styles.TableHeaderCell>
            </Styles.TableRow>
          </Styles.TableHeader>
          <Styles.TableBody>
            {data?.map((fill: OrderFill) => (
              <Fill key={fill.id} fill={fill} />
            ))}

            {fields.map((field, index) => (
              <NewFill key={field.id} index={index} />
            ))}
          </Styles.TableBody>
        </Styles.Table>
      </OrderPanelStyles.SectionRow>
    </OrderPanelStyles.SecondarySection>
  );
};
