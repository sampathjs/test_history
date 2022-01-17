import { ComponentStory, Meta } from '@storybook/react';
import { useState } from 'react';

import { DatePickerProps } from '.';
import { DatePicker } from './DatePicker';

export default {
  title: 'DatePicker',
  component: DatePicker,
} as Meta<typeof DatePicker>;

const Template: ComponentStory<typeof DatePicker> = (args: DatePickerProps) => {
  const [selectedDates, setSelectedDates] = useState<Date[]>([]);
  return (
    <DatePicker
      {...args}
      onChange={(value: Date[]) => setSelectedDates(value)}
      value={selectedDates}
    />
  );
};

export const SingleDate = Template.bind({});
SingleDate.args = { range: false };

export const RangeDate = Template.bind({});
RangeDate.args = { range: true };
