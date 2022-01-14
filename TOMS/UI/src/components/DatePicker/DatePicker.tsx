import { DateObj, useDayzed } from 'dayzed';
import { forwardRef, useState } from 'react';

import {
  getDateCurrentMonthEnd,
  getDateCurrentMonthStart,
  getDateLastMonthEnd,
  getDateLastMonthStart,
  getDateLastWeek,
  getDateToday,
} from 'utils/date';

import { Calendar } from './Calendar';
import * as Styles from './DatePicker.styles';

export type DatePickerProps = {
  onChange?: (dates: Date[]) => void;
  minDate?: Date;
  maxDate?: Date;
  range?: boolean;
  value?: Date[];
  defaultDate?: Date[];
};

export const DatePicker = forwardRef<HTMLDivElement, DatePickerProps>(
  // eslint-disable-next-line react/prop-types
  ({ minDate, maxDate, onChange, range, value = [] }, ref) => {
    const [offset, setOffset] = useState(0);
    const isRange = Boolean(range);

    const getNewValue = (date: Date, value: Date[]) => {
      const dateTime = date.getTime();
      const startDate = value[0]?.getTime();
      const endDate = value[1]?.getTime();

      if (!startDate) {
        return [date];
      }

      if (!isRange) {
        return startDate === dateTime ? [] : [date];
      }

      if (!endDate) {
        // When date is the same as start date selected, reset
        if (dateTime === startDate) {
          return [];
        }

        return dateTime > startDate ? [...value, date] : [date, ...value];
      }

      // When selected value is the same as previous, reset
      if (dateTime === startDate && startDate === endDate) {
        return [];
      }

      // Move range end date
      if (dateTime > startDate) {
        return [...value.slice(0, 1), date];
      }

      // Move range start date to earlier
      if (dateTime < startDate) {
        return [date, ...value.slice(1, 2)];
      }

      return [date];
    };

    const handleDateSelected = ({ date }: DateObj) => {
      if (!value.length) {
        return onChange?.([date]);
      }
      const newValue = getNewValue(date, value);

      return onChange?.(newValue);
    };

    const handleOffsetChanged = (offset: number) => setOffset(offset);

    const handleTodayButtonClick = () => {
      onChange?.([getDateToday().toJSDate()]);
      setOffset(0);
    };

    const handlePreviousWeekButtonClick = () => {
      onChange?.([getDateLastWeek().toJSDate(), getDateToday().toJSDate()]);
      setOffset(0);
    };

    const handleCurrentMonthButtonClick = () => {
      onChange?.([
        getDateCurrentMonthStart().toJSDate(),
        getDateCurrentMonthEnd().toJSDate(),
      ]);
      setOffset(0);
    };

    const handleLastMonthButtonClick = () => {
      onChange?.([
        getDateLastMonthStart().toJSDate(),
        getDateLastMonthEnd().toJSDate(),
      ]);
      setOffset(-1);
    };

    const isWithinHoverRange = (date: Date, hoveredDate: Date) => {
      const dateTime = date.getTime();
      const hoveredDateTime = hoveredDate?.getTime();
      const startDate = value[0]?.getTime();
      const endDate = value[1]?.getTime();

      if (!startDate || !hoveredDateTime || (startDate && endDate)) {
        return false;
      }

      return (
        (startDate < dateTime && hoveredDateTime >= dateTime) ||
        (dateTime < startDate && dateTime >= hoveredDateTime)
      );
    };

    const isWithinSelectionRange = (date: Date) => {
      const dateTime = date.getTime();
      const startDate = value[0]?.getTime();
      const endDate = value[1]?.getTime();

      if (!value.length || !endDate) {
        return false;
      }

      return dateTime > startDate && dateTime < endDate;
    };

    const dayzedData = useDayzed({
      selected: value,
      onDateSelected: handleDateSelected,
      onOffsetChanged: handleOffsetChanged,
      showOutsideDays: true,
      minDate,
      maxDate,
      offset,
    });

    return (
      <Calendar
        ref={ref}
        isRange={isRange}
        isWithinHoverRange={isWithinHoverRange}
        isWithinSelectionRange={isWithinSelectionRange}
        shortcuts={
          <>
            <Styles.Shortcut type="button" onClick={handleTodayButtonClick}>
              Today
            </Styles.Shortcut>
            {range && (
              <Styles.Shortcut
                type="button"
                onClick={handlePreviousWeekButtonClick}
              >
                Last 7 days
              </Styles.Shortcut>
            )}
            <Styles.Shortcut
              type="button"
              onClick={handleCurrentMonthButtonClick}
            >
              Current Month
            </Styles.Shortcut>
            <Styles.Shortcut type="button" onClick={handleLastMonthButtonClick}>
              Last Month
            </Styles.Shortcut>
          </>
        }
        {...dayzedData}
      />
    );
  }
);
