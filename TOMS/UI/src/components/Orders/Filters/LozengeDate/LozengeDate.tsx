import flattenDeep from 'lodash/flattenDeep';
import isEqual from 'lodash/isEqual';
import { DateTime } from 'luxon';
import { useEffect, useRef, useState } from 'react';
import { useClickAway } from 'react-use';

import { DatePicker } from 'components/DatePicker';
import { FilterValue } from 'types';
import { API_DATE_TIME_FORMAT } from 'utils/date';

import { Lozenge } from '../Lozenge';
import * as Styles from './LozengeDate.styles';

type LozengeDateProps = {
  children: string;
  value: FilterValue;
  onSelect(dates: string[]): void;
};

export const LozengeDate = ({
  children,
  onSelect,
  value,
}: LozengeDateProps) => {
  const datePickerRef = useRef<HTMLDivElement>(null);
  const [isHidden, setIsHidden] = useState(true);

  useClickAway(datePickerRef, () => {
    if (!isHidden) {
      setIsHidden(true);
    }
  });

  const [dates, setDates] = useState<string[]>([]);
  const [selectedDates, setSelectedDates] = useState<Date[]>([]);

  const areDateValuesEqual = (dates: string[], value: FilterValue) =>
    isEqual(flattenDeep(value), dates);

  const hasRangeSelected = (dates: string[]) => dates.length === 2;

  const convertDatesToStrings = (dates: Date[]) =>
    dates.map((date) =>
      DateTime.fromJSDate(date).toFormat(API_DATE_TIME_FORMAT)
    );

  useEffect(() => {
    if (
      hasRangeSelected(dates) &&
      !areDateValuesEqual(dates, value) &&
      !isHidden
    ) {
      onSelect(dates);
    }
  }, [dates, value, isHidden, onSelect]);

  const handleChange = (dates: Date[]) => {
    const newValue = convertDatesToStrings(dates);

    if (!isEqual(selectedDates, dates)) {
      setSelectedDates(dates);
    }

    if (
      hasRangeSelected(newValue) &&
      !areDateValuesEqual(newValue, value) &&
      !isHidden
    ) {
      setDates(newValue);
    }
  };

  const handleClick = () => {
    if (isHidden) {
      setIsHidden(false);
      onSelect([]);
    }
  };

  return (
    <>
      <Lozenge
        isActive={
          !isHidden || (areDateValuesEqual(dates, value) && value.length > 0)
        }
        onClick={handleClick}
      >
        {children}
      </Lozenge>
      <Styles.DatePicker $isHidden={isHidden}>
        <DatePicker
          ref={datePickerRef}
          value={selectedDates}
          onChange={handleChange}
          range
        />
      </Styles.DatePicker>
    </>
  );
};
