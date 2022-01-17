import isDate from 'lodash/isDate';
import { useEffect } from 'react';
import { useFormContext } from 'react-hook-form';

import { Button } from 'components/Button';
import { DateInput } from 'components/Form/DateInput';
import { SelectInput } from 'components/Form/SelectInput';
import { TextAreaInput } from 'components/Form/TextAreaInput';
import { TextInput } from 'components/Form/TextInput';
import { ToggleInput } from 'components/Form/ToggleInput';
import { FormControlLabel } from 'components/FormControlLabel';
import { ChevronThinRightIcon } from 'components/Icon';
import { ToggleItem } from 'components/Toggle';
import { Tooltip } from 'components/Tooltip';
import { useAuthContext, useTimeZoneContext } from 'contexts';
import { useParties } from 'hooks/useParties';
import { useRefData } from 'hooks/useRefData';
import {
  ContractTerms,
  OrderSide,
  OrderType,
  PriceType,
  RefTypeIds,
  ValidationType,
  YesNo,
} from 'types';
import { getDateToday } from 'utils/date';
import {
  getSelectOptionsFromPartyData,
  getSelectOptionsFromRefData,
  isSelectedOptionAvailable,
} from 'utils/form';
import { formatOrderType } from 'utils/order';
import { getRefDataByTypeId } from 'utils/references';

import { getProductTickerOptions, getTradeableParties } from '../helpers';
import * as Styles from '../OrderPanel.styles';
import { ParsedFormInputs, RawFormInputs } from './types';
import { useTradeableMetalForms } from './useTradeableMetalForms';
import { useTradeableMetalLocations } from './useTradeableMetalLocations';
import { useTradeableTickers } from './useTradeableTickers';

type Props = {
  onSubmit(values: ParsedFormInputs): Promise<void> | void;
};

