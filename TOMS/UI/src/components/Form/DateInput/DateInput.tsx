import { useRef, useState } from 'react';
import {
  FieldValues,
  useController,
  UseControllerProps,
} from 'react-hook-form';
import { useClickAway } from 'react-use';

import { DatePicker } from 'components/DatePicker';
import { CalendarIcon } from 'components/Icon';
import { NO_VALUE } from 'constants/format';
import { TimeZone } from 'types/date';

import * as Styles from './DateInput.styles';

interface DateInputProps<T> extends UseControllerProps<T> {
  minDate?: Date;
  maxDate?: Date;
  timeZone?: TimeZone;
}

export const DateInput = <T extends FieldValues>({
  maxDate,
  minDate,
  timeZone, // TODO: Implement timezones if neccessary
  ...props
}: DateInputProps<T>) => {
  const { field, fieldState } = useController(props);

  const datePickerRef = useRef<HTMLDivElement>(null);
  const [isHidden, setIsHidden] = useState(true);

  useClickAway(datePickerRef, () => {
    if (!isHidden) {
      setIsHidden(true);
    }
  });

  const formatDateValue = () => {
    return field.value ? field.value.toLocaleDateString() : '';
  };

  return (
    <Styles.DateInput $isValid={!Boolean(fieldState?.error)}>
      <Styles.TextInput
        {...field}
        placeholder={NO_VALUE}
        value={formatDateValue()}
        disabled
      />
      <Styles.Button
        type="button"
        onClick={() => setIsHidden((isHidden) => !isHidden)}
      >
        <CalendarIcon />
      </Styles.Button>
      <Styles.DatePicker $isHidden={isHidden}>
        <DatePicker
          ref={datePickerRef}
          minDate={minDate}
          maxDate={maxDate}
          value={[field?.value]}
          onChange={(value: Date[]) => {
            const newValue = value?.[0];
            if (newValue !== field.value) {
              field.onChange(newValue);
            }
          }}
        />
      </Styles.DatePicker>
    </Styles.DateInput>
  );
};
