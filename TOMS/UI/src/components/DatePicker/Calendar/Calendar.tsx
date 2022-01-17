import { RenderProps } from 'dayzed';
import { forwardRef, ReactNode, useState } from 'react';

import { ChevronLeftIcon, ChevronRightIcon } from 'components/Icon';
import { Nullable } from 'types/util';
import { getMonthNameFromNumber } from 'utils/date';

import * as Styles from './Calendar.styles';

const weekdayNamesShort = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

type CalendarProps = {
  isWithinHoverRange?(date: Date, hoveredDate: Nullable<Date>): boolean;
  isWithinSelectionRange?(date: Date): boolean;
  isRange?: boolean;
  shortcuts?: ReactNode;
} & RenderProps;

// TODO: Re-enable rule once fix had been implemented to fix this issue
// https://github.com/yannickcr/eslint-plugin-react/issues/3015
/* eslint-disable react/prop-types */
export const Calendar = forwardRef<HTMLDivElement, CalendarProps>(
  (
    {
      calendars,
      getBackProps,
      getDateProps,
      getForwardProps,
      isRange,
      isWithinHoverRange,
      isWithinSelectionRange,
      shortcuts,
    },
    ref
  ) => {
    const [hoveredDate, setHoveredDate] = useState<Nullable<Date>>(null);

    const handleMouseEnter = (date: Date) => setHoveredDate(date);

    const handleMouseLeave = () => setHoveredDate(null);

    return (
      <Styles.Calendars ref={ref}>
        {calendars.map(({ month, weeks, year }) => (
          <Styles.Calendar key={`${month}${year}`}>
            <Styles.Navigator>
              <button {...getBackProps({ calendars })}>
                <ChevronLeftIcon />
              </button>
              <span>
                {getMonthNameFromNumber(month + 1)} {year}
              </span>
              <button {...getForwardProps({ calendars })}>
                <ChevronRightIcon />
              </button>
            </Styles.Navigator>
            <Styles.Month onMouseLeave={handleMouseLeave}>
              {weekdayNamesShort.map((weekday, index) => (
                <Styles.Weekday key={`${month}${year}${weekday}${index}`}>
                  {weekday}
                </Styles.Weekday>
              ))}
              {weeks.map((week, weekIndex) =>
                week.map((dateObj, index) => {
                  const key = `${month}${year}${weekIndex}${index}`;

                  if (!dateObj) {
                    return <div />;
                  }

                  const {
                    date,
                    nextMonth,
                    prevMonth,
                    selectable,
                    selected,
                    today,
                  } = dateObj;

                  return (
                    <Styles.Day
                      key={key}
                      type="button"
                      $isToday={today}
                      $isSelected={selected}
                      $isSelectable={selectable}
                      $isInRange={Boolean(
                        isRange && isWithinSelectionRange?.(date)
                      )}
                      $isInHoverRange={Boolean(
                        isRange && isWithinHoverRange?.(date, hoveredDate)
                      )}
                      $isNotCurrentMonth={prevMonth || nextMonth}
                      {...getDateProps({
                        dateObj,
                        onMouseEnter: () => handleMouseEnter(date),
                      })}
                    >
                      {selectable ? date.getDate() : 'X'}
                    </Styles.Day>
                  );
                })
              )}
            </Styles.Month>
            {shortcuts && <Styles.Shortcuts>{shortcuts}</Styles.Shortcuts>}
          </Styles.Calendar>
        ))}
      </Styles.Calendars>
    );
  }
);

Calendar.displayName = 'Calendar';