export const OrderForm = (props: Props) => {
  const { onSubmit } = props;
  const { timeZone } = useTimeZoneContext();
  const { user } = useAuthContext();
  const refData = useRefData();
  const { data: partyData } = useParties();

  const {
    control,
    formState,
    getValues,
    handleSubmit,
    reset,
    resetField,
    watch,
  } = useFormContext<RawFormInputs>();
  const { isDirty, isSubmitting, isValid } = formState;

  const [jmPmmUnit, counterparty] = watch(['jmPmmUnit', 'counterparty']);

  const productTickers = useTradeableTickers(
    jmPmmUnit?.value,
    counterparty?.value
  );
  const metalForms = useTradeableMetalForms(counterparty?.value);
  const metalLocations = useTradeableMetalLocations(counterparty?.value);
  const metalUnits = getRefDataByTypeId(refData, RefTypeIds.METAL_UNIT);
  const [internalParties, externalParties] = getTradeableParties(
    user,
    partyData
  );

  useEffect(() => {
    // Reset location if no longer available
    const selectedMetalLocation = getValues('location');
    if (
      selectedMetalLocation &&
      !isSelectedOptionAvailable(
        getSelectOptionsFromRefData(metalLocations),
        selectedMetalLocation
      )
    ) {
      resetField('location');
    }

    // Reset metal form if no longer available
    const selectedMetalForm = getValues('metalForm');
    if (
      selectedMetalForm &&
      !isSelectedOptionAvailable(
        getSelectOptionsFromRefData(metalForms),
        selectedMetalForm
      )
    ) {
      resetField('metalForm');
    }

    // Reset product ticker if no longer available
    const selectedTicker = getValues('productTicker');
    if (
      selectedTicker &&
      !isSelectedOptionAvailable(
        getSelectOptionsFromRefData(productTickers),
        selectedTicker
      )
    ) {
      resetField('productTicker');
    }
  });

  return (
    <Styles.Form onSubmit={handleSubmit(onSubmit)}>
      <>
        <Styles.Content>
          {isSubmitting ? (
            // TODO: replace with skeleton loader
            <p style={{ textAlign: 'center', color: 'white' }}>
              {'Loading...'}
            </p>
          ) : (
            <Styles.Details>
              <Styles.Summary>
                <Styles.SectionRow>
                  <Styles.ToggleWrapper>
                    <FormControlLabel
                      label="Trade side"
                      labelPlacement="start"
                      control={
                        <ToggleInput
                          name="orderSide"
                          control={control}
                          rules={{ required: true }}
                        >
                          <ToggleItem
                            side={OrderSide.buy}
                            value={OrderSide.buy}
                          />
                          <ToggleItem
                            side={OrderSide.sell}
                            value={OrderSide.sell}
                          />
                        </ToggleInput>
                      }
                    />
                    <FormControlLabel
                      label="Order type"
                      labelPlacement="start"
                      control={
                        <ToggleInput
                          name="orderType"
                          control={control}
                          rules={{ required: true }}
                        >
                          <ToggleItem value={OrderType.limit}>
                            {formatOrderType(OrderType.limit)}
                          </ToggleItem>
                          <ToggleItem value={OrderType.reference}>
                            {formatOrderType(OrderType.reference)}
                          </ToggleItem>
                        </ToggleInput>
                      }
                    />
                  </Styles.ToggleWrapper>

                  <FormControlLabel
                    label="Order reference (Optional)"
                    isOptional
                    labelPlacement="start"
                    control={
                      <TextInput
                        name="orderRef"
                        control={control}
                        maxLength={32}
                      />
                    }
                  />
                </Styles.SectionRow>

                <Styles.SectionRow>
                  <Styles.ThirdColumn>
                    <FormControlLabel
                      labelPlacement="start"
                      label="JM PMM unit"
                      control={
                        <SelectInput
                          name="jmPmmUnit"
                          control={control}
                          options={getSelectOptionsFromPartyData(
                            internalParties
                          )}
                          rules={{ required: true }}
                        />
                      }
                    />
                  </Styles.ThirdColumn>

                  <Styles.Column>
                    <FormControlLabel
                      labelPlacement="start"
                      label="Counterparty"
                      control={
                        <SelectInput
                          name="counterparty"
                          control={control}
                          options={getSelectOptionsFromPartyData(
                            externalParties
                          )}
                          rules={{ required: true }}
                        />
                      }
                    />
                  </Styles.Column>
                </Styles.SectionRow>
              </Styles.Summary>

              <Styles.PrimarySection>
                <Styles.SectionTitle>
                  <Tooltip text="Tooltip text">Metal Information</Tooltip>
                </Styles.SectionTitle>
                <Styles.SectionRow>
                  <Styles.ThirdColumn>
                    <FormControlLabel
                      labelPlacement="start"
                      label="Product ticker"
                      control={
                        <SelectInput
                          name="productTicker"
                          control={control}
                          options={getProductTickerOptions(productTickers)}
                          rules={{ required: true }}
                        />
                      }
                    />
                  </Styles.ThirdColumn>

                  <Styles.ThirdColumn>
                    <FormControlLabel
                      labelPlacement="start"
                      label="Unit"
                      control={
                        <SelectInput
                          name="metalUnit"
                          control={control}
                          options={getSelectOptionsFromRefData(metalUnits)}
                          rules={{ required: true }}
                        />
                      }
                    />
                  </Styles.ThirdColumn>

                  <Styles.ThirdColumn>
                    <FormControlLabel
                      labelPlacement="start"
                      label="Metal QTY"
                      control={
                        <TextInput
                          type="number"
                          name="metalQty"
                          control={control}
                          rules={{ required: true }}
                        />
                      }
                    />
                  </Styles.ThirdColumn>
                </Styles.SectionRow>
              </Styles.PrimarySection>

              <Styles.PrimarySection>
                <Styles.SectionTitle>
                  <Tooltip text="Tooltip text">Contract Terms</Tooltip>
                </Styles.SectionTitle>
                <Styles.SectionRow>
                  <Styles.ThirdColumn>
                    <Styles.SpotForwardWrapper>
                      <ToggleInput
                        name="priceType"
                        control={control}
                        rules={{ required: true }}
                      >
                        <ToggleItem value={PriceType.spot} />
                        <ToggleItem value={PriceType.forward} />
                      </ToggleInput>
                      <ChevronThinRightIcon />
                    </Styles.SpotForwardWrapper>
                  </Styles.ThirdColumn>

                  <Styles.ThirdColumn>
                    <ToggleInput
                      name="contractTemplate"
                      control={control}
                      rules={{ required: true }}
                    >
                      <ToggleItem value={ContractTerms.fixed} />
                      <ToggleItem value={ContractTerms.relative} />
                    </ToggleInput>
                  </Styles.ThirdColumn>
                </Styles.SectionRow>
              </Styles.PrimarySection>

              <Styles.SecondarySection>
                <Styles.SectionRow>
                  <Styles.ThirdColumn>
                    <FormControlLabel
                      labelPlacement="start"
                      label="Limit Price"
                      control={
                        <TextInput
                          name="limitPrice"
                          type="number"
                          control={control}
                          rules={{ required: true }}
                        />
                      }
                    />
                  </Styles.ThirdColumn>

                  <Styles.ThirdColumn>
                    <FormControlLabel
                      labelPlacement="start"
                      label="Start Date"
                      control={
                        <DateInput
                          name="startDate"
                          control={control}
                          minDate={getDateToday().toJSDate()}
                          timeZone={timeZone}
                          rules={{
                            deps: ['settleDate'],
                            required: true,
                          }}
                        />
                      }
                    />
                  </Styles.ThirdColumn>
                  <Styles.ThirdColumn>
                    <FormControlLabel
                      labelPlacement="start"
                      label="Settle Date"
                      control={
                        <DateInput
                          name="settleDate"
                          control={control}
                          minDate={getDateToday().toJSDate()}
                          timeZone={timeZone}
                          rules={{
                            required: true,
                            validate: {
                              minDate: (value) => {
                                const startDate = getValues('startDate');

                                if (isDate(value) && startDate) {
                                  return (
                                    value.getTime() >= startDate.getTime() ||
                                    'Settle date cannot be before start date'
                                  );
                                }

                                return true;
                              },
                            },
                          }}
                        />
                      }
                    />
                  </Styles.ThirdColumn>
                </Styles.SectionRow>
              </Styles.SecondarySection>

              <Styles.SecondarySection>
                <Styles.SectionRow>
                  <FormControlLabel
                    labelPlacement="start"
                    label="Validation Type"
                    control={
                      <ToggleInput
                        name="validationType"
                        control={control}
                        rules={{ required: true }}
                      >
                        <ToggleItem value={ValidationType.goodTillCancelled} />
                        <ToggleItem value={ValidationType.expiryDate} />
                      </ToggleInput>
                    }
                  />

                  <Styles.ThirdColumn>
                    <FormControlLabel
                      labelPlacement="start"
                      label="Part Fillable"
                      control={
                        <ToggleInput
                          name="partFillable"
                          control={control}
                          rules={{ required: true }}
                        >
                          <ToggleItem value={YesNo.yes} />
                          <ToggleItem value={YesNo.no} />
                        </ToggleInput>
                      }
                    />
                  </Styles.ThirdColumn>
                </Styles.SectionRow>
              </Styles.SecondarySection>

              <Styles.SecondarySection>
                <Styles.SectionRow>
                  <Styles.HalfColumn>
                    <FormControlLabel
                      labelPlacement="start"
                      label="Location"
                      control={
                        <SelectInput
                          name="location"
                          control={control}
                          options={getSelectOptionsFromRefData(metalLocations)}
                          rules={{ required: true }}
                        />
                      }
                    />
                  </Styles.HalfColumn>

                  <Styles.HalfColumn>
                    <FormControlLabel
                      labelPlacement="start"
                      label="Metal Form"
                      control={
                        <SelectInput
                          name="metalForm"
                          control={control}
                          options={getSelectOptionsFromRefData(metalForms)}
                          rules={{ required: true }}
                        />
                      }
                    />
                  </Styles.HalfColumn>
                </Styles.SectionRow>
              </Styles.SecondarySection>

              <Styles.SecondarySection>
                <FormControlLabel
                  labelPlacement="start"
                  label="Comments (Optional)"
                  isOptional
                  control={
                    <TextAreaInput
                      name="comment"
                      placeholder="Add your comment"
                      control={control}
                    />
                  }
                />
              </Styles.SecondarySection>
            </Styles.Details>
          )}
        </Styles.Content>
        <Styles.Footer>
          <Styles.Actions>
            <Button
              variant="outlined"
              color="secondary"
              type="button"
              onClick={() => reset()}
            >
              Reset
            </Button>
          </Styles.Actions>
          <Styles.Actions>
            {/* TODO: Remove some button checks for now disabled={... || isSubmitted } */}
            <Button
              disabled={!isDirty || !isValid || isSubmitting}
              type="submit"
            >
              Submit Order
            </Button>
          </Styles.Actions>
        </Styles.Footer>
      </>
    </Styles.Form>
  );
};
