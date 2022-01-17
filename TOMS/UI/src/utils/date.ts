import findKey from 'lodash/findKey';
import { DateTime } from 'luxon';

import { NO_VALUE } from 'constants/format';
import { TimeZone } from 'types/date';
import { Nullable } from 'types/util';

export const API_DATE_TIME_FORMAT = 'yyyy-MM-dd HH:mm:ss';

export const API_DATE_FORMAT = 'yyyy-MM-dd';

export const parseApiDate = (date: Nullable<string>) => {
  return date ? DateTime.fromSQL(date, { zone: 'UTC' }) : null;
};

export const createApiDate = () => {
  return DateTime.utc().toSQL({ includeOffset: false });
};

export const formatToApiDate = (date: Date) => {
  return DateTime.fromJSDate(date).toSQL({ includeOffset: false });
};

export const getCurrentDateTime = () => {
  return DateTime.now();
};

export const getDateToday = () => {
  return DateTime.now().startOf('day');
};

export const getDateCurrentMonthStart = () => {
  return DateTime.now().startOf('month');
};

export const getDateCurrentMonthEnd = () => {
  return DateTime.now().endOf('month');
};

export const getDateLastMonthStart = () => {
  return DateTime.now().minus({ month: 1 }).startOf('month');
};

export const getDateLastMonthEnd = () => {
  return DateTime.now().minus({ month: 1 }).endOf('month');
};

export const getDateLastWeek = () => {
  return DateTime.now().minus({ week: 1 });
};

export const getMonthNameFromNumber = (month: number) => {
  return DateTime.fromObject({ month }).toLocaleString({
    month: 'long',
  });
};

export const formatDate = (date: Nullable<DateTime>) => {
  return date && date.isValid ? date.toFormat('D') : NO_VALUE;
};

export const formatShortDateTime = (date: Nullable<DateTime>) => {
  return date && date.isValid ? date.toFormat('dd/MM/yy T') : NO_VALUE;
};

export const formatDateTimeWithTimezone = (
  date: Nullable<DateTime>,
  timeZone: TimeZone
) => {
  if (date === null || !date.isValid) {
    return NO_VALUE;
  }

  const formattedDate = date.setZone(timeZone).toFormat('D T');
  const formattedTimeZone = findKey(TimeZone, (value) => value === timeZone);

  return `${formattedDate} ${formattedTimeZone}`;
};
